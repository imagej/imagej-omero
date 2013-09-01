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

import imagej.ImageJ;
import imagej.command.CommandInfo;
import imagej.core.commands.debug.SystemInformation;
import imagej.module.Module;
import imagej.module.ModuleException;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.util.Manifest;

/**
 * Helper class for executing ImageJ {@link Module}s as OMERO scripts.
 * 
 * @author Curtis Rueden
 * @see "https://www.openmicroscopy.org/site/support/omero4/developers/Modules/Scripts.html"
 */
public class ScriptRunner extends AbstractContextual {

	// -- Instance fields --

	/** The ImageJ application gateway. */
	private final ImageJ ij;

	// -- Constructors --

	public ScriptRunner() {
		this(new ImageJ());
	}

	public ScriptRunner(final Context context) {
		this(new ImageJ(context));
	}

	public ScriptRunner(final ImageJ ij) {
		this.ij = ij;
		setContext(ij.getContext());
	}

	// -- ScriptRunner methods --

	/** Gets the ImageJ application gateway used by this module executor. */
	public ImageJ ij() {
		return ij;
	}

	/** Toplevel invocation entry point from OMERO Jython scripts. */
	public void invoke(final String command) throws omero.ServerError,
		Glacier2.CannotCreateSessionException, Glacier2.PermissionDeniedException
	{
		ij.log().debug("invoke: " + command);

		// dump system properties, for debugging purposes
		ij.log().debug("System properties:");
		ij.log().debug(SystemInformation.getSystemProperties());

		// look up the requested command (FIXME: support non-command modules too)
		final CommandInfo info = ij.command().getCommand(command);

		// initialize OMERO client session
		final omero.client c = new omero.client();

		// perform appropriate action (either parse or launch)
		try {
			c.createSession().detachOnDestroy();

			final String parse = c.getProperty("omero.scripts.parse");
			if (!parse.isEmpty()) params(c, info);
			else launch(c, info);
		}
		finally {
			c.__del__();
		}
	}

	/** Parses ImageJ module parameters, for the specified OMERO session. */
	public void params(final omero.client c, final ModuleInfo info)
		throws omero.ServerError
	{
		// Parsing. See OmeroPy/src/omero/scripts.py
		// for the Python implementation.
		// =========================================
		c.setOutput("omero.scripts.parse", convertInfo(info));
	}

	/** Executes an ImageJ module, for the specified OMERO session. */
	public void launch(final omero.client c, final ModuleInfo info)
		throws omero.ServerError
	{
		// populate inputs
		ij.log().debug(info.getTitle() + ": populating inputs");
		final HashMap<String, Object> inputMap = new HashMap<String, Object>();
		for (final String name : c.getInputKeys()) {
			inputMap.put(name, convertValue(c.getInput(name)));
		}

		// execute ImageJ module
		ij.log().debug(info.getTitle() + ": executing module");
		final Future<Module> future = ij.command().run(info, inputMap);
		final Module module = ij.module().waitFor(future);

		// populate outputs
		ij.log().debug(info.getTitle() + ": populating outputs");
		for (final ModuleItem<?> item : module.getInfo().outputs()) {
			c.setOutput(item.getName(), convertValue(item.getValue(module)));
		}

		ij.log().debug(info.getTitle() + ": completed execution");
	}

	/** Converts ImageJ module metadata to OMERO job metadata. */
	public omero.RType convertInfo(final ModuleInfo info) {
		// populate module metadata
		final omero.grid.JobParams params = new omero.grid.JobParams();
		params.name = "[ImageJ] " + info.getTitle(); // info.getName();
		params.version = getVersion(info);
		params.description = info.getDescription();
		params.stdoutFormat = "text/plain";
		params.stderrFormat = "text/plain";

		// TODO: Instantiate and preprocess the module, excluding resolved inputs.

		// convert metadata for each module input
		params.inputs = new HashMap<String, omero.grid.Param>();
		for (final ModuleItem<?> item : info.inputs()) {
			final omero.grid.Param param = convertItem(item);
			if (param != null) params.inputs.put(item.getName(), param);
		}

		// convert metadata for each module output
		params.outputs = new HashMap<String, omero.grid.Param>();
		for (final ModuleItem<?> item : info.outputs()) {
			final omero.grid.Param param = convertItem(item);
			if (param != null) params.outputs.put(item.getName(), param);
		}

		return omero.rtypes.rinternal(params);
	}

	/** Converts ImageJ parameter metadata to OMERO parameter metadata. */
	public omero.grid.Param convertItem(final ModuleItem<?> item) {
		final omero.grid.Param param = new omero.grid.Param();
		param.optional = !item.isRequired();
		param.prototype = prototype(item.getType());
		param.description = item.getDescription();
		return param;
	}

	/** Creates an OMERO parameter prototype for the given Java class. */
	public omero.RType prototype(final Class<?> type) {
		// TODO: Handle more cases.
		if (Boolean.class.isAssignableFrom(type)) {
			return omero.rtypes.rbool(false);
		}
		if (Double.class.isAssignableFrom(type)) {
			return omero.rtypes.rdouble(Double.NaN);
		}
		if (Float.class.isAssignableFrom(type)) {
			return omero.rtypes.rfloat(Float.NaN);
		}
		if (Integer.class.isAssignableFrom(type)) {
			return omero.rtypes.rint(0);
		}
		if (Long.class.isAssignableFrom(type)) {
			return omero.rtypes.rlong(0L);
		}
		if (type.isArray()) {
			return omero.rtypes.rarray();
		}
		if (List.class.isAssignableFrom(type)) {
			return omero.rtypes.rlist();
		}
		if (Map.class.isAssignableFrom(type)) {
			return omero.rtypes.rmap();
		}
		if (Set.class.isAssignableFrom(type)) {
			return omero.rtypes.rset();
		}
		// default case: convert to string
		return omero.rtypes.rstring("");
	}

	/** Converts an ImageJ parameter value to an OMERO parameter value. */
	public omero.RType convertValue(final Object value) {
		// TODO: Handle more cases.

		// try generic conversion method
		try {
			return omero.rtypes.rtype(value);
		}
		catch (final omero.ClientError err) {
			// default case: convert to string
			return omero.rtypes.rstring(value.toString());
		}

	}

	/** Converts an OMERO parameter value to an ImageJ parameter value. */
	public Object convertValue(final omero.RType value) {
		// Use getValue() method if one exists for this type.
		// This is necessary because there is no common interface
		// with the getValue() method implemented by each subclass.
		try {
			final Method method = value.getClass().getMethod("getValue");
			return method.invoke(value);
		}
		catch (final NoSuchMethodException exc) {
			ij.log().debug(exc);
		}
		catch (final IllegalArgumentException exc) {
			ij.log().warn(exc);
		}
		catch (final IllegalAccessException exc) {
			ij.log().warn(exc);
		}
		catch (final InvocationTargetException exc) {
			ij.log().warn(exc);
		}
		ij.log().error("Unsupported type: " + value.getClass().getName());
		return null;
	}

	// -- Helper methods --

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
			ij.log().debug(exc);
			return null;
		}
		final Manifest m = Manifest.getManifest(c);
		if (m == null) return null;
		return m.getImplementationVersion();
	}

}
