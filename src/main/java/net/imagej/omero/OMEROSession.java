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

import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.util.FormatTools;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.omero.roi.OMEROROICollection;
import net.imagej.omero.roi.ROICache;
import net.imagej.omero.roi.ROIUtils;
import net.imagej.omero.table.TableUtils;
import net.imagej.roi.DefaultROITree;
import net.imagej.roi.ROITree;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RealInterval;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.util.Pair;

import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.table.Column;
import org.scijava.table.GenericTable;
import org.scijava.table.Table;
import org.scijava.table.TableDisplay;
import org.scijava.util.DefaultTreeNode;
import org.scijava.util.TreeNode;
import org.scijava.util.Types;

import omero.RLong;
import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.Facility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.DataObject;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ROIResult;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.log.SimpleLogger;
import omero.model.DatasetI;
import omero.model.IObject;
import omero.model.Image;
import omero.model.ImageI;
import omero.model.Pixels;
import omero.model.PixelsType;

/**
 * An OMERO session, the central access point for OMERO functions.
 *
 * @author Curtis Rueden
 */
public class OMEROSession /*extends AbstractContextual*/ implements Closeable {

	private final OMEROService omeroService;

	private final OMEROServer server;
	private final ROICache roiCache;

	private omero.client client;
	private ServiceFactoryPrx sfp;
	private ExperimenterData experimenter;
	private Gateway gateway;
	private SecurityContext ctx;
	private String sessionID;

	// -- Constructors --

	/**
	 * Constructs an OMERO session with server specified in the environment.
	 * Typically used to execute OMERO scripts on the server side, not to
	 * communicate with OMERO from an ImageJ2 application.
	 *
	 * @param omeroService The SciJava context.
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public OMEROSession(final OMEROService omeroService) throws OMEROException {
		// Create client in a vacuum. Works if running on the server side.
		this(omeroService, new omero.client());
	}

	public OMEROSession(final OMEROService omeroService, final OMEROServer server,
		final OMEROCredentials credentials) throws OMEROException
	{
		this(omeroService, server, credentials, //
			new omero.client(server.host, server.port));
	}

	private OMEROSession(final OMEROService omeroService, final omero.client c)
		throws OMEROException
	{
		this(omeroService, new OMEROServer(OMERO.host(c), OMERO.port(c)), null, c);
	}

	private OMEROSession(final OMEROService omeroService,
		final OMEROServer omeroServer, final OMEROCredentials omeroCredentials,
		final omero.client omeroClient) throws OMEROException
	{
		omeroCredentials.validate();

		this.omeroService = omeroService;
		this.server = omeroServer;
		roiCache = new ROICache();

		initializeSession(omeroCredentials, omeroClient);
	}

	// -- Data transfer --

	/**
	 * Converts an ImageJ parameter value to an OMERO parameter value.
	 * <p>
	 * If the given object is an image type (i.e., {@link Dataset},
	 * {@link DatasetView} or {@link ImageDisplay}) then the {@link #uploadImage}
	 * method will be used transparently to convert the object into an OMERO image
	 * ID.
	 * </p>
	 * <p>
	 * In the case of {@link Table}s, it will be converted to a {@link TableData}.
	 * </p>
	 *
	 * @throws IllegalArgumentException if the conversion is not supported.
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public Object toOMERO(final Object value) throws OMEROException {
		if (value == null) return null;

		// -- Image cases --

		if (value instanceof Dataset) {
			// upload image to OMERO, returning the resultant image ID
			final Dataset d = (Dataset) value;
			final long imageID = uploadImage(d);

			// upload any attached ROIs
			// TODO: upload or update?
			if (d.getProperties().get("rois") != null) {
				uploadROIs((TreeNode<?>) d.getProperties().get("rois"), imageID);
			}

			// upload any attached tables
			// TODO: Modify tables to implement Named??
			if (d.getProperties().get("tables") != null) {
				@SuppressWarnings("unchecked")
				final List<Table<?, ?>> tables = (List<Table<?, ?>>) d.getProperties()
					.get("tables");
				for (final Table<?, ?> table : tables)
					uploadTable(d.getName() + "-table", table, imageID);
			}

			return toOMERO(imageID);
		}
		if (omeroService.convert().supports(value, Dataset.class)) {
			return toOMERO(omeroService.convert().convert(value, Dataset.class));
		}
		if (value instanceof DatasetView) {
			final DatasetView datasetView = (DatasetView) value;
			// TODO: Verify whether any view-specific metadata can be preserved.
			return toOMERO(datasetView.getData());
		}
		if (value instanceof ImageDisplay) {
			final ImageDisplay imageDisplay = (ImageDisplay) value;
			// TODO: Support more aspects of image displays; e.g., multiple datasets.
			return toOMERO(omeroService.imageDisplay().getActiveDataset(
				imageDisplay));
		}

		// -- Table cases --

		if (value instanceof Table) {
			return TableUtils.convertOMEROTable((Table<?, ?>) value, omeroService
				.convert());
		}
		if (value instanceof TableDisplay) {
			return toOMERO(((TableDisplay) value).get(0));
		}
		if (omeroService.convert().supports(value, Table.class)) {
			return toOMERO(omeroService.convert().convert(value, Table.class));
		}

		// -- ROI cases --

		if (value instanceof TreeNode) {
			return convertOMEROROI((TreeNode<?>) value, null);
		}
		if (value instanceof MaskPredicate) {
			final Object o = toOMERO(new DefaultTreeNode<>(value, null));
			return ((List<?>) o).get(0);
		}
		if (omeroService.convert().supports(value, TreeNode.class)) {
			return toOMERO(omeroService.convert().convert(value, TreeNode.class));
		}
		if (omeroService.convert().supports(value, MaskPredicate.class)) {
			return toOMERO(omeroService.convert().convert(value,
				MaskPredicate.class));
		}

		return OMERO.rtype(value);
	}

	/**
	 * Converts an OMERO parameter value to an ImageJ value of the given type.
	 * <p>
	 * If the requested type is an image type (i.e., {@link Dataset},
	 * {@link DatasetView} or {@link ImageDisplay}) then the
	 * {@link #downloadImage} method will be used transparently to convert the
	 * OMERO image ID into such an object.
	 *
	 * @throws IllegalArgumentException if the conversion is not supported.
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public Object toImageJ(final omero.RType value, final Class<?> type)
		throws OMEROException
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
				throw new IllegalArgumentException("Unsupported collection: " + //
					value.getClass().getName());
			}
			// convert elements recursively
			Object element = null; // NB: Save 1st non-null element for later use.
			for (final omero.RType rType : omeroCollection) {
				final Object converted = toImageJ(rType, null);
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
				map.put(key, toImageJ(omeroMap.get(key), null));
			}
			return map;
		}

		// HACK: Use getValue() method if one exists for this type.
		// Reflection is necessary because there is no common interface
		// with the getValue() method implemented by each subclass.
		try {
			final Method method = value.getClass().getMethod("getValue");
			final Object result = method.invoke(value);
			return convert(result, type);
		}
		catch (final NoSuchMethodException | IllegalArgumentException
				| IllegalAccessException | InvocationTargetException exc)
		{
			throw new OMEROException("Unsupported type: " + //
				value.getClass().getName(), exc);
		}
	}

	// -- Images --

	/**
	 * Downloads the image with the given image ID from OMERO, storing the result
	 * into a new {@link Dataset}.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public Dataset downloadImage(final long imageID) throws OMEROException {
		final OMEROLocation source = new OMEROLocation(server, "/image/" + imageID);
		final SCIFIOConfig config = //
			new SCIFIOConfig().imgOpenerSetImgModes(ImgMode.CELL);
		try {
			return omeroService.datasetIO().open(source, config);
		}
		catch (final IOException exc) {
			throw new OMEROException(exc);
		}
	}

	/**
	 * Uploads the given {@link Dataset} to OMERO, returning the new image ID on
	 * the OMERO server.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public long uploadImage(final Dataset dataset) throws OMEROException {
		final OMEROLocation dest = new OMEROLocation(server, "");
		try {
			final Metadata metadata = omeroService.datasetIO().save(dataset, dest);
			if (metadata instanceof OMEROFormat.Metadata) {
				final OMEROFormat.Metadata omeroMeta = (OMEROFormat.Metadata) metadata;
				return omeroMeta.getImageID();
			}
		}
		catch (final IOException exc) {
			throw new OMEROException(exc);
		}
		return -1;
	}

	/**
	 * Uploads the given {@link Dataset} to OMERO, and optionally uploads the
	 * given ROIs and tables. The ROIs can also optionally be updated on the
	 * server.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public void uploadImage(final Dataset image, final boolean uploadROIs,
		final TreeNode<?> rois, final boolean updateROIs,
		final boolean uploadTables, final List<Table<?, ?>> tables,
		final String[] tableNames, final long omeroDatasetID) throws OMEROException
	{
		if (image == null) {
			throw new IllegalArgumentException("Image cannot be null!");
		}

		final DataManagerFacility dm = facility(DataManagerFacility.class);

		// Upload Image
		final long omeroImageID = uploadImage(image);

		// Upload/update attachments
		uploadImageAttachments(omeroImageID, uploadROIs, updateROIs, uploadTables,
			rois, tables, tableNames);

		// Attach image to Dataset
		if (omeroDatasetID > 0) {
			final ImageData imgData = new ImageData(new ImageI(omeroImageID, false));
			final DatasetData dsData = new DatasetData(new DatasetI(omeroDatasetID,
				false));
			OMERO.tell(() -> dm.addImageToDataset(ctx, imgData, dsData));
		}
	}

	/**
	 * Uploads the attachments to the OMERO server, and attaches them to the image
	 * associated with the given id. In order to update ROIs these ROIs must exist
	 * on the given image in OMERO.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public void uploadImageAttachments(final long imageID,
		final boolean uploadROIs, final boolean updateROIs,
		final boolean uploadTables, final TreeNode<?> rois,
		final List<Table<?, ?>> tables, final String[] tableNames)
		throws OMEROException
	{
		if ((updateROIs || uploadROIs) && rois != null) {
			if (updateROIs) updateAndReturnROIs(rois, imageID);
			else uploadROIs(rois, imageID);
		}

		if (uploadTables && tables != null && !tables.isEmpty()) {
			for (int i = 0; i < tables.size(); i++) {
				uploadTable(tableNames[i], tables.get(i), imageID);
			}
		}
	}

	// -- Tables --

	/**
	 * Downloads the table with the given ID from OMERO, storing the result into a
	 * new ImageJ {@link Table}.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public Table<?, ?> downloadTable(final long tableID) throws OMEROException {
		final TablesFacility tfac = facility(TablesFacility.class);
		final TableData omeroTable = //
			OMERO.ask(() -> tfac.getTable(ctx, tableID, 0, Integer.MAX_VALUE - 1));

		final TableDataColumn[] omeroColumns = omeroTable.getColumns();
		final Object[][] data = omeroTable.getData();

		final Table<?, ?> sjTable = TableUtils.createSciJavaTable(omeroColumns);
		sjTable.setRowCount((int) omeroTable.getNumberOfRows());

		boolean colsCreated = false;
		if (!(sjTable instanceof GenericTable)) {
			sjTable.appendColumns(omeroColumns.length);
			colsCreated = true;
		}

		for (int i = 0; i < omeroColumns.length; i++) {
			if (!colsCreated) {
				final Column<?> imageJCol = TableUtils.createSciJavaColumn(
					omeroColumns[i]);
				TableUtils.populateSciJavaColumn(omeroColumns[i].getType(),
					data[omeroColumns[i].getIndex()], imageJCol);
				((GenericTable) sjTable).add(omeroColumns[i].getIndex(), imageJCol);
			}
			else {
				TableUtils.populateSciJavaColumn(omeroColumns[i].getType(),
					data[omeroColumns[i].getIndex()], sjTable.get(i));
				sjTable.get(i).setHeader(omeroColumns[i].getName());
			}
		}
		return sjTable;
	}

	/**
	 * Downloads all tables associated with the given image ID in OMERO.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public List<Table<?, ?>> downloadTables(final long imageID)
		throws OMEROException
	{
		final TablesFacility tfac = facility(TablesFacility.class);

		final ImageData imageData = new ImageData(new ImageI(imageID, false));
		final Collection<FileAnnotationData> files = //
			OMERO.ask(() -> tfac.getAvailableTables(ctx, imageData));

		final List<Table<?, ?>> tables = new ArrayList<>(files.size());
		for (final FileAnnotationData file : files)
			tables.add(downloadTable(file.getFileID()));

		return tables;
	}

	/**
	 * Uploads a SciJava {@link Table} to OMERO, returning the new table ID on the
	 * OMERO server. Tables must be attached to a DataObject, thus the given image
	 * ID must be valid or this method will throw an exception.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public long uploadTable(final String name, final Table<?, ?> sjTable,
		final long imageID) throws OMEROException
	{
		final TableData omeroTable = //
			TableUtils.convertOMEROTable(sjTable, omeroService.convert());

		final BrowseFacility browseFacility = facility(BrowseFacility.class);
		final TablesFacility tablesFacility = facility(TablesFacility.class);

		return OMERO.ask(() -> {
			// Get image
			final ImageData image = browseFacility.getImage(ctx, imageID);

			// attach table to image
			final TableData stored = tablesFacility.addTable(ctx, image, name,
				omeroTable);
			return stored.getOriginalFileId();
		});
	}

	// -- ROIs --

	/**
	 * Downloads the {@link ROIData} with the given {@code roiID} from OMERO, and
	 * returns it as a {@link ROITree}.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public ROITree downloadROI(final long roiID) throws OMEROException {
		final ROIFacility roiFacility = facility(ROIFacility.class);
		final ROIResult roi = OMERO.ask(() -> roiFacility.loadROI(ctx, roiID));
		final ROIData rd = roi.getROIs().iterator().next();
		final TreeNode<?> treeNode = omeroService.convert().convert(rd,
			TreeNode.class);
		final ROITree tree = new DefaultROITree();
		tree.children().add(treeNode);
		return tree;
	}

	/**
	 * Downloads the ROIs associated with the given {@code imageID} from OMERO,
	 * and returns them as a {@link ROITree}.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public ROITree downloadROIs(final long imageID) throws OMEROException {
		final ROITree roiTree = new DefaultROITree();

		final ROIFacility roifac = facility(ROIFacility.class);
		final int roiCount = OMERO.ask(() -> roifac.getROICount(ctx, imageID));
		if (roiCount == 0) return roiTree;

		final List<ROIResult> roiresults = //
			OMERO.ask(() -> roifac.loadROIs(ctx, imageID));

		final Iterator<ROIResult> r = roiresults.iterator();
		while (r.hasNext()) {
			final ROIResult res = r.next();
			final Collection<ROIData> rois = res.getROIs();
			for (final ROIData roi : rois) {
				final TreeNode<?> ijRoi = omeroService.convert().convert(roi,
					TreeNode.class);
				if (ijRoi == null) {
					throw new IllegalArgumentException(
						"ROIData cannot be converted to ImageJ ROI");
				}
				roiTree.children().add(ijRoi);
			}
		}
		return roiTree;
	}

	/**
	 * Converts the given {@link TreeNode} to OMERO ROI(s), uploads them to the
	 * OMEROServer, and attaches them to the image with the specified ID. All ROIs
	 * are uploaded as new Objects, regardless of their origin.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public long[] uploadROIs(final TreeNode<?> ijROIs, final long imageID)
		throws OMEROException
	{
		final Collection<ROIData> omeroROIs = uploadAndReturnROIs(ijROIs, imageID);
		return ROIUtils.getROIIds(omeroROIs);
	}

	/**
	 * Converts the given {@link TreeNode} to OMERO ROIs(s). ROIs which originated
	 * from OMERO are updated on the server, and new ROIs are uploaded. The ids of
	 * the new ROI objects is returned.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public long[] updateROIs(final TreeNode<?> ijROIs, final long imageID)
		throws OMEROException
	{
		final Collection<ROIData> omeroROIs = updateAndReturnROIs(ijROIs, imageID);
		return ROIUtils.getROIIds(omeroROIs);
	}

	/**
	 * Converts the given {@link TreeNode} to OMERO ROI(s), and uploads them all
	 * as new Objects to the server. The new OMERO objects are then returned.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public Collection<ROIData> uploadAndReturnROIs(final TreeNode<?> ijROIs,
		final long imageID) throws OMEROException
	{
		final Interval interval = getImageInterval(imageID);
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs = //
			ROIUtils.split(ijROIs);
		final List<ROIData> savedOMERORois = new ArrayList<>();

		final ROIFacility roifac = facility(ROIFacility.class);

		// FIXME: This is a lot of server calls

		// Handle ROIs which originated in ImageJ
		for (final TreeNode<?> ijROI : splitROIs.getB()) {
			final List<ROIData> roiData = convertOMEROROI(ijROI, interval);
			final Collection<ROIData> saved = saveROIs(imageID, roifac, roiData);
			roiCache.addROIMapping(ijROI.data(), saved.iterator().next());
			savedOMERORois.add(saved.iterator().next());
		}

		// Handle ROIs which originated in OMERO
		for (final OMEROROICollection orc : splitROIs.getA()) {
			final List<ROIData> roiData = convertOMEROROI(orc, interval);
			final long roiID = roiData.get(0).getId();
			roiCache.removeDownloaded(roiID);
			final Collection<ROIData> saved = saveROIs(imageID, roifac, roiData);
			final ROIData savedRoi = saved.iterator().next();

			// NB: If updated later, the id will match correctly
			ROIUtils.updateROIData(orc, savedRoi);
			roiCache.updateServerROIData(savedRoi.getId(), savedRoi);

			savedOMERORois.add(savedRoi);
		}

		return savedOMERORois;
	}

	/**
	 * Convenience method to access the {@link LogService} from the
	 * {@link Context} in which this session was started.
	 */
	public LogService log() {
		return omeroService.log();
	}

	/**
	 * @return The {@link ROICache} for this session
	 */
	public ROICache roiCache() {
		return roiCache;
	}

	/**
	 * Helper method to ensure a given {@link Callable} is executed with the
	 * current {@code OMEROSession} as the active session in its {@link Context}'s
	 * {@link OMEROService}. This is necessary for some contextual actions (e.g.
	 * converters) that may need to connect with OMERO but whose only access is
	 * via an OMEROService.
	 * 
	 * @param callable Function to execute within this session's context.
	 * @return The result of the given callable
	 * @throws OMEROException
	 */
	public <T> T with(Callable<T> callable) throws OMEROException {
		omeroService.pushSession(this);

		T result;
		try {
			result = callable.call();
		}
		catch (Exception e) {
			throw new OMEROException(e);
		}
		finally {
			omeroService.popSession();
		}
		return result;
	}

	/**
	 * Converts the given {@link TreeNode} to OMERO ROI(s), creating new Objects
	 * on the server only for ROIs which didn't previously exist. ROIs which
	 * originated from OMERO are updated on the server. A collection of the new
	 * ROI objects is returned.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public Collection<ROIData> updateAndReturnROIs(final TreeNode<?> ijROIs,
		final long imageID) throws OMEROException
	{
		final Interval interval = getImageInterval(imageID);
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs = //
			ROIUtils.split(ijROIs);
		final List<Long> ids = new ArrayList<>();
		final ROIFacility roifac = facility(ROIFacility.class);
		final DataManagerFacility dm = facility(DataManagerFacility.class);

		// Handle ROIs which originated in OMERO
		for (final OMEROROICollection orc : splitROIs.getA()) {
			final ROIData converted = convertOMEROROI(orc, interval).get(0);
			final ROIData downloaded = //
				roiCache.getUpdatedServerROIData(converted.getId());
			final ROIData roiToSave = downloaded == null ? converted : downloaded;
			final DataObject savedOMERO = //
				OMERO.ask(() -> dm.saveAndReturnObject(ctx, roiToSave));
			if (!(savedOMERO instanceof ROIData)) {
				throw new IllegalArgumentException("ROI was not returned by OMERO");
			}
			final ROIData savedROI = (ROIData) savedOMERO;
			roiCache.updateServerROIData(savedROI.getId(), savedROI);
			ROIUtils.updateROIData(orc, savedROI);
			ids.add(savedROI.getId());
		}

		return OMERO.ask(() -> {
			final List<ROIData> newROIs = new ArrayList<>();

			// Handle ROIs which originated in ImageJ
			for (final TreeNode<?> dn : splitROIs.getB()) {
				final List<ROIData> converted = convertOMEROROI(dn, interval);
				final Collection<ROIData> saved = roifac.saveROIs(ctx, imageID,
					converted);
				if (roiCache.getROIMapping(dn.data()) == null) {
					newROIs.add(saved.iterator().next());
				}
				roiCache.addROIMapping(dn.data(), saved.iterator().next());
				ids.add(saved.iterator().next().getId());
			}

			// Check if any ROIs must be deleted
			final Collection<ROIResult> roisOnServer = roifac.loadROIs(ctx, imageID);
			for (final ROIResult result : roisOnServer) {
				for (final ROIData roi : result.getROIs()) {
					final long roiID = roi.getId();
					if (!ids.contains(roiID)) {
						dm.delete(ctx, roi.asIObject());

						// check if deleted ROI was mapped, if so remove mapping
						roiCache.removeDownloaded(roiID);
						roiCache.removeSaved(roiID);
					}
				}
			}

			return newROIs;
		});
	}

	// -- Accessors --

	/** Gets the {@link ExperimenterData} associated with this session. */
	public ExperimenterData getExperimenter() {
		return experimenter;
	}

	/** Gets the {@link Gateway} associated with this session. */
	public Gateway getGateway() {
		return gateway;
	}

	/** Gets the {@link SecurityContext} associated with this session. */
	public SecurityContext getSecurityContext() {
		return ctx;
	}

	/** Gets an OMERO {@code Pixels} descriptor, loading remotely as needed. */
	public Pixels loadPixels(final OMEROFormat.Metadata meta) throws ServerError {
		// return cached Pixels if available
		Pixels pixels = meta.getPixels();
		if (pixels != null) return pixels;

		// NB: We cannot write:
		//
		// loadImage(meta).getPixels(0)
		//
		// because retrieving an Image is not enough to
		// retrieve all fields of the linked Pixels.

		// load the pixels ID from the remote server
		final long pixelsID = loadPixelsID(meta);
		meta.setPixelsID(pixelsID);

		// load the Pixels from the remote server
		pixels = sfp.getPixelsService().retrievePixDescription(pixelsID);
		meta.setPixels(pixels);

		return pixels;
	}

	/** Gets the metadata's associated image name, loading remotely as needed. */
	public String loadImageName(final OMEROFormat.Metadata meta)
		throws ServerError
	{
		// return cached image name if available
		String name = meta.getName();
		if (name != null) return name;

		// load the Image name from the remote server
		name = loadImage(meta).getName().getValue();
		meta.setName(name);

		return name;
	}

	/**
	 * Obtains a raw pixels store for reading from the pixels associated with the
	 * given metadata.
	 */
	public RawPixelsStorePrx openPixels(final OMEROFormat.Metadata meta)
		throws OMEROException
	{
		final RawPixelsStorePrx store = OMERO.ask(() -> sfp.createRawPixelsStore());
		OMERO.tell(() -> store.setPixelsId(loadPixelsID(meta), false));
		return store;
	}

	/**
	 * Obtains a raw pixels store for writing to a new image which will be
	 * associated with the given metadata.
	 */
	public RawPixelsStorePrx createPixels(final OMEROFormat.Metadata meta)
		throws OMEROException
	{
		// create a new Image which will house the written pixels
		final ImageData newImage;

		try {
			newImage = createImage(meta);
		}
		catch (ServerError | FormatException exc) {
			throw new OMEROException("Error creating omero image", exc);
		}

		final long imageID = newImage.getId();
		meta.setImageID(imageID);

		// configure the raw pixels store
		final long pixelsID;
		final RawPixelsStorePrx store;
		try {
			store = sfp.createRawPixelsStore();
			pixelsID = newImage.getDefaultPixels().getId();
			store.setPixelsId(pixelsID, false);
		}
		catch (final ServerError exc) {
			throw new OMEROException("Error creating pixels store", exc);
		}
		meta.setPixelsID(pixelsID);

		return store;
	}

	/**
	 * @see #restore(OMEROCredentials)
	 */
	public void restore() throws OMEROException {
		restore(null);
	}

	/**
	 * Recreates the session if this session has previously had (@link
	 * {@link #close()} called. If this session is still open this method has no
	 * effect.
	 *
	 * @param credentials - Optional user credentials for logging into the server.
	 *          If not provided, a previously cached sessionId is required.
	 */
	public void restore(OMEROCredentials credentials) throws OMEROException {
		if (sfp == null || experimenter == null || ctx == null) {
			synchronized (this) {
				// Double lock to ensure we only re-initialize once
				if (sfp == null || experimenter == null || ctx == null) {
					initializeSession(credentials);
				}
			}
		}
	}

	// -- Closeable methods --

	@Override
	public void close() {
		client.__del__();
		gateway.disconnect();
		sfp = null;
		experimenter = null;
		ctx = null;
	}

	// -- Helper methods --

	private void initializeSession(OMEROCredentials credentials)
		throws OMEROException
	{
		initializeSession(credentials, null);
	}

	/**
	 * (Re-)establish a connection to the OMERO server.
	 */
	private void initializeSession(final OMEROCredentials credentials,
		final omero.client omeroClient) throws OMEROException
	{
		// initialize the client
		if (omeroClient == null) {
			if (server != null) {
				client = new omero.client(server.host, server.port);
			}
			else client = new omero.client();
		}
		else {
			client = omeroClient;
		}

		// log in to the server
		final String user = credentials == null ? null : credentials.getUser();
		final String pass = credentials == null ? null : credentials.getPassword();
		sfp = credentials == null ? //
			OMERO.ask(() -> client.createSession(user, pass)) : OMERO.ask(() -> client
				.joinSession(sessionID));

		// create OMERO gateway
		gateway = new Gateway(new SimpleLogger());

		// set experimenter and security context
		final String lHost = server == null ? OMERO.host(client) : server.host;
		final int lPort = server == null ? OMERO.port(client) : server.port;
		final String lUser = user == null ? sessionID : user;
		final String lPass = pass == null ? sessionID : pass;
		final LoginCredentials loginCredentials = //
			new LoginCredentials(lUser, lPass, lHost, lPort);
		experimenter = OMERO.ask(() -> gateway.connect(loginCredentials));

		// Update the sessionID if we were given credentials for authentication
		if (credentials != null) {
			try {
				sessionID = gateway.getSessionId(experimenter);
			}
			catch (DSOutOfServiceException exc) {
				throw new OMEROException("Failed to get session ID for experimenter: " +
					experimenter);
			}
		}
		ctx = new SecurityContext(experimenter.getGroupId());

		// Until imagej-omero #30 is resolved; see:
		// https://github.com/imagej/imagej-omero/issues/30
		// if (client.isSecure() && !credentials.isEncrypted()) {
		// client = client.createClient(false);
		// session = client.getSession();
		// }

		OMERO.tell(() -> sfp.detachOnDestroy());
	}

	/** Gets an OMERO {@code Image} descriptor, loading remotely as needed. */
	private Image loadImage(final OMEROFormat.Metadata meta) throws ServerError {
		// return cached Image if available
		Image image = meta.getImage();
		if (image != null) return image;

		final long imageID = meta.getImageID();
		if (imageID == 0) throw new IllegalArgumentException("Image ID is unset");

		// load the Image from the remote server
		final List<Long> ids = Arrays.asList(imageID);
		final List<Image> images = sfp.getContainerService().getImages("Image", ids,
			null);
		if (images == null || images.isEmpty()) {
			throw new IllegalArgumentException("Invalid image ID: " + imageID);
		}
		image = images.get(0);
		meta.setImage(image);

		return image;
	}

	/** Gets the metadata's associated pixels ID, loading remotely as needed. */
	private long loadPixelsID(final OMEROFormat.Metadata meta)
		throws ServerError
	{
		// return cached pixels ID if available
		long pixelsID = meta.getPixelsID();
		if (pixelsID != 0) return pixelsID;

		// obtain pixels ID from image ID
		pixelsID = loadImage(meta).getPixels(0).getId().getValue();
		meta.setPixelsID(pixelsID);

		return pixelsID;
	}

	private ImageData createImage(final OMEROFormat.Metadata meta)
		throws ServerError, FormatException
	{
		// create a new Image
		final ImageMetadata imageMeta = meta.get(0);
		final int xLen = axisLength(imageMeta, Axes.X);
		final int yLen = axisLength(imageMeta, Axes.Y);
		final int zLen = axisLength(imageMeta, Axes.Z);
		final int tLen = axisLength(imageMeta, Axes.TIME);
		final int cLen = axisLength(imageMeta, Axes.CHANNEL);
		final int sizeX = xLen == 0 ? 1 : xLen;
		final int sizeY = yLen == 0 ? 1 : yLen;
		final int sizeZ = zLen == 0 ? 1 : zLen;
		final int sizeT = tLen == 0 ? 1 : tLen;
		final int sizeC = cLen == 0 ? 1 : cLen;
		final List<Integer> channelList = new ArrayList<>(sizeC);
		for (int c = 0; c < sizeC; c++) {
			// TODO: Populate actual emission wavelengths?
			channelList.add(c);
		}
		final int pixelType = imageMeta.getPixelType();
		final PixelsType pixelsType = getPixelsType(pixelType);
		final String name = meta.getName();
		final String description = meta.getName();
		final RLong id = sfp.getPixelsService().createImage(sizeX, sizeY, sizeZ,
			sizeT, channelList, pixelsType, name, description);
		if (id == null) throw new FormatException("Cannot create image");

		// retrieve the newly created Image
		final List<Image> results = sfp.getContainerService().getImages(Image.class
			.getName(), Arrays.asList(id.getValue()), null);
		return new ImageData(results.get(0));
	}

	private PixelsType getPixelsType(final int pixelType) throws ServerError,
		FormatException
	{
		return getPixelsType(FormatTools.getPixelTypeString(pixelType));
	}

	private PixelsType getPixelsType(final String pixelType) throws ServerError,
		FormatException
	{
		final List<IObject> list = sfp.getPixelsService().getAllEnumerations(
			PixelsType.class.getName());
		final Iterator<IObject> iter = list.iterator();
		while (iter.hasNext()) {
			final PixelsType type = (PixelsType) iter.next();
			final String value = type.getValue().getValue();
			if (value.equals(pixelType)) return type;
		}
		throw new FormatException("Invalid pixel type: " + pixelType);
	}

	private int axisLength(final ImageMetadata imageMeta, final AxisType axisType)
		throws FormatException
	{
		final long axisLength = imageMeta.getAxisLength(axisType);
		if (axisLength > Integer.MAX_VALUE) {
			throw new FormatException("Length of " + axisType +
				" axis is too large for OMERO: " + axisLength);
		}
		return (int) axisLength;
	}

	/**
	 * Retrieves the {@link ImageData} from the OMERO server, and compute its
	 * {@link Interval}.
	 *
	 * @param imageID ID of {@link ImageData} whose bounds should be computed
	 * @return the computed {@link Interval}
	 */
	private Interval getImageInterval(final long imageID) throws OMEROException {
		final BrowseFacility browse = facility(BrowseFacility.class);
		return OMERO.ask(() -> {
			final ImageData image = browse.getImage(ctx, imageID);
			return new FinalInterval(new long[] { 0, 0 }, new long[] { image
				.getDefaultPixels().getSizeX(), image.getDefaultPixels().getSizeY() });
		});
	}

	/**
	 * Converts the given {@link TreeNode} to {@link ROIData}. If an interval is
	 * provided it will be used to convert unbounded {@link MaskPredicate}s to
	 * {@link ROIData}. If the interval is null, then unbounded
	 * {@link MaskPredicate}s cannot be converted to {@link ROIData} and an
	 * exception is thrown.
	 */
	private List<ROIData> convertOMEROROI(final TreeNode<?> dataNodeRois,
		final Interval interval) throws OMEROException
	{
		final List<ROIData> omeroROIs = new ArrayList<>();
		final List<TreeNode<?>> roiTreeNodes = //
			ROIUtils.collectROITreeNodes(dataNodeRois);

		for (final TreeNode<?> dn : roiTreeNodes) {
			ROIData oR;
			// If the data node has unbounded mask predicate data, apply the given
			// interval if non-null
			if (!(dn.data() instanceof Interval) && //
				!(dn.data() instanceof RealInterval) && //
				dn.data() instanceof MaskPredicate && interval != null)
			{
				oR = with(() -> omeroService.convert().convert(ROIUtils.interval(//
					(MaskPredicate<?>) dn.data(), interval), ROIData.class));
			}
			// else convert directly
			else oR = with(() -> omeroService.convert().convert(dn, ROIData.class));
			if (oR == null) throw new IllegalArgumentException("Unsupported type: " +
				dn.data().getClass());
			omeroROIs.add(oR);
		}
		return omeroROIs;
	}

	/**
	 * Converts the given POJO to the specified type (if given).
	 * <p>
	 * This method handles coersion of POJOs unwrapped from OMERO into the
	 * relevant type needed by ImageJ2. Examples:
	 * </p>
	 * <ol>
	 * <li>Many ImageJ2 types (such as {@link org.scijava.util.ColorRGB}) are
	 * mapped to {@link String} for use with OMERO. We lean on the SciJava Common
	 * {@link ConvertService#convert(Object, Class)} method to handle conversion
	 * of such types back to ImageJ2's expected type for the parameter.</li>
	 * <li>ImageJ2's image types (i.e., {@link Dataset}, {@link DatasetView} and
	 * {@link ImageDisplay}) are mapped to {@code long} since OMERO communicates
	 * about images using image IDs. Work must be done to download the image from
	 * a specified ID and convert the result to the appropriate type of ImageJ2
	 * object such as {@link Dataset}.</li>
	 * </ol>
	 */
	@SuppressWarnings("deprecation")
	private <T> T convert(final Object value, final Class<T> type)
		throws OMEROException
	{
		if (value == null) return null;
		if (type == null) {
			// no type given; try a simple cast
			@SuppressWarnings("unchecked")
			final T typedResult = (T) value;
			return typedResult;
		}

		// CTR START HERE

		// First, we look for registered objects of the requested type whose
		// toString() value matches the given string. This allows known sorts of
		// objects to be requested by name, including SingletonPlugin types like
		// CalculatorOp and ThresholdMethod.
		if (value instanceof String) {
			final String s = (String) value;
			final List<T> objects = omeroService.object().getObjects(type);
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
				final T dataset = (T) downloadImage(imageID);
				return dataset;
			}
			if (DatasetView.class.isAssignableFrom(type)) {
				final Dataset dataset = convert(value, Dataset.class);
				@SuppressWarnings("unchecked")
				final T dataView = (T) omeroService.imageDisplay().createDataView(
					dataset);
				return dataView;
			}
			if (ImageDisplay.class.isAssignableFrom(type)) {
				final Dataset dataset = convert(value, Dataset.class);
				@SuppressWarnings("unchecked")
				final T display = (T) omeroService.display().createDisplay(dataset);
				return display;
			}
			if (Table.class.isAssignableFrom(type)) {
				final long tableID = ((Number) value).longValue();
				@SuppressWarnings("unchecked")
				final T table = (T) downloadTable(tableID);
				return table;
			}
			if (TreeNode.class.isAssignableFrom(type)) {
				final long imageID = ((Number) value).longValue();
				@SuppressWarnings("unchecked")
				final T TreeNode = (T) downloadROIs(imageID);
				return TreeNode;
			}
			if (MaskPredicate.class.isAssignableFrom(type)) {
				final long roiID = ((Number) value).longValue();
				final TreeNode<?> TreeNode = downloadROI(roiID);
				final List<TreeNode<?>> children = TreeNode.children();
				@SuppressWarnings("unchecked")
				final T omeroMP = (T) children.get(0).children().get(0).data();
				if (children.size() > 1) {
					omeroService.log().warn("Requested OMERO ROI has more than " +
						"one ShapeData. Only one shape will be returned.");
				}
				return omeroMP;
			}
			if (omeroService.convert().supports(Dataset.class, type)) {
				final Dataset d = convert(value, Dataset.class);
				return omeroService.convert().convert(d, type);
			}
			if (omeroService.convert().supports(TreeNode.class, type)) {
				final TreeNode<?> dn = convert(value, TreeNode.class);
				return omeroService.convert().convert(dn, type);
			}
			if (omeroService.convert().supports(MaskPredicate.class, type)) {
				final MaskPredicate<?> mp = convert(value, MaskPredicate.class);
				return omeroService.convert().convert(mp, type);
			}
			if (omeroService.convert().supports(Table.class, type)) {
				final Table<?, ?> t = convert(value, Table.class);
				return omeroService.convert().convert(t, type);
			}
		}

		// use SciJava Common's automagical conversion routine
		final T converted = omeroService.convert().convert(value, type);
		if (converted == null) {
			omeroService.log().error("Cannot convert: " + value.getClass().getName() + //
				" to " + type.getName());
		}
		return converted;
	}

	private Collection<ROIData> saveROIs(final long imageID,
		final ROIFacility roifac, final List<ROIData> roiData) throws OMEROException
	{
		ROIUtils.clearROIs(roiData);
		return OMERO.ask(() -> roifac.saveROIs(ctx, imageID, roiData));
	}

	/**
	 * Gets a facility from the gateway.
	 *
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	private <T extends Facility> T facility(final Class<T> type)
		throws OMEROException
	{
		return OMERO.ask(() -> gateway.getFacility(type));
	}

	/*** Converts a {@link Collection} to an array of the given type. */
	private static <T> T[] toArray(final Collection<Object> collection,
		final Class<T> type)
	{
		@SuppressWarnings("unchecked")
		final T[] array = (T[]) Array.newInstance(type, 0);
		return collection.toArray(array);
	}
}
