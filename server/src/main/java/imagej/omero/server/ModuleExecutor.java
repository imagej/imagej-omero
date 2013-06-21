/*
 * #%L
 * Call ImageJ commands from OMERO on the server side.
 * %%
 * Copyright (C) 2013 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package imagej.omero.server;

import imagej.Identifiable;
import imagej.ImageJ;
import imagej.command.Command;
import imagej.command.CommandInfo;
import imagej.module.AbstractModuleInfo;
import imagej.module.Module;
import imagej.module.ModuleException;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;
import imagej.module.ModuleService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.util.Manifest;

/**
 * Executes ImageJ modules from OMERO server side.
 * <p>
 * An ImageJ <em>module</em> is something that implements the {@link Module}
 * interface, whose details are described by a {@link ModuleInfo} object. While
 * ImageJ {@link Command}s are one kind of {@link Module}, they are not
 * necessarily the only kind. Hence, this class can expose and execute available
 * {@link Command}s, but also any other sort of custom {@link Module}s known to
 * the ImageJ application context. ImageJ's {@link ModuleService} does most of
 * the work of discovering and executing modules.
 * </p>
 * <p>
 * This class has three features:
 * </p>
 * <ul>
 * <li>{@link #generateStubs()} - Generates stub files for available ImageJ
 * modules.</li>
 * <li>{@link #parse(File)} - Parses the given stub file, reporting metadata to
 * OMERO.</li>
 * <li>{@link #execute(File)} - Executes the module indicated by the given stub
 * file.</li>
 * </ul>
 * <p>
 * It also accepts command line arguments via the {@link #execute(String[])}
 * method to perform some combination of these actions.
 * </p>
 * <p>
 * Note that only ImageJ modules which are {@link Identifiable} are supported.
 * Fortunately, both {@link CommandInfo} and {@link AbstractModuleInfo} do
 * implement this interface. But other {@link ModuleInfo} implementations need
 * to also implement {@link Identifiable} for this mechanism to support them.
 * </p>
 */
public class ModuleExecutor extends AbstractContextual {

	// -- Command line arguments (thank you args4j!) --

	@Option(name = "-g", aliases = {"--generate"},
		usage = "generates a list of stub files for available ImageJ modules")
	private boolean generate;

	@Option(name = "-p", aliases = {"--parse"},
		usage = "parses the given stub file, uploading metadata to OMERO")
	private File stubToParse;

	@Option(name = "-e", aliases = { "--execute" },
		usage = "executes the ImageJ module specified by the given stub file, "
			+ "obtaining input parameter values from OMERO prior to execution, "
			+ "and uploading output parameter values to OMERO afterward")
	private File stubToExecute;

	@Option(name = "-v", aliases = { "--verbose" },
		usage = "provide copious details about what is happening")
	private boolean verbose;

	// -- Other instance fields --

	/** The current ImageJ application context. */
	private final ImageJ ij;

	/** Index of modules keyed on identifier. */
	private final HashMap<String, ModuleInfo> ids;

	// -- Constructors --

	public ModuleExecutor() {
		this(new ImageJ());
	}

	public ModuleExecutor(final Context context) {
		this(new ImageJ(context));
	}

	public ModuleExecutor(final ImageJ ij) {
		this.ij = ij;
		setContext(ij.getContext());

		// build index of modules, keyed on identifier
		ids = new HashMap<String, ModuleInfo>();
		for (final ModuleInfo info : ij.module().getModules()) {
			final String id = getID(info);
			if (id == null) {
				ij.log().warn("Ignoring unidentifiable module: " + info);
				continue;
			}
			if (verbose) ij.log().info("Indexed module: " + info);
			ids.put(id, info);
		}
	}

	// -- ModuleExecutor methods --

	/** Gets the ImageJ context wrapper used by this module executor. */
	public ImageJ ij() {
		return ij;
	}

	/** Gets whether verbose output mode is enabled. */
	public boolean isVerbose() {
		return verbose;
	}

	/** Toggles verbose output mode. */
	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	/** Executes the operations specified by the given command line arguments. */
	public int execute(final String[] args) {
		final CmdLineParser parser = new CmdLineParser(this);

		try {
			// parse the command line arguments, populating instance fields
			parser.parseArgument(args);
			validateArgs(parser);
		}
		catch (final CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println();

			// print the list of available options
			System.err.println("Available options:");
			parser.printUsage(System.err);
			System.err.println();

			return 1;
		}

		if (generate) generateStubs();
		if (stubToParse != null) parse(stubToParse);
		if (stubToExecute != null) execute(stubToExecute);

		return 0;
	}

	/**
	 * Generates a list of stub files on disk,
	 * describing all available ImageJ modules.
	 */
	public void generateStubs() {
		int no = 0;
		for (final String id : ids.keySet()) {
			try {
				generateStub(id, no++);
			}
			catch (final IOException exc) {
				ij.log().warn("Invalid identifier: " + id, verbose ? exc : null);
			}
		}
	}

	/**
	 * Parses a stub file to produce metadata about a module/script,
	 * reporting details back to OMERO.
	 * 
	 * @param stub The stub file which identifies the module to describe.
	 */
	public void parse(final File stub) {
		final ModuleInfo info;
		try {
			info = findInfo(stub);
		}
		catch (final IOException exc) {
			ij.log().error("Invalid stub: " + stub, verbose ? exc : null);
			return;
		}
		if (verbose) ij.log().info("Parsing: " + info);

		final OMEROJobParams omeroData = convert(info);
		upload(omeroData);
	}

	/**
	 * Executes a module as specified by a particular stub file.
	 * <p>
	 * Inputs are received from OMERO via Ice just prior to execution.
	 * Outputs are sent back to OMERO via Ice immediately afterward.
	 * </p>
	 * 
	 * @param stub The stub file which identifies the module to execute.
	 */
	public void execute(final File stub) {
		// locate module
		if (verbose) ij.log().info(stub + ": locating module");
		final ModuleInfo info;
		try {
			info = findInfo(stub);
		}
		catch (final IOException exc) {
			ij.log().error("Invalid stub: " + stub, verbose ? exc : null);
			return;
		}

		// populate inputs
		if (verbose) ij.log().info(stub + ": populating inputs");
		final HashMap<String, Object> inputMap = new HashMap<String, Object>();
		// TODO: Ask OMERO for input parameter values and convert them.

		// execute module
		if (verbose) ij.log().info(stub + ": executing module");
		final Module module;
		try {
			module = ij.command().run(info, inputMap).get();
		}
		catch (final InterruptedException exc) {
			ij.log().error(stub + ": error executing script", exc);
			return;
		}
		catch (final ExecutionException exc) {
			ij.log().error(stub + ": error executing script", exc);
			return;
		}

		// populate outputs
		if (verbose) ij.log().info(stub + ": populating outputs");
		final Map<String, Object> outputs = module.getOutputs();
		for (final String output : outputs.keySet()) {
			final Object value = outputs.get(output);
			// TODO: Convert output value to OMERO-compatible format and feed it.
		}
		if (verbose) ij.log().info(stub + ": completed execution");
	}

	// -- Helper methods --

	/**
	 * Gets the identifier for the given object, or null if the object is not
	 * {@link Identifiable}.
	 */
	private String getID(final Object o) {
		if (!(o instanceof Identifiable)) {
			return null;
		}
		return ((Identifiable) o).getIdentifier();
	}

	/** Verifies that the combination of arguments given is reasonable. */
	private void validateArgs(final CmdLineParser parser) throws CmdLineException
	{
		if (!generate && stubToParse == null && stubToExecute == null) {
			throw new CmdLineException(parser, "No commands given");
		}
	}

	/** Generates a stub file corresponding to the given {@link ModuleInfo}. */
	private File generateStub(final String id, final int no) throws IOException {
		final ModuleInfo info = ids.get(id);
		if (info == null) throw new IOException("No module for identifier: " + id);

		// TODO: Make the path configurable.
		final String stubPath = "ij-stubs/" + no + ".ij-stub";

		// write out the stub
		final File stub = new File(stubPath);
		final DataOutputStream out =
			new DataOutputStream(new FileOutputStream(stub));
		out.writeUTF(id);
		out.close();
		return stub;
	}

	/** Identifies the {@link ModuleInfo} associated with the given stub file. */
	private ModuleInfo findInfo(final File stub) throws IOException {
		final DataInputStream in = new DataInputStream(new FileInputStream(stub));
		final String id = in.readUTF();
		in.close();
		return ids.get(id);
	}

	/**
	 * Converts the given {@link ModuleInfo} to an OMERO-compatible format. and
	 * feeds it to OMERO.
	 */
	private OMEROJobParams convert(final ModuleInfo info) {
		// populate module metadata
		final String version = getVersion(info);
		final String name = info.getName();
		final String label = info.getLabel();
		final String description = info.getDescription();
		final String title = info.getTitle();
		// TODO: Populate OMERO data structure with the above fields.

		// TODO: Instantiate and preprocess the module, excluding resolved inputs.
		// convert metadata for each module input
		for (final ModuleItem<?> input : info.inputs()) {
			convert(input);
		}
		// TODO: Double check how OMERO wants to handle input+output parameters.
		// convert metadata for each module output
		for (final ModuleItem<?> output : info.outputs()) {
			convert(output);
		}
		return null;
	}

	/** Converts the given {@link ModuleItem} to an OMERO-compatible format. */
	private OMEROParam convert(final ModuleItem<?> input) {
		final String name = input.getName();
		final String label = input.getLabel();
		final String description = input.getDescription();
		final Class<?> type = input.getType();
		// TODO: Populate OMERO data structure with the above fields.
		return null;
	}

	/** Uploads OMERO job parameters to OMERO. */
	private void upload(final OMEROJobParams omeroJobParams) {
		// TODO: Feed completed OMERO data structure to OMERO.
	}

	/**
	 * Extracts the version of the given module, by scanning the relevant JAR
	 * manifest.
	 * 
	 * @return The <code>Implementation-Version</code> of the associated JAR
	 *         manifest; or if there is no associated JAR manifest, or something
	 *         else goes wrong, returns null.
	 */
	private String getVersion(final ModuleInfo info) {
		final Class<?> c;
		try {
			c = info.createModule().getDelegateObject().getClass();
		}
		catch (final ModuleException exc) {
			if (verbose) exc.printStackTrace();
			return null;
		}
		final Manifest m = Manifest.getManifest(c);
		if (m == null) return null;
		return m.getImplementationVersion();
	}

}
