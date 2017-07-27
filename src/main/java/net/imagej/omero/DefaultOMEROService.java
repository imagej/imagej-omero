/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2016 Open Microscopy Environment:
 * 	- Board of Regents of the University of Wisconsin-Madison
 * 	- Glencoe Software, Inc.
 * 	- University of Dundee
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

package net.imagej.omero;

import io.scif.Metadata;
import io.scif.services.DatasetIOService;

import java.io.IOException;
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
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.table.Column;
import net.imagej.table.GenericTable;
import net.imagej.table.Table;

import org.scijava.Optional;
import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.util.ClassUtils;
import org.scijava.util.ConversionUtils;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;

/**
 * Default ImageJ service for managing OMERO data conversion.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultOMEROService extends AbstractService implements
	OMEROService, Optional
{

	// -- Parameters --

	@Parameter
	private LogService log;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ObjectService objectService;

	@Parameter
	private ConvertService convertService;

	// -- OMEROService methods --

	@Override
	public omero.grid.Param getJobParam(final ModuleItem<?> item) {
		final omero.grid.Param param = new omero.grid.Param();
		param.optional = !item.isRequired();
		param.prototype = prototype(item.getType());
		param.description = item.getDescription();
		final List<?> choices = item.getChoices();
		if (choices != null && !choices.isEmpty()) {
			param.values = (omero.RList) toOMERO(choices);
		}
		final Object min = item.getMinimumValue();
		if (min != null) param.min = toOMERO(min);
		final Object max = item.getMaximumValue();
		if (max != null) param.max = toOMERO(max);
		return param;
	}

	@Override
	public omero.RType prototype(final Class<?> type) {
		// image types
		if (Dataset.class.isAssignableFrom(type) ||
			DatasetView.class.isAssignableFrom(type) ||
			ImageDisplay.class.isAssignableFrom(type))
		{
			// use an image ID
			return omero.rtypes.rlong(0);
		}

		// primitive types
		final Class<?> saneType = ConversionUtils.getNonprimitiveType(type);
		if (Boolean.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rbool(false);
		}
		if (Double.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rdouble(Double.NaN);
		}
		if (Float.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rfloat(Float.NaN);
		}
		if (Integer.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rint(0);
		}
		if (Long.class.isAssignableFrom(saneType)) {
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
		// - java.io.File
		// - java.lang.Character
		// - java.lang.String
		// - java.math.BigDecimal
		// - java.math.BigInteger
		// - org.scijava.util.ColorRGB
		return omero.rtypes.rstring("");
	}

	@Override
	public omero.RType toOMERO(final Object value) {
		if (value == null) return null;

		// NB: Unfortunately, omero.rtypes.rtype is not smart enough
		// to recurse into data structures, so we do it ourselves!

		// TODO: Use omero.rtypes.wrap, now that it exists!
		// https://github.com/openmicroscopy/openmicroscopy/commit/0767a2e37996d553bbdec343488b7b385756490a

		if (value.getClass().isArray()) {
			final omero.RType[] val = new omero.RType[Array.getLength(value)];
			for (int i = 0; i < val.length; i++) {
				val[i] = toOMERO(Array.get(value, i));
			}
			return omero.rtypes.rarray(val);
		}
		if (value instanceof List) {
			final List<?> list = (List<?>) value;
			final omero.RType[] val = new omero.RType[list.size()];
			for (int i = 0; i < val.length; i++) {
				val[i] = toOMERO(list.get(i));
			}
			return omero.rtypes.rlist(val);
		}
		if (value instanceof Map) {
			final Map<?, ?> map = (Map<?, ?>) value;
			final HashMap<String, omero.RType> val =
				new HashMap<String, omero.RType>();
			for (final Object key : map.keySet()) {
				val.put(key.toString(), toOMERO(map.get(key)));
			}
			return omero.rtypes.rmap(val);
		}
		if (value instanceof Set) {
			final Set<?> set = (Set<?>) value;
			final omero.RType[] val = new omero.RType[set.size()];
			int index = 0;
			for (final Object element : set) {
				val[index++] = toOMERO(element);
			}
			return omero.rtypes.rset(val);
		}

		// try generic OMEROification routine
		try {
			return omero.rtypes.rtype(value);
		}
		catch (final omero.ClientError err) {
			// default case: convert to string
			return omero.rtypes.rstring(value.toString());
		}
	}

	@Override
	public omero.RType toOMERO(final omero.client client, final Object value)
		throws omero.ServerError, IOException, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		if (value instanceof Dataset) {
			// upload image to OMERO, returning the resultant image ID
			final long imageID = uploadImage(client, (Dataset) value);
			return toOMERO(client, imageID);
		}
		if (value instanceof DatasetView) {
			final DatasetView datasetView = (DatasetView) value;
			// TODO: Verify whether any view-specific metadata can be preserved.
			return toOMERO(client, datasetView.getData());
		}
		if (value instanceof ImageDisplay) {
			final ImageDisplay imageDisplay = (ImageDisplay) value;
			// TODO: Support more aspects of image displays; e.g., multiple datasets.
			return toOMERO(client, imageDisplayService.getActiveDataset(imageDisplay));
		}
		if (value instanceof Table) {
			final OMEROCredentials cred = createCredentials(client);
			final long tableID = uploadTable(cred, "table", (Table<?, ?>) value, 0);
			return toOMERO(client, tableID);
		}
		return toOMERO(value);
	}

	@Override
	public Object toImageJ(final omero.client client, final omero.RType value,
		final Class<?> type) throws omero.ServerError, IOException,
		PermissionDeniedException, CannotCreateSessionException, SecurityException,
		DSOutOfServiceException, ExecutionException, DSAccessException
	{
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
				final Object converted = toImageJ(client, rType, null);
				if (converted != null) element = converted;
				collection.add(converted);
			}
			if (value instanceof omero.RArray) {
				// convert from Collection to array of the appropriate type
				if (element == null) {
					// unknown type
					return collection.toArray();
				}
				// typed on 1st element
				return toArray(collection, element.getClass());
			}
			// not an array, but a bona fide collection
			return collection;
		}
		if (value instanceof omero.RMap) {
			// map of objects
			final Map<String, omero.RType> omeroMap = ((omero.RMap) value).getValue();
			final Map<String, Object> map = new HashMap<String, Object>();
			for (final String key : omeroMap.keySet()) {
				map.put(key, toImageJ(client, omeroMap.get(key), null));
			}
			return map;
		}

		// HACK: Use getValue() method if one exists for this type.
		// Reflection is necessary because there is no common interface
		// with the getValue() method implemented by each subclass.
		try {
			final Method method = value.getClass().getMethod("getValue");
			final Object result = method.invoke(value);
			return convert(client, result, type);
		}
		catch (final NoSuchMethodException exc) {
			log.debug(exc);
		}
		catch (final IllegalArgumentException exc) {
			log.error(exc);
		}
		catch (final IllegalAccessException exc) {
			log.error(exc);
		}
		catch (final InvocationTargetException exc) {
			log.error(exc);
		}
		log.error("Unsupported type: " + value.getClass().getName());
		return null;
	}

	@Override
	public Dataset downloadImage(final omero.client client, final long imageID)
		throws omero.ServerError, IOException
	{
		// TODO: Reuse existing client instead of creating a new connection.
		// Will need to rethink how SCIFIO conveys source and destination metadata.
		// The RandomAccessInput/OutputStream design is probably too narrow.
		final String omeroSource =
			"omero:" + credentials(client) + "&imageID=" + imageID;

		return datasetIOService.open(omeroSource);
	}

	@Override
	public long uploadImage(final omero.client client, final Dataset dataset)
		throws omero.ServerError, IOException
	{
		// TODO: Reuse existing client instead of creating a new connection.
		// Will need to rethink how SCIFIO conveys source and destination metadata.
		// The RandomAccessInput/OutputStream design is probably too narrow.
		final String omeroDestination =
			"name=" + dataset.getName() + "&" + credentials(client) //
				+ ".omero"; // FIXME: Remove this after SCIFIO doesn't need it anymore.

		final Metadata metadata = datasetIOService.save(dataset, omeroDestination);

		if (metadata instanceof OMEROFormat.Metadata) {
			final OMEROFormat.Metadata omeroMeta = (OMEROFormat.Metadata) metadata;
			return omeroMeta.getImageID();
		}
		return -1;
	}

	@Override
	public long uploadTable(final OMEROCredentials credentials, final String name,
		final Table<?, ?> imageJTable, final long imageID) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		long id = -1;
		try (final OMEROSession session = new DefaultOMEROSession(credentials)) {
			final TableDataColumn[] omeroColumns = new TableDataColumn[imageJTable
				.getColumnCount()];
			final Object[][] data = new Object[imageJTable.getColumnCount()][];

			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
				omeroColumns[c] = TableUtils.createOMEROColumn(imageJTable.get(c), c);
				data[c] = TableUtils.populateOMEROColumn(imageJTable.get(c),
					convertService);
			}

			// Get image
			final BrowseFacility browseFacility = session.getGateway().getFacility(
				BrowseFacility.class);
			final ImageData image = browseFacility.getImage(session
				.getSecurityContext(), imageID);

			// Create table and attach to image
			final TableData omeroTable = new TableData(omeroColumns, data);
			omeroTable.setNumberOfRows(imageJTable.getRowCount());
			final TablesFacility tablesFacility = session.getGateway().getFacility(
				TablesFacility.class);
			final TableData stored = tablesFacility.addTable(session
				.getSecurityContext(), image, name, omeroTable);
			id = stored.getOriginalFileId();
		}
		return id;
	}

	@Override
	public Table<?, ?> downloadTable(final OMEROCredentials credentials,
		final long tableID) throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		try (final OMEROSession session = new DefaultOMEROSession(credentials)) {
			final TablesFacility tableService = session.getGateway().getFacility(
				TablesFacility.class);
			final TableData table = tableService.getTable(session
				.getSecurityContext(), tableID, 0, Integer.MAX_VALUE - 1);

			final TableDataColumn[] omeroColumns = table.getColumns();
			final Object[][] data = table.getData();

			final Table<?, ?> imageJTable = TableUtils.createImageJTable(
				omeroColumns);
			imageJTable.setRowCount((int) table.getNumberOfRows());

			boolean colsCreated = false;
			if (!(imageJTable instanceof GenericTable)) {
				imageJTable.appendColumns(omeroColumns.length);
				colsCreated = true;
			}

			for (int i = 0; i < omeroColumns.length; i++) {
				if (!colsCreated) {
					final Column<?> imageJCol = TableUtils.createImageJColumn(
						omeroColumns[i]);
					TableUtils.populateImageJColumn(omeroColumns[i].getType(),
						data[omeroColumns[i].getIndex()], imageJCol);
					((GenericTable) imageJTable).add(omeroColumns[i].getIndex(),
						imageJCol);
				}
				else {
					TableUtils.populateImageJColumn(omeroColumns[i].getType(),
						data[omeroColumns[i].getIndex()], imageJTable.get(i));
					imageJTable.get(i).setHeader(omeroColumns[i].getName());
				}
			}
			return imageJTable;
		}
	}

	// -- Helper methods --

	/**
	 * Generates an OMERO source string fragment with credentials matching the
	 * given client.
	 */
	private static String credentials(final omero.client client) {
		return "server=" + client.getProperty("omero.host") + //
			"&port=" + client.getProperty("omero.port") + //
			"&sessionID=" + client.getSessionId();
	}

	/**
	 * Converts the given POJO to the specified type (if given).
	 * <p>
	 * This method handles coersion of POJOs unwrapped from OMERO into the
	 * relevant type needed by ImageJ. Examples:
	 * </p>
	 * <ol>
	 * <li>Many ImageJ types (such as {@link org.scijava.util.ColorRGB}) are
	 * mapped to {@link String} for use with OMERO. We lean on the SciJava Common
	 * {@link ConvertService#convert(Object, Class)} method to handle conversion
	 * of such types back to ImageJ's expected type for the parameter.</li>
	 * <li>ImageJ's image types (i.e., {@link Dataset}, {@link DatasetView} and
	 * {@link ImageDisplay}) are mapped to {@code long} since OMERO communicates
	 * about images using image IDs. Work must be done to download the image from
	 * a specified ID and convert the result to the appropriate type of ImageJ
	 * object such as {@link Dataset}.</li>
	 * </ol>
	 *
	 * @throws CannotCreateSessionException
	 * @throws PermissionDeniedException
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 * @throws ExecutionException
	 */
	private <T> T convert(final omero.client client, final Object value,
		final Class<T> type) throws omero.ServerError, IOException,
		PermissionDeniedException, CannotCreateSessionException,
		DSOutOfServiceException, ExecutionException, DSAccessException
	{
		if (value == null) return null;
		if (type == null) {
			// no type given; try a simple cast
			@SuppressWarnings("unchecked")
			final T typedResult = (T) value;
			return typedResult;
		}

		// First, we look for registered objects of the requested type whose
		// toString() value matches the given string. This allows known sorts of
		// objects to be requested by name, including SingletonPlugin types like
		// CalculatorOp and ThresholdMethod.
		if (value instanceof String) {
			final String s = (String) value;
			final List<T> objects = objectService.getObjects(type);
			for (final T object : objects) {
				if (s.equals(object.toString())) return object;
			}
		}

		// special case for converting an OMERO image ID to an ImageJ image type
		if (ClassUtils.isNumber(value.getClass())) {
			if (Dataset.class.isAssignableFrom(type)) {
				final long imageID = ((Number) value).longValue();
				// TODO: Consider consequences of this cast more carefully.
				@SuppressWarnings("unchecked")
				final T dataset = (T) downloadImage(client, imageID);
				return dataset;
			}
			if (DatasetView.class.isAssignableFrom(type)) {
				final Dataset dataset = convert(client, value, Dataset.class);
				@SuppressWarnings("unchecked")
				final T dataView = (T) imageDisplayService.createDataView(dataset);
				return dataView;
			}
			if (ImageDisplay.class.isAssignableFrom(type)) {
				final Dataset dataset = convert(client, value, Dataset.class);
				@SuppressWarnings("unchecked")
				final T display = (T) displayService.createDisplay(dataset);
				return display;
			}
			if (Table.class.isAssignableFrom(type)) {
				final long tableID = ((Number) value).longValue();
				final OMEROCredentials credentials = createCredentials(client);
				@SuppressWarnings("unchecked")
				final T table = (T) downloadTable(credentials, tableID);
				return table;
			}
		}

		// use SciJava Common's automagical conversion routine
		final T converted = convertService.convert(value, type);
		if (converted == null) {
			log.error("Cannot convert: " + value.getClass().getName() + " to " +
				type.getName());
		}
		return converted;
	}

	/** Converts a {@link Collection} to an array of the given type. */
	private static <T> T[] toArray(final Collection<Object> collection,
		final Class<T> type)
	{
		@SuppressWarnings("unchecked")
		final T[] array = (T[]) Array.newInstance(type, 0);
		return collection.toArray(array);
	}

	private OMEROCredentials createCredentials(final omero.client client) {
		final OMEROCredentials credentials = new OMEROCredentials();
		credentials.setServer(client.getProperty("omero.host"));
		credentials.setPort(Integer.parseInt(client.getProperty("omero.port")));
		credentials.setUser(client.getProperty("omero.user"));
		credentials.setPassword(client.getProperty("omero.pass"));
		return credentials;
	}
}
