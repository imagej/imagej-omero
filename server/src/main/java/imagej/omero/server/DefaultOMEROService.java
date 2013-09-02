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

import imagej.data.Dataset;
import imagej.data.display.DatasetView;
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.display.DisplayService;
import imagej.module.ModuleItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.util.ClassUtils;

/**
 * Default ImageJ service for managing OMERO data conversion.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultOMEROService extends AbstractService implements
	OMEROService
{

	// -- Parameters --

	@Parameter
	private LogService log;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	// -- OMEROService methods --

	@Override
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

	@Override
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

	@Override
	public omero.grid.Param getJobParam(final ModuleItem<?> item) {
		final omero.grid.Param param = new omero.grid.Param();
		param.optional = !item.isRequired();
		param.prototype = prototype(item.getType());
		param.description = item.getDescription();
		return param;
	}

	@Override
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
