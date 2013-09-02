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
import imagej.data.Dataset;
import imagej.data.display.DatasetView;
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.display.DisplayService;
import imagej.module.Module;
import imagej.module.ModuleException;
import imagej.module.ModuleInfo;
import imagej.module.ModuleItem;
import imagej.module.ModuleService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Future;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.util.ClassUtils;
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
	private DisplayService displayService;

	@Parameter
	private ImageDisplayService imageDisplayService;

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
		client.setOutput("omero.scripts.parse", convertInfo());
	}

	/** Executes an ImageJ module, for the specified OMERO session. */
	public void launch() throws omero.ServerError {
		// populate inputs
		log.debug(info.getTitle() + ": populating inputs");
		final HashMap<String, Object> inputMap = new HashMap<String, Object>();
		for (final String name : client.getInputKeys()) {
			final Class<?> type = info.getInput(name).getType();
			inputMap.put(name, convertValue(client.getInput(name), type));
		}

		// execute ImageJ module
		log.debug(info.getTitle() + ": executing module");
		final Future<Module> future = commandService.run(info, inputMap);
		final Module module = moduleService.waitFor(future);

		// populate outputs
		log.debug(info.getTitle() + ": populating outputs");
		for (final ModuleItem<?> item : module.getInfo().outputs()) {
			client.setOutput(item.getName(), convertValue(item.getValue(module)));
		}

		log.debug(info.getTitle() + ": completed execution");
	}

	/** Converts ImageJ module metadata to OMERO job metadata. */
	public omero.RType convertInfo() {
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
			final omero.grid.Param param = ConversionUtils.convertItem(item);
			if (param != null) params.inputs.put(item.getName(), param);
		}

		// convert metadata for each module output
		params.outputs = new HashMap<String, omero.grid.Param>();
		for (final ModuleItem<?> item : info.outputs()) {
			final omero.grid.Param param = ConversionUtils.convertItem(item);
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
			return convertValue(imageDisplayService.getActiveDataset(imageDisplay));
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
				log.error("Unsupported collection: " + value.getClass().getName());
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
				if (element == null) {
					// unknown type
					return collection.toArray();
				}
				// typed on 1st element
				return ConversionUtils.toArray(collection, element.getClass());
			}
			// not an array, but a bona fide collection
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
			log.debug(exc);
		}
		catch (final IllegalArgumentException exc) {
			log.warn(exc);
		}
		catch (final IllegalAccessException exc) {
			log.warn(exc);
		}
		catch (final InvocationTargetException exc) {
			log.warn(exc);
		}
		log.error("Unsupported type: " + value.getClass().getName());
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
	private <T> T convertToType(final Object result, final Class<T> type) {
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
				final T dataView = (T) imageDisplayService.createDataView(dataset);
				return dataView;
			}
			if (ImageDisplay.class.isAssignableFrom(type)) {
				final Dataset dataset = convertToType(result, Dataset.class);
				@SuppressWarnings("unchecked")
				final T display = (T) displayService.createDisplay(dataset);
				return display;
			}
		}
		log.error("Cannot convert: " + result.getClass().getName() + " to " +
			type.getName());
		return null;
	}

}
