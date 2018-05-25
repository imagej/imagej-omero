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
import java.util.IdentityHashMap;
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
import net.imagej.omero.roi.ROIConverters;
import net.imagej.roi.DefaultROITree;
import net.imagej.roi.ROITree;
import net.imagej.table.BoolTable;
import net.imagej.table.ByteTable;
import net.imagej.table.Column;
import net.imagej.table.FloatTable;
import net.imagej.table.GenericTable;
import net.imagej.table.IntTable;
import net.imagej.table.LongTable;
import net.imagej.table.ResultsTable;
import net.imagej.table.ShortTable;
import net.imagej.table.Table;
import net.imagej.table.TableDisplay;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.roi.Mask;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.Line;
import net.imglib2.roi.geom.real.PointMask;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.Polyline;
import net.imglib2.roi.geom.real.RealPointCollection;
import net.imglib2.roi.geom.real.Sphere;
import net.imglib2.roi.geom.real.WritableBox;
import net.imglib2.roi.geom.real.WritableEllipsoid;
import net.imglib2.roi.geom.real.WritableLine;
import net.imglib2.roi.geom.real.WritablePointMask;
import net.imglib2.roi.geom.real.WritablePolygon2D;
import net.imglib2.roi.geom.real.WritablePolyline;
import net.imglib2.roi.geom.real.WritableRealPointCollection;
import net.imglib2.roi.geom.real.WritableSphere;
import net.imglib2.type.logic.BoolType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
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
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.DataObject;
import omero.gateway.model.DatasetData;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ROIResult;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.model.DatasetI;
import omero.model.ImageI;

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

	private final Map<Object, ROIData> savedRois =
		new IdentityHashMap<>();
	private final Map<Long, ROIData> downloadedROIs = new HashMap<>();

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
	@SuppressWarnings("deprecation")
	public omero.RType prototype(final Class<?> type) {
		// image types
		if (Dataset.class.isAssignableFrom(type) || DatasetView.class
			.isAssignableFrom(type) || ImageDisplay.class.isAssignableFrom(type) ||
			(convertService.supports(type, Dataset.class) && convertService.supports(
				Dataset.class, type)))
		{
			// use an image ID
			return omero.rtypes.rlong(0);
		}

		// table
		if (Table.class.isAssignableFrom(type) || TableDisplay.class
			.isAssignableFrom(type) || (convertService.supports(Table.class, type) &&
				convertService.supports(type, Table.class)))
		{
			// table file ID
			return omero.rtypes.rlong(0);
		}

		// ROI
		// When requesting a TreeNode it is assumed that you want all the number
		// provided is an image ID and you want all the ROIs associated with
		// that image
		if (TreeNode.class.isAssignableFrom(type) || (convertService.supports(
			TreeNode.class, type) && convertService.supports(type, TreeNode.class)))
			return omero.rtypes.rlong(0);

		if (MaskPredicate.class.isAssignableFrom(type) || (convertService.supports(
			MaskPredicate.class, type) && convertService.supports(type,
				MaskPredicate.class))) return omero.rtypes.rlong(0);

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
		DSAccessException, NumberFormatException, URISyntaxException
	{
		if (value == null) return null;

		// -- Image cases --

		if (value instanceof Dataset) {
			// upload image to OMERO, returning the resultant image ID
			final Dataset d = (Dataset) value;
			final long imageID = uploadImage(client, d);

			// upload any attached ROIs
			// TODO: upload or update?
			if (d.getProperties().get("rois") != null) uploadROIs(createCredentials(
				client), (TreeNode<?>) d.getProperties().get("rois"), imageID);

			// upload any attached tables
			// TODO: Modify tables to implement Named??
			if (d.getProperties().get("tables") != null) {
				@SuppressWarnings("unchecked")
				final List<Table<?, ?>> tables = (List<Table<?, ?>>) d.getProperties()
					.get("tables");
				final OMEROLocation cred = createCredentials(client);
				for (final Table<?, ?> table : tables)
					uploadTable(cred, d.getName() + "-table", table, imageID);
			}

			return toOMERO(client, imageID);
		}
		if (convertService.supports(value, Dataset.class)) return toOMERO(
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
		if (convertService.supports(value, Table.class)) return toOMERO(
			client, convertService.convert(value, Table.class));

		// -- ROI cases --

		if (value instanceof TreeNode) {
			return convertOMEROROI((TreeNode<?>) value, null);
		}
		if (value instanceof MaskPredicate) {
			final Object o = toOMERO(client, new DefaultTreeNode<>(value, null));
			return ((List<?>) o).get(0);
		}
		if (convertService.supports(value, TreeNode.class)) return toOMERO(client,
			convertService.convert(value, TreeNode.class));
		if (convertService.supports(value, MaskPredicate.class)) return toOMERO(
			client, convertService.convert(value, MaskPredicate.class));

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
	public void uploadImage(final OMEROLocation credentials, final Dataset image,
		final boolean uploadROIs, final TreeNode<?> rois, final boolean updateROIs,
		final boolean uploadTables, final List<Table<?, ?>> tables,
		final String[] tableNames, final long omeroDatasetID) throws ServerError,
		IOException, ExecutionException, DSOutOfServiceException, DSAccessException,
		PermissionDeniedException, CannotCreateSessionException
	{
		if (image == null) throw new IllegalArgumentException(
			"Image cannot be null!");

		final OMEROSession session = session(credentials);
		long omeroImageID = -1;
		final DataManagerFacility dm = session.getGateway().getFacility(
			DataManagerFacility.class);

		// Upload Image
		omeroImageID = uploadImage(session.getClient(), image);

		// Upload/update attachments
		uploadImageAttachments(credentials, omeroImageID, uploadROIs, updateROIs,
			uploadTables, rois, tables, tableNames);

		// Attach image to Dataset
		if (omeroDatasetID > 0) {
			final ImageData imgData = new ImageData(new ImageI(omeroImageID, false));
			final DatasetData dsData = new DatasetData(new DatasetI(omeroDatasetID,
				false));
			dm.addImageToDataset(session.getSecurityContext(), imgData, dsData);
		}
	}

	@Override
	public void uploadImageAttachments(final OMEROLocation credentials,
		final long imageID, final boolean uploadROIs, final boolean updateROIs,
		final boolean uploadTables, final TreeNode<?> rois,
		final List<Table<?, ?>> tables, final String[] tableNames)
		throws ExecutionException, DSOutOfServiceException, DSAccessException,
		ServerError, PermissionDeniedException, CannotCreateSessionException
	{
		if ((updateROIs || uploadROIs) && rois != null) {
			if (updateROIs) updateAndReturnROIs(credentials, rois, imageID);
			else uploadROIs(credentials, rois, imageID);
		}

		if (uploadTables && tables != null && !tables.isEmpty()) {
			for (int i = 0; i < tables.size(); i++) {
				uploadTable(credentials, tableNames[i], tables.get(i), imageID);
			}
		}
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
	public List<Table<?, ?>> downloadTables(final OMEROLocation credentials,
		final long imageID) throws ExecutionException, DSOutOfServiceException,
		DSAccessException, ServerError, PermissionDeniedException,
		CannotCreateSessionException
	{
		final OMEROSession session = session(credentials);
		final TablesFacility tableService = session.getGateway().getFacility(
			TablesFacility.class);

		final Collection<FileAnnotationData> files = tableService
			.getAvailableTables(session.getSecurityContext(), new ImageData(
				new ImageI(imageID, false)));

		final List<Table<?, ?>> tables = new ArrayList<>(files.size());
		for (final FileAnnotationData file : files)
			tables.add(downloadTable(credentials, file.getFileID()));

		return tables;
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

		if (roifac.getROICount(session.getSecurityContext(), imageID) == 0)
			return roiTree;

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
		final Collection<ROIData> uploadedROIs = uploadAndReturnROIs(credentials,
			ijROIs, imageID);
		return getROIIds(uploadedROIs);
	}

	@Override
	public long[] updateROIs(final OMEROLocation credentials,
		final TreeNode<?> ijROIs, final long imageID) throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final Collection<ROIData> uploadedROIs = updateAndReturnROIs(credentials,
			ijROIs, imageID);
		return getROIIds(uploadedROIs);
	}

	@Override
	public Collection<ROIData> uploadAndReturnROIs(
		final OMEROLocation credentials, final TreeNode<?> ijROIs,
		final long imageID) throws ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final OMEROSession session = session(credentials);
		final Interval interval = getImageInterval(session, imageID);
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs = split(
			ijROIs);
		final List<ROIData> savedOMERORois = new ArrayList<>();
		final ROIFacility roifac = session.getGateway().getFacility(
			ROIFacility.class);

		// FIXME: This is a lot of server calls

		// Handle ROIs which originated in ImageJ
		for (final TreeNode<?> ijROI : splitROIs.getB()) {
			final List<ROIData> roiData = convertOMEROROI(ijROI, interval);
			clearROIs(roiData);
			final Collection<ROIData> saved = roifac.saveROIs(session
				.getSecurityContext(), imageID, roiData);
			addROIMapping(ijROI.data(), saved.iterator().next());
			savedOMERORois.add(saved.iterator().next());
		}

		// Handle ROIs which originated in OMERO
		for (final OMEROROICollection orc : splitROIs.getA()) {
			final List<ROIData> roiData = convertOMEROROI(orc, interval);
			if (downloadedROIs.containsKey(roiData.get(0).getId())) downloadedROIs
				.remove(roiData.get(0).getId());
			clearROIs(roiData);
			final Collection<ROIData> saved = roifac.saveROIs(session
				.getSecurityContext(), imageID, roiData);
			final ROIData savedRoi = saved.iterator().next();

			// NB: If updated later, the id will match correctly
			updateROIData(orc, savedRoi);
			downloadedROIs.put(savedRoi.getId(), savedRoi);

			savedOMERORois.add(savedRoi);
		}

		return savedOMERORois;
	}

	@Override
	public Collection<ROIData> updateAndReturnROIs(
		final OMEROLocation credentials, final TreeNode<?> ijROIs,
		final long imageID) throws ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final OMEROSession session = session(credentials);
		final Interval interval = getImageInterval(session, imageID);
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs = split(
			ijROIs);
		final List<ROIData> newROIs = new ArrayList<>();
		final List<Long> ids = new ArrayList<>();
		final ROIFacility roifac = session.getGateway().getFacility(ROIFacility.class);
		final DataManagerFacility dm = session.getGateway().getFacility(
			DataManagerFacility.class);

		// Handle ROIs which originated in OMERO
		for (final OMEROROICollection orc : splitROIs.getA()) {
			ROIData converted = convertOMEROROI(orc, interval).get(0);
			if (downloadedROIs.containsKey(converted.getId())) converted =
				downloadedROIs.get(converted.getId());
			final DataObject savedOMERO = dm.saveAndReturnObject(session
				.getSecurityContext(), converted);
			if (!(savedOMERO instanceof ROIData)) throw new IllegalArgumentException(
				"ROI was not returned by OMERO");
			final ROIData savedROI = (ROIData) savedOMERO;
			downloadedROIs.put(savedROI.getId(), savedROI);
			updateROIData(orc, savedROI);
			ids.add(savedROI.getId());
		}

		// Handle ROIs which originated in ImageJ
		for (final TreeNode<?> dn : splitROIs.getB()) {
			final List<ROIData> converted = convertOMEROROI(dn, interval);
			final Collection<ROIData> saved = roifac.saveROIs(session
				.getSecurityContext(), imageID, converted);
			if (getROIMapping(dn.data()) == null) newROIs.add(saved.iterator()
				.next());
			addROIMapping(dn.data(), saved.iterator().next());
			ids.add(saved.iterator().next().getId());
		}

		// Check if any ROIs must be deleted
		final Collection<ROIResult> roisOnServer = roifac.loadROIs(session
			.getSecurityContext(), imageID);
		for (final ROIResult result : roisOnServer) {
			for (final ROIData roi : result.getROIs()) {
				if (!ids.contains(roi.getId())) {
					dm.delete(session.getSecurityContext(), roi.asIObject());

					// check if deleted ROI was mapped, if so remove mapping
					if (downloadedROIs.containsKey(roi.getId())) downloadedROIs.remove(roi
						.getId());
					for (final Object key : savedRois.keySet()) {
						if (savedRois.get(key).getId() == roi.getId()) savedRois.remove(
							key);
					}
				}
			}
		}

		return newROIs;
	}

	@Override
	public List<ROIData> convertOMEROROI(final TreeNode<?> dataNodeRois,
		final Interval interval)
	{
		final List<ROIData> omeroROIs = new ArrayList<>();
		final List<TreeNode<?>> roiTreeNodes = collectROITreeNodes(dataNodeRois);

		for (final TreeNode<?> dn : roiTreeNodes) {
			ROIData oR;
			// If the data node has unbounded mask predicate data, apply the given
			// interval if non-null
			if (!(dn.data() instanceof Interval) && !(dn
				.data() instanceof RealInterval) && dn
					.data() instanceof MaskPredicate && interval != null) oR =
						convertService.convert(interval((MaskPredicate<?>) dn.data(),
							interval), ROIData.class);
			// else convert directly
			else oR = convertService.convert(dn, ROIData.class);
			if (oR == null) throw new IllegalArgumentException("Unsupported type: " +
				dn.data().getClass());
			omeroROIs.add(oR);
		}
		return omeroROIs;
	}

	@Override
	public ROIData getUpdatedServerROIData(final long roiDataId) {
		return downloadedROIs.get(roiDataId);
	}

	@Override
	public void addROIMapping(final Object roi, final ROIData shape) {
		savedRois.put(roi, shape);
	}

	@Override
	public ROIData getROIMapping(final Object key) {
		return savedRois.get(key);
	}

	@Override
	public Set<Object> getROIMappingKeys() {
		return Collections.unmodifiableSet(savedRois.keySet());
	}

	@Override
	public void removeROIMapping(final Object key) {
		savedRois.remove(key);
	}

	@Override
	public void clearROIMappings() {
		savedRois.clear();
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
	 * of such types back to ImageJ's expected type fo for (final TreeNode<?> dn :
	 * ijROIs.children()) { ROIData oR; if (!(dn.data() instanceof Interval) &&
	 * !(dn .data() instanceof RealInterval) && dn .data() instanceof
	 * MaskPredicate) oR = convertService.convert( interval((MaskPredicate<?>)
	 * dn.data(), interval, imageID, session), ROIData.class); else oR =
	 * convertService.convert(dn, ROIData.class); if (oR == null) throw new
	 * IllegalArgumentException("Unsupported type: " + dn.data().getClass());
	 * omeroROIs.add(oR); } r the parameter.</li>
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
	@SuppressWarnings("deprecation")
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
				final long imageID = ((Number) value).longValue();
				final OMEROLocation credentials = createCredentials(client);
				@SuppressWarnings("unchecked")
				final T TreeNode = (T) downloadROIs(credentials, imageID);
				return TreeNode;
			}
			if (MaskPredicate.class.isAssignableFrom(type)) {
				final long roiID = ((Number) value).longValue();
				final OMEROLocation credentials = createCredentials(client);
				final TreeNode<?> TreeNode = downloadROI(credentials, roiID);
				final List<TreeNode<?>> children = TreeNode.children();
				@SuppressWarnings("unchecked")
				final T omeroMP = (T) children.get(0).children().get(0).data();
				if (children.size() > 1) log.warn("Requested OMERO ROI has more than " +
					"one ShapeData. Only one shape will be returned.");
				return omeroMP;
			}
			if (convertService.supports(Dataset.class, type)) {
				final Dataset d = convert(client, value, Dataset.class);
				return convertService.convert(d, type);
			}
			if (convertService.supports(TreeNode.class, type)) {
				final TreeNode<?> dn = convert(client, value, TreeNode.class);
				return convertService.convert(dn, type);
			}
			if (convertService.supports(MaskPredicate.class, type)) {
				final MaskPredicate<?> mp = convert(client, value, MaskPredicate.class);
				return convertService.convert(mp, type);
			}
			if (convertService.supports(Table.class, type)) {
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
	 * Puts interval bounds on an unbounded {@link MaskPredicate}. The bounds will
	 * correspond to the size of the image. If the {@link MaskPredicate} is a
	 * {@link RealMask}, it is also rasterized.
	 *
	 * @param m an unbounded {@link MaskPredicate}
	 * @param interval the interval to apply
	 * @return a TreeNode whose data is a RandomAccessibleInterval representation
	 *         of the original data
	 */
	private TreeNode<RandomAccessibleInterval<BoolType>> interval(
		final MaskPredicate<?> m, final Interval interval)
	{
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

	private long[] getROIIds(final Collection<ROIData> rois) {
		final long[] ids = new long[rois.size()];
		final Iterator<ROIData> itr = rois.iterator();
		for (int i = 0; i < ids.length; i++)
			ids[i] = itr.next().getId();
		return ids;
	}

	/**
	 * Split the ROIs within the given {@link TreeNode} into a {@link Pair} of
	 * ROIs which originated from OMERO and ROIs which originated from ImageJ.
	 *
	 * @param dn ROIs to separate
	 * @return a {@link Pair} of list of roi {@link TreeNode}s
	 */
	private Pair<List<OMEROROICollection>, List<TreeNode<?>>> split(
		final TreeNode<?> dn)
	{
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs =
			new ValuePair<>(new ArrayList<>(), new ArrayList<>());

		if (dn instanceof OMEROROICollection) {
			splitROIs.getA().add((OMEROROICollection) dn);
			return splitROIs;
		}
		if (dn.data() instanceof MaskPredicate) splitROIs.getB().add(dn);
		if (dn.children() == null || dn.children().isEmpty()) return splitROIs;

		for (final TreeNode<?> child : dn.children())
			split(child, splitROIs);

		return splitROIs;
	}

	/**
	 * Split the ROIs within the given {@link TreeNode} into a {@link Pair} of
	 * ROIs which originated from OMERO and ROIs which originated from ImageJ.
	 *
	 * @param dn ROIs to separate
	 */
	private void split(final TreeNode<?> dn,
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs)
	{
		if (dn instanceof OMEROROICollection) {
			splitROIs.getA().add((OMEROROICollection) dn);
			return;
		}
		if (dn.data() instanceof MaskPredicate) splitROIs.getB().add(dn);
		if (dn.children() == null || dn.children().isEmpty()) return;
		for (final TreeNode<?> child : dn.children())
			split(child, splitROIs);
	}

	/**
	 * Clears ids of the given {@link ROIData} objects, to ensure they are
	 * uploaded to the server as a new object.
	 *
	 * @param rois {@link ROIData} objects whose ids must be cleared
	 */
	private void clearROIs(final List<ROIData> rois) {
		for (final ROIData roi : rois) {
			roi.asIObject().setId(null);
			final Iterator<List<ShapeData>> itr = roi.getIterator();
			while (itr.hasNext()) {
				for (final ShapeData shape : itr.next())
					shape.asIObject().setId(null);
			}
		}
	}

	/**
	 * Sets the backing {@link ROIData} and {@link ShapeData} objects to have the
	 * same IDs as the newly saved version on the server.
	 *
	 * @param orc {@link OMEROROICollection} whose backing ROIData will be updated
	 * @param saved recently saved {@link ROIData} whose IDs will be copied
	 */
	private void updateROIData(final OMEROROICollection orc,
		final ROIData saved)
	{
		final ROIData rd = orc.data();
		rd.setId(saved.getId());

		// Shapes may not be in the same order
		final List<ShapeData> shapes = new ArrayList<>();
		final List<ShapeData> savedShapes = new ArrayList<>();
		final Iterator<List<ShapeData>> itr = rd.getIterator();
		final Iterator<List<ShapeData>> savedItr = saved.getIterator();
		while (itr.hasNext())
			shapes.addAll(itr.next());
		while (savedItr.hasNext())
			savedShapes.addAll(savedItr.next());

		for (final ShapeData shape : shapes) {
			ShapeData match = null;
			for (final ShapeData savedShape : savedShapes) {
				if (ROIConverters.shapeDataEquals(shape, savedShape)) {
					match = savedShape;
					shape.setId(savedShape.getId());
					break;
				}
			}
			if (match == null) throw new IllegalArgumentException(
				"Uploaded ROIData is missing a shape!");
			savedShapes.remove(match);
		}
	}

}
