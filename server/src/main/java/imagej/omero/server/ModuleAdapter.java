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

import imagej.command.Command;
import imagej.command.CommandService;
import imagej.module.Module;
import imagej.module.ModuleException;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;
import imagej.module.ModuleService;

import java.util.HashMap;
import java.util.concurrent.Future;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.ItemVisibility;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.util.Manifest;

/**
 * Adapts an ImageJ {@link Module} (such as a {@link Command}) to be usable as
 * an OMERO script, converting information between ImageJ- and OMERO-compatible
 * formats as appropriate.
 * 
 * @author Curtis Rueden
 */
public class ModuleAdapter extends AbstractContextual {

	// -- Parameters --

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private ModuleService moduleService;

	// -- Fields --

	/** The {@link ModuleInfo} associated with this adapter. */
	private final ModuleInfo info;

	/** The OMERO client to use when communicating about a job. */
	private final omero.client client;

	// -- Constructor --

	public ModuleAdapter(final Context context, final ModuleInfo info,
		final omero.client client)
	{
		setContext(context);
		this.info = info;
		this.client = client;
	}

	// -- ModuleAdapter methods --

	/** Parses ImageJ module parameters, for the specified OMERO session. */
	public void params() throws omero.ServerError {
		// Parsing. See OmeroPy/src/omero/scripts.py
		// for the Python implementation.
		// =========================================
		client.setOutput("omero.scripts.parse", getJobInfo());
	}

	/** Executes an ImageJ module, for the specified OMERO session. */
	public void launch() throws omero.ServerError {
		// populate inputs
		log.debug(info.getTitle() + ": populating inputs");
		final HashMap<String, Object> inputMap = new HashMap<String, Object>();
		for (final String name : client.getInputKeys()) {
			final ModuleItem<?> item = info.getInput(name);
			if (item.getVisibility() == ItemVisibility.MESSAGE) continue;
			final Class<?> type = item.getType();
			final Object value = omeroService.convert(client.getInput(name), type);
			inputMap.put(name, value);
		}

		// execute ImageJ module
		log.debug(info.getTitle() + ": executing module");
		final Future<Module> future = commandService.run(info, inputMap);
		final Module module = moduleService.waitFor(future);

		// populate outputs
		log.debug(info.getTitle() + ": populating outputs");
		for (final ModuleItem<?> item : module.getInfo().outputs()) {
			final omero.RType value = omeroService.convert(item.getValue(module));
			client.setOutput(item.getName(), value);
		}

		log.debug(info.getTitle() + ": completed execution");
	}

	/** Converts ImageJ module metadata to OMERO job metadata. */
	public omero.RType getJobInfo() {
		// populate module metadata
		final omero.grid.JobParams params = new omero.grid.JobParams();
		params.name = "[ImageJ] " + info.getTitle(); // info.getName();
		params.version = getVersion();
		params.description = info.getDescription();
		params.stdoutFormat = "text/plain";
		params.stderrFormat = "text/plain";

		// TODO: Instantiate and preprocess the module, excluding resolved inputs.

		// convert metadata for each module input
		params.inputs = new HashMap<String, omero.grid.Param>();
		for (final ModuleItem<?> item : info.inputs()) {
			final omero.grid.Param param = omeroService.getJobParam(item);
			if (param != null) params.inputs.put(item.getName(), param);
		}

		// convert metadata for each module output
		params.outputs = new HashMap<String, omero.grid.Param>();
		for (final ModuleItem<?> item : info.outputs()) {
			final omero.grid.Param param = omeroService.getJobParam(item);
			if (param != null) params.outputs.put(item.getName(), param);
		}

		return omero.rtypes.rinternal(params);
	}

	/**
	 * Extracts the version of the given module, by scanning the relevant JAR
	 * manifest.
	 * 
	 * @return The <code>Implementation-Version</code> of the associated JAR
	 *         manifest; or if there is no associated JAR manifest, or something
	 *         else goes wrong, returns null.
	 */
	public String getVersion() {
		final Class<?> c;
		try {
			c = info.createModule().getDelegateObject().getClass();
		}
		catch (final ModuleException exc) {
			log.debug(exc);
			return null;
		}
		final Manifest m = Manifest.getManifest(c);
		if (m == null) return null;
		return m.getImplementationVersion();
	}

}
