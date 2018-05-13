/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2018 Open Microscopy Environment:
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.omero.roi.OMEROROICollection;
import net.imagej.roi.DefaultROITree;
import net.imagej.roi.ROITree;
import net.imagej.table.Column;
import net.imagej.table.GenericTable;
import net.imagej.table.Table;
import net.imagej.table.TableDisplay;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.roi.Mask;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.type.logic.BoolType;
import net.imglib2.view.Views;

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
import org.scijava.util.DefaultTreeNode;
import org.scijava.util.TreeNode;
import org.scijava.util.Types;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.api.IQueryPrx;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ROIResult;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.gateway.model.TagAnnotationData;
import omero.model.TagAnnotationI;
import omero.sys.Filter;

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

//-- Fields --

	private final HashMap<OMEROLocation, OMEROSession> sessions = new HashMap<>();

	private final ThreadLocal<OMEROSession> activeSessions = new ThreadLocal<>();

	private Map<Class<?>, Collection<Class<?>>> convertTo;
	private Map<Class<?>, Collection<Class<?>>> convertFrom;

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
		if (Dataset.class.isAssignableFrom(type) || DatasetView.class
			.isAssignableFrom(type) || ImageDisplay.class.isAssignableFrom(type) ||
			(getFromConvert(Dataset.class).contains(type) && getToConvert(
				Dataset.class).contains(type)))
		{
			// use an image ID
			return omero.rtypes.rlong(0);
		}

		// table
		if (Table.class.isAssignableFrom(type) || TableDisplay.class
			.isAssignableFrom(type) || (getFromConvert(Table.class).contains(type) &&
				getToConvert(Table.class).contains(type)))
		{
			// table file ID
			return omero.rtypes.rlong(0);
		}

		// ROI
		if (TreeNode.class.isAssignableFrom(type) || (getFromConvert(TreeNode.class)
			.contains(type) && getToConvert(TreeNode.class).contains(type)))
			return omero.rtypes.rlist();

		if (MaskPredicate.class.isAssignableFrom(type) || (getFromConvert(
			MaskPredicate.class).contains(type) && getToConvert(MaskPredicate.class)
				.contains(type))) return omero.rtypes.rlong(0);

		// primitive types
		final Class<?> saneType = Types.box(type);
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
			final HashMap<String, omero.RType> val = new HashMap<>();
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
	public Object toOMERO(final omero.client client, final Object value)
		throws omero.ServerError, IOException, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		// -- Image cases --

		if (value instanceof Dataset) {
			// upload image to OMERO, returning the resultant image ID
			final long imageID = uploadImage(client, (Dataset) value);
			return toOMERO(client, imageID);
		}
		if (getToConvert(Dataset.class).contains(value.getClass())) return toOMERO(
			client, convertService.convert(value, Dataset.class));
		if (value instanceof DatasetView) {
			final DatasetView datasetView = (DatasetView) value;
			// TODO: Verify whether any view-specific metadata can be preserved.
			return toOMERO(client, datasetView.getData());
		}
		if (value instanceof ImageDisplay) {
			final ImageDisplay imageDisplay = (ImageDisplay) value;
			// TODO: Support more aspects of image displays; e.g., multiple datasets.
			return toOMERO(client, imageDisplayService.getActiveDataset(
				imageDisplay));
		}

		// -- Table cases --

		if (value instanceof Table) return convertOMEROTable((Table<?, ?>) value);
		if (value instanceof TableDisplay) return toOMERO(client,
			((TableDisplay) value).get(0));
		if (getToConvert(Table.class).contains(value.getClass())) return toOMERO(
			client, convertService.convert(value, Table.class));

		// -- ROI cases --

		if (value instanceof OMEROROICollection) return convertService.convert(
			value, ROIData.class);
		if ((value instanceof TreeNode && ((TreeNode<?>) value)
			.data() instanceof MaskPredicate))
		{
			final MaskPredicate<?> mp = (MaskPredicate<?>) ((TreeNode<?>) value)
				.data();
			if (mp instanceof Interval || mp instanceof RealInterval)
				return convertService.convert(value, ROIData.class);
			throw new IllegalArgumentException("MaskPredicate must be MaskInterval " +
				"or RealMaskRealInterval to be converted to ROIData");
		}
		if (value instanceof List && checkROIList((List<?>) value)) {
			final List<Object> l = new ArrayList<>(((List<?>) value).size());
			for (Object o : (List<?>) value)
				l.add(toOMERO(client, o));
			return l;
		}
		if (value instanceof MaskPredicate) return toOMERO(client,
			new DefaultTreeNode<>(value, null));
		if (value instanceof MaskPredicate) {
			final Object o = toOMERO(client, new DefaultTreeNode<>(value, null));
			return ((List<?>) o).get(0);
		}
		if (getToConvert(TreeNode.class).contains(value.getClass())) return toOMERO(
			client, convertService.convert(value, TreeNode.class));
		if (getToConvert(MaskPredicate.class).contains(value.getClass()))
			return toOMERO(client, convertService.convert(value,
				MaskPredicate.class));

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
				collection = new ArrayList<>();
			}
			else if (value instanceof omero.RSet) {
				collection = new HashSet<>();
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
			final Map<String, Object> map = new HashMap<>();
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
		catch (final URISyntaxException exc) {
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
		final String omeroSource = "omero:" + credentials(client) + "&imageID=" +
			imageID;

		return datasetIOService.open(omeroSource);
	}

	@Override
	public long uploadImage(final omero.client client, final Dataset dataset)
		throws omero.ServerError, IOException
	{
		// TODO: Reuse existing client instead of creating a new connection.
		// Will need to rethink how SCIFIO conveys source and destination metadata.
		// The RandomAccessInput/OutputStream design is probably too narrow.
		final String omeroDestination = "name=" + dataset.getName() + "&" +
			credentials(client) //
			+ ".omero"; // FIXME: Remove this after SCIFIO doesn't need it anymore.

		final Metadata metadata = datasetIOService.save(dataset, omeroDestination);

		if (metadata instanceof OMEROFormat.Metadata) {
			final OMEROFormat.Metadata omeroMeta = (OMEROFormat.Metadata) metadata;
			return omeroMeta.getImageID();
		}
		return -1;
	}

	@Override
	public long uploadTable(final OMEROLocation credentials, final String name,
		final Table<?, ?> imageJTable, final long imageID) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final TableData omeroTable = convertOMEROTable(imageJTable);
		long id = -1;
		final OMEROSession session = session(credentials);
		// Get image
		final BrowseFacility browseFacility = session.getGateway().getFacility(
			BrowseFacility.class);
		final ImageData image = browseFacility.getImage(session
			.getSecurityContext(), imageID);

		// attach table to image
		final TablesFacility tablesFacility = session.getGateway().getFacility(
			TablesFacility.class);
		final TableData stored = tablesFacility.addTable(session
			.getSecurityContext(), image, name, omeroTable);
		id = stored.getOriginalFileId();
		return id;
	}

	@Override
	public TableData convertOMEROTable(final Table<?, ?> imageJTable) {
		final TableDataColumn[] omeroColumns = new TableDataColumn[imageJTable
			.getColumnCount()];
		final Object[][] data = new Object[imageJTable.getColumnCount()][];

		for (int c = 0; c < imageJTable.getColumnCount(); c++) {
			omeroColumns[c] = TableUtils.createOMEROColumn(imageJTable.get(c), c);
			data[c] = TableUtils.populateOMEROColumn(imageJTable.get(c),
				convertService);
		}

		// Create table and attach to image
		final TableData omeroTable = new TableData(omeroColumns, data);
		omeroTable.setNumberOfRows(imageJTable.getRowCount());

		return omeroTable;
	}

	@Override
	public Table<?, ?> downloadTable(final OMEROLocation credentials,
		final long tableID) throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final OMEROSession session = session(credentials);
		final TablesFacility tableService = session.getGateway().getFacility(
			TablesFacility.class);
		final TableData table = tableService.getTable(session.getSecurityContext(),
			tableID, 0, Integer.MAX_VALUE - 1);

		final TableDataColumn[] omeroColumns = table.getColumns();
		final Object[][] data = table.getData();

		final Table<?, ?> imageJTable = TableUtils.createImageJTable(omeroColumns);
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
				((GenericTable) imageJTable).add(omeroColumns[i].getIndex(), imageJCol);
			}
			else {
				TableUtils.populateImageJColumn(omeroColumns[i].getType(),
					data[omeroColumns[i].getIndex()], imageJTable.get(i));
				imageJTable.get(i).setHeader(omeroColumns[i].getName());
			}
		}
		return imageJTable;
	}

	@Override
	public ROITree downloadROIs(final OMEROLocation credentials,
		final long imageID) throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final ROITree roiTree = new DefaultROITree();
		final OMEROSession session = session(credentials);
		final ROIFacility roifac = session.getGateway().getFacility(
			ROIFacility.class);
		final List<ROIResult> roiresults = roifac.loadROIs(session
			.getSecurityContext(), imageID);
		final Iterator<ROIResult> r = roiresults.iterator();
		while (r.hasNext()) {
			final ROIResult res = r.next();
			final Collection<ROIData> rois = res.getROIs();
			for (final ROIData roi : rois) {
				final TreeNode<?> ijRoi = convertService.convert(roi, TreeNode.class);
				if (ijRoi == null) throw new IllegalArgumentException(
					"ROIData cannot be converted to ImageJ ROI");
				roiTree.children().add(ijRoi);
			}
		}
		return roiTree;
	}

	@Override
	public ROITree downloadROI(final OMEROLocation credentials,
		final long roiID) throws DSOutOfServiceException, DSAccessException,
		ExecutionException
	{
		final OMEROSession session = session(credentials);
		final ROIFacility roifac = session.getGateway().getFacility(
			ROIFacility.class);
		final ROIResult roi = roifac.loadROI(session.getSecurityContext(), roiID);
		final ROIData rd = roi.getROIs().iterator().next();
		final TreeNode<?> treeNode = convertService.convert(rd, TreeNode.class);
		final ROITree tree = new DefaultROITree();
		tree.children().add(treeNode);
		return tree;
	}

	@Override
	public long[] uploadROIs(final OMEROLocation credentials,
		final TreeNode<?> ijROIs, final long imageID) throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final OMEROSession session = session(credentials);
		final ROIFacility roifac = session.getGateway().getFacility(
			ROIFacility.class);
		final Interval interval = null;

		final List<ROIData> omeroROIs = new ArrayList<>();
		final List<TreeNode<?>> roiTreeNodes = collectROITreeNodes(ijROIs);
		for (final TreeNode<?> dn : roiTreeNodes) {
			ROIData oR;
			if (!(dn.data() instanceof Interval) && !(dn
				.data() instanceof RealInterval) && dn.data() instanceof MaskPredicate)
				oR = convertService.convert(interval((MaskPredicate<?>) dn.data(),
					interval, imageID, session), ROIData.class);
			else oR = convertService.convert(dn, ROIData.class);
			if (oR == null) throw new IllegalArgumentException("Unsupported type: " +
				dn.data().getClass());
			omeroROIs.add(oR);
		}

		final Collection<ROIData> updatedOmeroROIs = roifac.saveROIs(session
			.getSecurityContext(), imageID, omeroROIs);

		final long[] ids = new long[updatedOmeroROIs.size()];
		int count = 0;
		for (final ROIData roi : updatedOmeroROIs) {
			ids[count] = roi.asIObject().getId().getValue();
			count++;
		}

		return ids;
	}

	@Override
	public OMEROSession session(final OMEROLocation location) {
		final OMEROSession session = sessions.computeIfAbsent(location,
			c2 -> createSession(c2));
		activeSessions.set(session);
		return session;
	}

	@Override
	public OMEROSession session() {
		return activeSessions.get();
	}

	@Override
	public OMEROSession createSession(final OMEROLocation location) {
		try {
			return new DefaultOMEROSession(location, this);
		}
		catch (ServerError | PermissionDeniedException
				| CannotCreateSessionException exc)
		{
			log.error("Cannot connect to OMERO server", exc);
		}
		return null;
	}

	@Override
	public void removeSession(final OMEROSession session) {
		if (session == null || !sessions.containsValue(session)) return;
		if (Objects.equals(activeSessions.get(), session)) activeSessions.set(null);
		for (final OMEROLocation l : sessions.keySet()) {
			if (Objects.equals(sessions.get(l), session)) {
				sessions.remove(l);
				return;
			}
		}
	}

	@Override
	public TagAnnotationI getAnnotation(final String description,
		final String value, final OMEROLocation location) throws ExecutionException,
		ServerError, DSOutOfServiceException, DSAccessException
	{
		final OMEROSession s = session(location);
		return getAnnotation(description, value, s);
	}

	@Override
	public TagAnnotationI getAnnotation(final String description,
		final String value) throws ExecutionException, ServerError,
		DSOutOfServiceException, DSAccessException
	{
		final OMEROSession s = activeSessions.get();
		if (s == null) throw new IllegalArgumentException(
			"Cannot get TagAnnotation, no session associated with running thread!");
		return getAnnotation(description, value, s);
	}

	// -- Helper methods --

	/**
	 * Generates an OMERO source string fragment with credentials matching the
	 * given client.
	 */
	private static String credentials(final omero.client client) {
		return "server=" + getHost(client) + //
			"&port=" + client.getProperty("omero.port") + //
			"&sessionID=" + client.getSessionId();
	}

	private OMEROLocation createCredentials(final omero.client client)
		throws NumberFormatException, URISyntaxException
	{
		return new OMEROLocation(getHost(client), Integer.parseInt(client
			.getProperty("omero.port")), client.getProperty("omero.user"), client
				.getProperty("omero.pass"));
	}

	private static String getHost(final omero.client client) {
		String host = client.getProperty("omero.host");
		if (host == null || host.isEmpty()) {
			final String router = client.getProperty("Ice.Default.Router");
			final int index = router.indexOf("-h ");
			if (index == -1) throw new IllegalArgumentException("hostname required");
			host = router.substring(index + 3, router.length());
		}
		return host;
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
	 * @throws URISyntaxException
	 * @throws NumberFormatException
	 */
	private <T> T convert(final omero.client client, final Object value,
		final Class<T> type) throws omero.ServerError, IOException,
		PermissionDeniedException, CannotCreateSessionException,
		DSOutOfServiceException, ExecutionException, DSAccessException,
		NumberFormatException, URISyntaxException
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
		if (Types.isNumber(value.getClass())) {
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
				final OMEROLocation credentials = createCredentials(client);
				@SuppressWarnings("unchecked")
				final T table = (T) downloadTable(credentials, tableID);
				return table;
			}
			if (TreeNode.class.isAssignableFrom(type)) {
				final long roiID = ((Number) value).longValue();
				final OMEROLocation credentials = createCredentials(client);
				@SuppressWarnings("unchecked")
				final T TreeNode = (T) downloadROI(credentials, roiID);
				return TreeNode;
			}
			if (MaskPredicate.class.isAssignableFrom(type)) {
				final long roiID = ((Number) value).longValue();
				final OMEROLocation credentials = createCredentials(client);
				final TreeNode<?> TreeNode = downloadROI(credentials, roiID);
				final List<TreeNode<?>> children = TreeNode.children();
				@SuppressWarnings("unchecked")
				final T omeroMP = (T) children.get(0).data();
				if (children.size() > 1) log.warn("Requested OMERO ROI has more than " +
					"one ShapeData. Only one shape will be returned.");
				return omeroMP;
			}
			if (getFromConvert(Dataset.class).contains(type)) {
				final Dataset d = convert(client, value, Dataset.class);
				return convertService.convert(d, type);
			}
			if (getFromConvert(TreeNode.class).contains(type)) {
				final TreeNode<?> dn = convert(client, value, TreeNode.class);
				return convertService.convert(dn, type);
			}
			if (getFromConvert(MaskPredicate.class).contains(type)) {
				final MaskPredicate<?> mp = convert(client, value, MaskPredicate.class);
				return convertService.convert(mp, type);
			}
			if (getFromConvert(Table.class).contains(type)) {
				final Table<?, ?> t = convert(client, value, Table.class);
				return convertService.convert(t, type);
			}
		}

		// use SciJava Common's automagical conversion routine
		final T converted = convertService.convert(value, type);
		if (converted == null) {
			log.error("Cannot convert: " + value.getClass().getName() + " to " + type
				.getName());
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

	/**
	 * Creates a {@link TagAnnotationData} with the given description and
	 * textValue, and saves it to the server using the session credentials.
	 *
	 * @param description the description for the {@link TagAnnotationData}, for
	 *          imagej tags this should start with "imagej:"
	 * @param value the text value for the {@link TagAnnotationData}
	 * @param s the session credentials to use and create the
	 *          {@link TagAnnotationData} for
	 * @return newly created {@link TagAnnotationData}
	 * @throws ExecutionException
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	private TagAnnotationData createTag(final String description,
		final String value, final OMEROSession s) throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final DataManagerFacility dm = s.getGateway().getFacility(
			DataManagerFacility.class);
		TagAnnotationData t = new TagAnnotationData(value);
		t.setDescription(description);
		t = (TagAnnotationData) dm.saveAndReturnObject(s.getSecurityContext(), t);
		return t;
	}

	/**
	 * Attempts to retrieve the {@link TagAnnotationI} with the given description
	 * and text value from the server. If no such {@link TagAnnotationI} exists
	 * for the given session/credentials, a new one is created and saved on the
	 * server.
	 *
	 * @param description the description for the tag of interest
	 * @param value the text value of the tag of interest
	 * @param s the session credentials to use when querying the server
	 * @return the found tag, or a new tag if no matching tag previously existed
	 * @throws ExecutionException
	 * @throws ServerError
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	private TagAnnotationI getAnnotation(final String description,
		final String value, final OMEROSession s) throws ExecutionException,
		ServerError, DSOutOfServiceException, DSAccessException
	{
		final IQueryPrx query = s.getGateway().getQueryService(s
			.getSecurityContext());
		final List<omero.model.IObject> tags = query.findAllByString(
			"TagAnnotationI", "description", description, true, new Filter(
				omero.rtypes.rbool(false), null, null, null, null, null, null));

		// If query returns nothing, make the tag
		if (tags == null) return (TagAnnotationI) createTag(description, value, s)
			.asIObject();

		for (final omero.model.IObject tag : tags) {
			if (tag instanceof TagAnnotationI && ((TagAnnotationI) tag).getTextValue()
				.getValue().equals(value)) return (TagAnnotationI) tag;
		}

		// if no matching tags, create tag
		return (TagAnnotationI) createTag(description, value, s).asIObject();
	}

	/**
	 * Puts interval bounds on an unbounded {@link MaskPredicate}. The bounds will
	 * correspond to the size of the image. If the {@link MaskPredicate} is a
	 * {@link RealMask}, it is also rasterized.
	 *
	 * @param m an unbounded {@link MaskPredicate}
	 * @param interval the interval to apply, if null it is computed
	 * @param imageID the ID of the OMERO image whose interval should be applied
	 * @param session the current session
	 * @return a TreeNode whose data is a RandomAccessibleInterval representation
	 *         of the original data
	 * @throws ExecutionException
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	private TreeNode<RandomAccessibleInterval<BoolType>> interval(
		final MaskPredicate<?> m, Interval interval, final long imageID,
		final OMEROSession session) throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		if (interval == null) interval = getImageInterval(session, imageID);

		RandomAccessibleInterval<BoolType> rai;
		if (m instanceof Mask) rai = Views.interval(Masks.toRandomAccessible(
			(Mask) m), interval);
		else rai = Views.interval(Views.raster(Masks.toRealRandomAccessible(
			(RealMask) m)), interval);

		return new DefaultTreeNode<>(rai, null);
	}

	/**
	 * Retrieve the {@link ImageData} from the OMERO server, and compute its
	 * {@link Interval}.
	 *
	 * @param session current session
	 * @param imageID ID of {@link ImageData} whose bounds should be computed
	 * @return the computed {@link Interval}
	 * @throws ExecutionException
	 * @throws DSOutOfServiceException
	 * @throws DSAccessException
	 */
	private Interval getImageInterval(final OMEROSession session,
		final long imageID) throws ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final BrowseFacility browse = session.getGateway().getFacility(
			BrowseFacility.class);
		final ImageData image = browse.getImage(session.getSecurityContext(),
			imageID);
		return new FinalInterval(new long[] { 0, 0 }, new long[] { image
			.getDefaultPixels().getSizeX(), image.getDefaultPixels().getSizeY() });
	}

	/**
	 * Collects all {@link TreeNode}s in the given "tree" whose data is a Roi.
	 *
	 * @param dn TreeNode whose data and children are check for ROIs
	 * @return a list of TreeNodes whose data is a ROI type.
	 */
	private List<TreeNode<?>> collectROITreeNodes(final TreeNode<?> dn) {
		if (dn == null) return Collections.emptyList();

		if (dn.children() == null || dn.children().isEmpty()) {
			if (dn.data() instanceof MaskPredicate) return Collections
				.singletonList(dn);
			return Collections.emptyList();
		}
		if (dn.data() instanceof ROIData) return Collections.singletonList(dn);

		final List<TreeNode<?>> rois = new ArrayList<>();
		for (final TreeNode<?> child : dn.children()) {
			if (child.data() instanceof ROIData) rois.add(child);
			else if ((child.children() == null || child.children().isEmpty()) && child
				.data() instanceof MaskPredicate) rois.add(child);
			else collectROITreeNodes(child, rois);
		}

		return rois;
	}

	private void collectROITreeNodes(final TreeNode<?> dn,
		final List<TreeNode<?>> rois)
	{
		if (dn.children() == null || dn.children().isEmpty()) return;

		for (final TreeNode<?> child : dn.children()) {
			if (child.data() instanceof ROIData) rois.add(child);
			else if (child.children() == null && child
				.data() instanceof MaskPredicate) rois.add(child);
			else collectROITreeNodes(child, rois);
		}
	}

	private Collection<Class<?>> getToConvert(final Class<?> convertToClass) {
		if (convertTo == null) initConvertTo();
		return convertTo.get(convertToClass);
	}

	private Collection<Class<?>> getFromConvert(final Class<?> convertFromClass) {
		if (convertFrom == null) initConvertFrom();
		return convertFrom.get(convertFromClass);
	}

	private synchronized void initConvertTo() {
		if (convertTo != null) return;
		final Map<Class<?>, Collection<Class<?>>> map = new HashMap<>();
		map.put(Dataset.class, Collections.unmodifiableCollection(convertService
			.getCompatibleInputClasses(Dataset.class)));
		map.put(TreeNode.class, Collections.unmodifiableCollection(convertService
			.getCompatibleInputClasses(TreeNode.class)));
		map.put(MaskPredicate.class, Collections.unmodifiableCollection(
			convertService.getCompatibleInputClasses(MaskPredicate.class)));
		map.put(Table.class, Collections.unmodifiableCollection(convertService
			.getCompatibleInputClasses(Table.class)));
		convertTo = Collections.unmodifiableMap(map);
	}

	private synchronized void initConvertFrom() {
		if (convertFrom != null) return;
		final Map<Class<?>, Collection<Class<?>>> map = new HashMap<>();
		map.put(Dataset.class, Collections.unmodifiableCollection(convertService
			.getCompatibleOutputClasses(Dataset.class)));
		map.put(TreeNode.class, Collections.unmodifiableCollection(convertService
			.getCompatibleOutputClasses(TreeNode.class)));
		map.put(MaskPredicate.class, Collections.unmodifiableCollection(
			convertService.getCompatibleOutputClasses(MaskPredicate.class)));
		map.put(Table.class, Collections.unmodifiableCollection(convertService
			.getCompatibleOutputClasses(Table.class)));
		convertFrom = Collections.unmodifiableMap(map);
	}

	/**
	 * Check if the given list contains only {@link TreeNode}s which can be
	 * converted to {@link ROIData}
	 *
	 * @param rois the {@code List} to check
	 * @return {@code true} if all components can be converted to {@link ROIData},
	 *         {@code false} otherwise
	 */
	private boolean checkROIList(final List<?> rois) {
		if (rois.isEmpty()) return false;
		for (Object o : rois) {
			if (o instanceof OMEROROICollection) continue;
			else if (o instanceof TreeNode && ((TreeNode<?>) o)
				.data() instanceof MaskPredicate) continue;
			else if (o instanceof MaskPredicate) continue;
			else return false;
		}
		return true;
	}

}
