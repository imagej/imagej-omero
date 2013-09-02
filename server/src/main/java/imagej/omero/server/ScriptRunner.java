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
import imagej.data.Dataset;
import imagej.data.display.DatasetView;
import imagej.data.display.ImageDisplay;
import imagej.module.Module;
import imagej.module.ModuleException;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.util.ClassUtils;
import org.scijava.util.Manifest;

/**
 * Executes ImageJ {@link Module}s as OMERO scripts.
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
			final Class<?> type = info.getInput(name).getType();
			inputMap.put(name, convertValue(c.getInput(name), type));
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
		// image types
		if (Dataset.class.isAssignableFrom(type) ||
			DatasetView.class.isAssignableFrom(type) ||
			ImageDisplay.class.isAssignableFrom(type))
		{
			// use a pixels ID
			return omero.rtypes.rlong(0);
		}

		// primitive types
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

		// data structure types
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
		// works for many types, including but not limited to:
		// - char
		// - imagej.util.ColorRGB
		// - java.io.File
		// - java.lang.Character
		// - java.lang.String
		// - java.math.BigDecimal
		// - java.math.BigInteger
		return omero.rtypes.rstring("");
	}

	/** Converts an ImageJ parameter value to an OMERO parameter value. */
	public omero.RType convertValue(final Object value) {
		if (value instanceof Dataset) {
			// TODO: Extract pixels and upload to OMERO.
			return null;
		}
		else if (value instanceof DatasetView) {
			final DatasetView datasetView = (DatasetView) value;
			// TODO: Verify whether any view-specific metadata can be preserved.
			return convertValue(datasetView.getData());
		}
		else if (value instanceof ImageDisplay) {
			final ImageDisplay imageDisplay = (ImageDisplay) value;
			// TODO: Support more aspects of image displays; e.g., multiple datasets.
			return convertValue(ij.imageDisplay().getActiveDataset(imageDisplay));
		}
		else {
			// try generic conversion method
			try {
				return omero.rtypes.rtype(value);
			}
			catch (final omero.ClientError err) {
				// default case: convert to string
				return omero.rtypes.rstring(value.toString());
			}
		}
	}

	/** Converts an OMERO parameter value to an ImageJ value of the given type. */
	public Object convertValue(final omero.RType value, final Class<?> type) {
		if (value instanceof omero.RCollection) {
			// collection of objects
			final Collection<omero.RType> omeroCollection =
				((omero.RCollection) value).getValue();
			final Collection<Object> collection;
			if (value instanceof omero.RArray || value instanceof omero.RList) {
				// NB: See special handling for omero.RArray below.
				collection = new ArrayList<Object>();
			}
			else if (value instanceof omero.RSet) {
				collection = new HashSet<Object>();
			}
			else {
				ij.log().error("Unsupported collection: " + value.getClass().getName());
				return null;
			}
			// convert elements recursively
			Object element = null; // NB: Save 1st non-null element for later use.
			for (final omero.RType rType : omeroCollection) {
				final Object converted = convertValue(rType, null);
				if (element != null) element = converted;
				collection.add(converted);
			}
			if (value instanceof omero.RArray) {
				// convert from Collection to array of the appropriate type
				if (element == null) return collection.toArray(); // unknown type
				return toArray(collection, element.getClass()); // typed on 1st element
			}
			return collection;
		}
		if (value instanceof omero.RMap) {
			// map of objects
			final Map<String, omero.RType> omeroMap = ((omero.RMap) value).getValue();
			final Map<String, Object> map = new HashMap<String, Object>();
			for (final String key : omeroMap.keySet()) {
				map.put(key, convertValue(omeroMap.get(key), null));
			}
			return map;
		}

		// Use getValue() method if one exists for this type.
		// Reflection is necessary because there is no common interface
		// with the getValue() method implemented by each subclass.
		try {
			final Method method = value.getClass().getMethod("getValue");
			final Object result = method.invoke(value);
			return convertToType(result, type);
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
	 * Converts the given POJO to the specified type (if given).
	 * <p>
	 * This method mainly exists to support OMERO pixel IDs; i.e., conversion of
	 * numbers to ImageJ image types.
	 * </p>
	 */
	private <T> T convertToType(Object result, Class<T> type) {
		if (result == null) return null;
		if (type == null || type.isInstance(result)) {
			@SuppressWarnings("unchecked")
			final T typedResult = (T) result;
			return typedResult;
		}
		if (ClassUtils.isNumber(result.getClass())) {
			if (Dataset.class.isAssignableFrom(type)) {
				// FIXME: Implement this.
				return null;
			}
			if (DatasetView.class.isAssignableFrom(type)) {
				final Dataset dataset = convertToType(result, Dataset.class);
				@SuppressWarnings("unchecked")
				final T dataView = (T) ij.imageDisplay().createDataView(dataset);
				return dataView;
			}
			if (ImageDisplay.class.isAssignableFrom(type)) {
				final Dataset dataset = convertToType(result, Dataset.class);
				@SuppressWarnings("unchecked")
				final T display = (T) ij.display().createDisplay(dataset);
				return display;
			}
		}
		ij.log().error(
			"Cannot convert: " + result.getClass().getName() + " to " +
				type.getName());
		return null;
	}

	/** Converts a {@link Collection} to an array of the given type. */
	private <T> T[] toArray(final Collection<Object> collection,
		final Class<T> type)
	{
		final Object array = Array.newInstance(type, 0);
		@SuppressWarnings("unchecked")
		final T[] typedArray = (T[]) array;
		return collection.toArray(typedArray);
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
			ij.log().debug(exc);
			return null;
		}
		final Manifest m = Manifest.getManifest(c);
		if (m == null) return null;
		return m.getImplementationVersion();
	}

}
