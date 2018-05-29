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

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.AbstractWriter;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.Field;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.MetadataService;
import io.scif.Plane;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.axis.LinearAxis;
import net.imagej.omero.roi.LazyROITree;
import net.imagej.roi.ROITree;
import net.imagej.table.Table;

import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ArrayUtils;

import omero.RInt;
import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ImageData;
import omero.model.Image;
import omero.model.Length;
import omero.model.Pixels;
import omero.model.Time;
import omero.model.enums.UnitsLength;
import omero.model.enums.UnitsTime;

/**
 * A SCIFIO {@link Format} which provides read/write access to pixels on an
 * OMERO server.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Format.class, priority = Priority.HIGH)
public class OMEROFormat extends AbstractFormat {

	// -- Format methods --

	@Override
	public String getFormatName() {
		return "OMERO";
	}

	// -- AbstractFormat methods --

	@Override
	protected String[] makeSuffixArray() {
		return new String[] { "omero" };
	}

	// -- Nested classes --

	public static class Checker extends AbstractChecker {

		@Override
		public boolean isFormat(final String name, final SCIFIOConfig config) {
			if (name != null && name.startsWith("omero:")) return true;
			return super.isFormat(name, config);
		}

	}

	public static class Metadata extends AbstractMetadata {

		@Field
		private OMEROLocation credentials;

		@Field
		private String name;

		@Field
		private long datasetID;

		@Field(label = "Image ID")
		private long imageID;

		@Field(label = "Pixels ID")
		private long pixelsID;

		@Field
		private int sizeX;

		@Field
		private int sizeY;

		@Field
		private int sizeZ;

		@Field
		private int sizeC;

		@Field
		private int sizeT;

		@Field
		private Length physSizeX;

		@Field
		private Length physSizeY;

		@Field
		private Length physSizeZ;

		@Field
		private RInt waveStart;

		@Field
		private RInt waveIncrement;

		@Field
		private Time timeIncrement;

		@Field
		private String pixelType;

		@Field
		private ROITree rois;

		@Field
		private List<Table<?, ?>> tables;

		/** Cached {@code Image} descriptor. */
		private Image image;

		/** Cached {@code Pixels} descriptor. */
		private Pixels pixels;

		// -- io.scif.omero.OMEROFormat.Metadata methods --

		public OMEROLocation getCredentials() {
			return credentials;
		}

		public String getName() {
			return name;
		}

		public long getDatasetID() {
			return datasetID;
		}

		public long getImageID() {
			return imageID;
		}

		public long getPixelsID() {
			return pixelsID;
		}

		public int getSizeX() {
			return sizeX;
		}

		public int getSizeY() {
			return sizeY;
		}

		public int getSizeZ() {
			return sizeZ;
		}

		public int getSizeC() {
			return sizeC;
		}

		public int getSizeT() {
			return sizeT;
		}

		public Length getPhysicalSizeX() {
			return physSizeX;
		}

		public Length getPhysicalSizeY() {
			return physSizeY;
		}

		public Length getPhysicalSizeZ() {
			return physSizeZ;
		}

		public RInt getWaveStart() {
			return waveStart;
		}

		public RInt getWaveIncrement() {
			return waveIncrement;
		}

		public Time getTimeIncrement() {
			return timeIncrement;
		}

		public String getPixelType() {
			return pixelType;
		}

		public ROITree getRois() {
			return rois;
		}

		public List<Table<?, ?>> getTables() {
			return tables;
		}

		public Image getImage() {
			return image;
		}

		public Pixels getPixels() {
			return pixels;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public void setCredentials(final OMEROLocation credentials) {
			this.credentials = credentials;
		}

		public void setImageID(final long imageID) {
			this.imageID = imageID;

			// clear stale Image cache
			final Image cachedImage = getImage();
			if (cachedImage != null && cachedImage.getId().getValue() != imageID) {
				setImage(null);
			}
		}

		public void setPixelsID(final long pixelsID) {
			this.pixelsID = pixelsID;

			// clear stale Pixels cache
			final Pixels cachedPixels = getPixels();
			if (cachedPixels != null && cachedPixels.getId().getValue() != pixelsID) {
				setPixels(null);
			}
		}

		public void setSizeX(final int sizeX) {
			this.sizeX = sizeX;
		}

		public void setSizeY(final int sizeY) {
			this.sizeY = sizeY;
		}

		public void setSizeZ(final int sizeZ) {
			this.sizeZ = sizeZ;
		}

		public void setSizeC(final int sizeC) {
			this.sizeC = sizeC;
		}

		public void setSizeT(final int sizeT) {
			this.sizeT = sizeT;
		}

		public void setPhysicalSizeX(final Length physSizeX) {
			this.physSizeX = physSizeX;
		}

		public void setPhysicalSizeY(final Length physSizeY) {
			this.physSizeY = physSizeY;
		}

		public void setPhysicalSizeZ(final Length physSizeZ) {
			this.physSizeZ = physSizeZ;
		}

		public void setWaveStart(final RInt waveStart) {
			this.waveStart = waveStart;
		}

		public void setWaveIncrement(final RInt waveIncrement) {
			this.waveIncrement = waveIncrement;
		}

		public void setTimeIncrement(final Time timeIncrement) {
			this.timeIncrement = timeIncrement;
		}

		public void setPixelType(final String pixelType) {
			this.pixelType = pixelType;
		}

		public void setRois(final ROITree rois) {
			this.rois = rois;
		}

		public void setTables(final List<Table<?, ?>> tables) {
			this.tables = tables;
		}

		public void setImage(final Image image) {
			this.image = image;
			if (image == null) return;

			// sanity check for matching image IDs
			final long existingID = getImageID();
			final long id = image.getId().getValue();
			if (existingID != id) {
				throw new IllegalArgumentException("existing image ID (" + existingID +
					") does not match the given Image (" + id + ")");
			}
		}

		public void setPixels(final Pixels pixels) {
			this.pixels = pixels;
			if (pixels == null) return;

			// sanity check for matching pixels IDs
			final long existingID = getPixelsID();
			final long id = pixels.getId().getValue();
			if (existingID != id) {
				throw new IllegalArgumentException("existing pixels ID (" + existingID +
					") does not match the given Pixels (" + id + ")");
			}
		}

		// -- io.scif.Metadata methods --

		@Override
		public void populateImageMetadata() {
			// TODO: Consider whether this check is really the right approach.
			// It is present because otherwise, the uninitialized format-specific
			// metadata fields overwrite the values populated by the ImgSaver.
			if (getImageCount() > 0) return; // already populated

			// construct dimensional axes
			final LinearAxis xAxis = axis(Axes.X, physSizeX);
			final LinearAxis yAxis = axis(Axes.Y, physSizeY);
			final LinearAxis zAxis = axis(Axes.Z, physSizeZ);
			final LinearAxis cAxis = axis(Axes.CHANNEL, waveStart, waveIncrement);
			final LinearAxis tAxis = axis(Axes.TIME, timeIncrement);
			// HACK: Do things in XYCZT order for ImageJ1 compatibility.
			// Technically, this _shouldn't_ matter because imagej-legacy
			// should take care of dimension swapping incompatible orderings.
			// But for now, this sidesteps the issue.
			final CalibratedAxis[] axes = { xAxis, yAxis, cAxis, zAxis, tAxis };
			final long[] axisLengths = { sizeX, sizeY, sizeC, sizeZ, sizeT };

			// obtain pixel type
			final int pixType = FormatTools.pixelTypeFromString(pixelType);

			// populate SCIFIO ImageMetadata
			createImageMetadata(1);
			final ImageMetadata imageMeta = get(0);
			imageMeta.setName(name);
			imageMeta.setAxes(axes, axisLengths);
			imageMeta.setPixelType(pixType);
			imageMeta.setMetadataComplete(true);
			imageMeta.setOrderCertain(true);
			imageMeta.setROIs(rois);
			imageMeta.setTables(tables);
		}
	}

	public static class Parser extends AbstractParser<Metadata> {

		@Parameter
		private MetadataService metadataService;

		@Parameter
		private OMEROService omeroService;

		@Override
		public void typedParse(final RandomAccessInputStream stream,
			final Metadata meta, final SCIFIOConfig config) throws IOException,
			FormatException
		{
			// parse OMERO credentials from source string
			parseArguments(metadataService, stream.getFileName(), meta);

			// initialize OMERO session
			final OMEROSession session;
			final Pixels pix;
			try {
				session = omeroService.session(meta.getCredentials());
				pix = session.loadPixels(meta);
				session.loadImageName(meta);
			}
			catch (final ServerError err) {
				throw communicationException(err);
			}
			catch (final Ice.LocalException exc) {
				throw versionException(exc);
			}

			// Set table and rois to lazy loaders
			meta.setRois(new LazyROITree(null, meta.getImageID(), //
				meta.getCredentials(), omeroService, log()));
			meta.setTables(new LazyTableList(meta.getImageID(), meta.getCredentials(),
				omeroService, log()));

			// parse pixel sizes
			meta.setSizeX(pix.getSizeX().getValue());
			meta.setSizeY(pix.getSizeY().getValue());
			meta.setSizeZ(pix.getSizeZ().getValue());
			meta.setSizeC(pix.getSizeC().getValue());
			meta.setSizeT(pix.getSizeT().getValue());

			// parse physical pixel sizes
			meta.setPhysicalSizeX(pix.getPhysicalSizeX());
			meta.setPhysicalSizeY(pix.getPhysicalSizeY());
			meta.setPhysicalSizeZ(pix.getPhysicalSizeZ());
			meta.setWaveStart(pix.getWaveStart());
			meta.setWaveIncrement(pix.getWaveIncrement());
			meta.setTimeIncrement(pix.getTimeIncrement());

			// parse pixel type
			meta.setPixelType(pix.getPixelsType().getValue().getValue());
		}

	}

	public static class Reader extends ByteArrayReader<Metadata> {

		@Parameter
		private OMEROService omeroService;

		private OMEROSession session;
		private RawPixelsStorePrx store;

		@Override
		public ByteArrayPlane openPlane(final int imageIndex, final long planeIndex,
			final ByteArrayPlane plane, final long[] planeMin, final long[] planeMax,
			final SCIFIOConfig config) throws FormatException, IOException
		{
			// TODO: Consider whether to reuse OMERO session from the parsing step.
			if (session == null) initSession();

			final Map<AxisType, CalibratedAxis> axisMap = //
				createAxisMap(getMetadata().get(imageIndex));
			final int[] zct = zct(planeIndex, getMetadata().get(imageIndex), axisMap);
			try {
				// FIXME: Check before array access, and before casting.
				final int x = (int) planeMin[0];
				final int y = (int) planeMin[1];
				// NB: planeMin is inclusive; planeMax is exclusive.
				final int w = (int) (planeMax[0] - planeMin[0]);
				final int h = (int) (planeMax[1] - planeMin[1]);
				log().debug("openPlane: z = " + zct[0] + " c = " + zct[1] + " t = " + zct[2]);
				log().debug("openPlane: x = " + x + ", y = " + y);
				log().debug("openPlane: w = " + w + ", h = " + h);
				final byte[] tile = store.getTile(zct[0], zct[1], zct[2], x, y, w, h);
				plane.setData(tile);
			}
			catch (final ServerError err) {
				throw communicationException(err);
			}
			catch (final Ice.LocalException exc) {
				throw versionException(exc);
			}

			return plane;
		}

		@Override
		public void close() {
			if (session != null) ((DefaultOMEROSession) session).close();
			session = null;
			store = null;
		}

		@Override
		protected String[] createDomainArray() {
			// FIXME: Decide on the proper domains to report here.
			return new String[] { FormatTools.LM_DOMAIN };
		}

		private void initSession() throws FormatException {
			try {
				session = omeroService.session(getMetadata().getCredentials());
				store = session.openPixels(getMetadata());
			}
			catch (final ServerError err) {
				throw communicationException(err);
			}
			catch (final Ice.LocalException exc) {
				throw versionException(exc);
			}
		}

	}

	public static class Writer extends AbstractWriter<Metadata> {

		@Parameter
		private MetadataService metadataService;

		@Parameter
		private OMEROService omeroService;

		private OMEROSession session;
		private RawPixelsStorePrx store;

		@Override
		public void writePlane(final int imageIndex, final long planeIndex,
			final Plane plane, final long[] planeMin, final long[] planeMax)
			throws FormatException, IOException
		{
			if (session == null) initSession();

			final ImageMetadata imageMeta = getMetadata().get(imageIndex);
			final List<CalibratedAxis> planarAxes = imageMeta.getAxesPlanar();
			final Map<AxisType, CalibratedAxis> axisMap = createAxisMap(imageMeta);

			final long sizeX = imageMeta.getAxisLength(planarAxes.get(0));
			final long sizeY = imageMeta.getAxisLength(planarAxes.get(1));
			final int nBytes = ArrayUtils.safeMultiply32(sizeX, sizeY);
			final byte[] bytes = plane.getBytes();
			final int[] zct = zct(planeIndex, imageMeta, axisMap);

			final int planarAxisCount = planarAxes.size();
			switch (planarAxisCount) {
				case 2: // XY
					writePlaneToOMERO(bytes, zct[0], zct[1], zct[2], //
						plane, imageIndex, planeIndex, log(), store);
					break;
				case 3: // XYC
					final CalibratedAxis channelAxis = axisMap.get(Axes.CHANNEL);
					if (!planarAxes.contains(channelAxis)) {
						throw new IllegalArgumentException("Unsupported planar axes: " +
							planarAxes.stream()//
								.map(axis -> axis.type())//
								.collect(Collectors.toList()));
					}
					final long channelCount = imageMeta.getAxisLength(channelAxis);
					int startByte = 0;
					int endByte = nBytes;
					for (int i = 0; i < channelCount; i++) {
						final byte[] data = Arrays.copyOfRange(bytes, startByte, endByte);
						writePlaneToOMERO(data, zct[0], zct[1] + i, zct[2], //
							plane, imageIndex, planeIndex, log(), store);
						startByte = endByte;
						endByte = endByte + nBytes;
					}
					break;
				default:
					throw new IllegalArgumentException(//
						"Unsupported planar axis count: " + planarAxisCount);
			}
		}

		@Override
		public void close() {
			if (store != null) {
				// save the data
				try {
					// store resultant image ID into the metadata
					final Image image = store.save().getImage();
					getMetadata().setImageID(image.getId().getValue());

					// try to attach image to dataset
					if (session.getExperimenter() != null && //
						session.getGateway() != null && getMetadata().getDatasetID() != 0)
					{
						attachImageToDataset(image, getMetadata().getDatasetID());
					}

					store.close();
				}
				catch (final ServerError err) {
					log().error("Error communicating with OMERO", err);
				}
			}
			store = null;
			if (session != null) ((DefaultOMEROSession) session).close();
			session = null;
		}

		@Override
		protected String[] makeCompressionTypes() {
			return new String[0];
		}

		private void initSession() throws FormatException {
			try {
				final Metadata meta = getMetadata();

				// parse OMERO credentials from destination string
				// HACK: Get destination string from the metadata's dataset name.
				// This is set in the method: AbstractWriter#setDest(String, int).
				parseArguments(metadataService, meta.getDatasetName(), meta);

				session = omeroService.session(meta.getCredentials());
				store = session.createPixels(meta);
			}
			catch (final ServerError err) {
				throw communicationException(err);
			}
		}

		/**
		 * Attaches an image to an OMERO dataset.
		 */
		private void attachImageToDataset(final Image image, final long datasetID) {
			try {
				final Gateway gateway = session.getGateway();
				final SecurityContext ctx = session.getSecurityContext();
				final BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
				final DataManagerFacility dmf = gateway.getFacility(
					DataManagerFacility.class);

				final ArrayList<Long> collect = new ArrayList<>();
				collect.add(datasetID);
				final Collection<DatasetData> temp = browse.getDatasets(ctx, collect);

				if (!temp.isEmpty()) {
					final DatasetData ds = temp.iterator().next();
					final ImageData imgdata = browse.getImage(ctx, //
						image.getId().getValue());
					dmf.addImageToDataset(ctx, imgdata, ds);
				}
			}
			catch (final ExecutionException err) {
				log().error("Error creating Facility", err);
			}
			catch (final DSAccessException err) {
				log().error("Error attaching image to OMERO dataset", err);
			}
			catch (final DSOutOfServiceException err) {
				log().error("Error attaching image to OMERO dataset", err);
			}
		}

	}

	// -- Utility methods --

	public static int[] zct(final long planeIndex, final ImageMetadata imageMeta,
		final Map<AxisType, CalibratedAxis> axisMap)
	{
		final long[] lengths = new long[3];
		lengths[0] = axisLength(imageMeta, axisMap, Axes.Z);
		lengths[1] = axisLength(imageMeta, axisMap, Axes.CHANNEL);
		lengths[2] = axisLength(imageMeta, axisMap, Axes.TIME);

		final AxisType[] axes = { Axes.Z, Axes.CHANNEL, Axes.TIME };
		final long[] zct = FormatTools.rasterToPosition(lengths, planeIndex);
		final int[] result = new int[zct.length];
		for (int i = 0; i < zct.length; i++)
			result[i] = value(axes[i], zct[i]);
		return result;
	}

	public static void parseArguments(final MetadataService metadataService,
		final String string, final Metadata meta) throws FormatException
	{
		// strip omero prefix and/or suffix
		final String clean = string.replaceFirst("^omero:", "").replaceFirst(
			"\\.omero$", "");

		final Map<String, Object> map = metadataService.parse(clean, "&");

		try {
			final OMEROLocation credentials = new OMEROLocation(map);
			meta.setCredentials(credentials);
		}
		catch (final URISyntaxException exc) {
			throw connectionException(exc);
		}

		// populate other metadata: image ID, etc.
		metadataService.populate(meta, map);
	}

	// -- Helper methods --

	private static FormatException communicationException(final Throwable cause) {
		return new FormatException("Error communicating with OMERO", cause);
	}

	private static FormatException connectionException(final Throwable cause) {
		return new FormatException("Error connecting to OMERO", cause);
	}

	private static FormatException versionException(final Throwable cause) {
		return new FormatException(
			"Error communicating with OMERO -- server version mismatch?", cause);
	}

	private static FormatException writerException(final Throwable t,
		final int imageIndex, final long planeIndex)
	{
		return new FormatException("Error writing to OMERO: imageIndex=" +
			imageIndex + ", planeIndex=" + planeIndex, t);
	}

	private static int value(final AxisType axisType, final long value) {
		if (value < 0) return 0;
		if (value <= Integer.MAX_VALUE) return (int) value;
		throw new IllegalArgumentException(axisType + " axis position too large: " +
			value);
	}

	private static LinearAxis axis(final AxisType axisType, final Length q) {
		final DefaultLinearAxis axis = new DefaultLinearAxis(axisType);
		if (q != null) calibrate(axis, null, q.getValue(), unit(q.getUnit()));
		return axis;
	}

	private static LinearAxis axis(final AxisType axisType, final RInt origin,
		final RInt scale)
	{
		final DefaultLinearAxis axis = new DefaultLinearAxis(axisType);
		calibrate(axis, i(origin), i(scale), "nm");
		return axis;
	}

	private static LinearAxis axis(final AxisType axisType, final Time q) {
		final DefaultLinearAxis axis = new DefaultLinearAxis(axisType);
		if (q != null) calibrate(axis, null, q.getValue(), unit(q.getUnit()));
		return axis;
	}

	private static void calibrate(final LinearAxis axis, final Number origin,
		final Number scale, final String unit)
	{
		if (origin != null) axis.setOrigin(origin.doubleValue());
		if (scale != null) axis.setScale(scale.doubleValue());
		if (unit != null) axis.setUnit(unit);
	}

	private static String unit(final UnitsTime unit) {
		return OMEROUtils.unit(unit).getSymbol();
	}

	private static String unit(final UnitsLength unit) {
		return OMEROUtils.unit(unit).getSymbol();
	}

	private static Integer i(final RInt value) {
		return value == null ? null : value.getValue();
	}

	private static long axisLength(final ImageMetadata imageMeta,
		final Map<AxisType, CalibratedAxis> axisMap, AxisType type)
	{
		final CalibratedAxis axis = axisMap.get(type);
		return axis == null ? -1 : imageMeta.getAxisLength(axis);
	}

	private static Map<AxisType, CalibratedAxis> createAxisMap(
		final ImageMetadata imageMeta)
	{
		final Map<AxisType, CalibratedAxis> axisMap = new HashMap<>();
		final List<CalibratedAxis> imageAxes = imageMeta.getAxes();
		final List<CalibratedAxis> planarAxes = imageMeta.getAxesPlanar();

		if (imageAxes.size() > 5) {
			throw new IllegalArgumentException("Cannot have more than 5 axes!");
		}
		if (planarAxes.size() > 3) {
			throw new IllegalArgumentException(
				"Cannot have more than three planar axes!");
		}
		for (final CalibratedAxis imageAxis : imageAxes) {
			mapAxesToType(imageMeta, axisMap, imageAxis);
		}

		return axisMap;
	}

	private static final List<AxisType> XYZCT = //
		Arrays.asList(Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL, Axes.TIME);

	private static void mapAxesToType(final ImageMetadata imageMeta,
		final Map<AxisType, CalibratedAxis> axisMap, final CalibratedAxis axis)
	{
		final AxisType type = axis.type();
		if (axis.type().toString().equals(Axes.unknown().toString())) {
			axisMap.put(computeUnknownAxisType(imageMeta, axisMap), axis);
		}
		else if (XYZCT.contains(type)) {
			if (axisMap.containsKey(type)) {
				throw new IllegalArgumentException("Duplicate " + type + " axis found");
			}
			axisMap.put(type, axis);
		}
		else throw new IllegalArgumentException("Unsupported axis type: " + type);
	}

	private static AxisType computeUnknownAxisType(final ImageMetadata imageMeta,
		final Map<AxisType, CalibratedAxis> axisMap)
	{
		for (final AxisType type : XYZCT) {
			if (isAxisAvailable(imageMeta, axisMap, type)) return type;
		}
		throw new IllegalArgumentException(
			"Cannot map unknown axis type, no labels free.");
	}

	private static boolean isAxisAvailable(final ImageMetadata imageMeta,
		final Map<AxisType, CalibratedAxis> axisMap, final AxisType type)
	{
		return imageMeta.getAxisIndex(type) < 0 && !axisMap.containsKey(type);
	}

	private static void writePlaneToOMERO(final byte[] data, final int z,
		final int c, final int t, final Plane plane, final int imageIndex,
		final long planeIndex, final LogService log, final RawPixelsStorePrx store)
		throws FormatException
	{
		try {
			log.debug("writePlane: bytes = " + data.length);
			log.debug("writePlane: z = " + z + " c = " + c + " t = " + t);
			log.debug("writePlane: w = " + plane.getImageMetadata().getAxisLength(0));
			log.debug("writePlane: h = " + plane.getImageMetadata().getAxisLength(1));
			log.debug("writePlane: num planar = " + //
				plane.getImageMetadata().getPlanarAxisCount());
			store.setPlane(data, z, c, t);
		}
		catch (final ServerError err) {
			throw writerException(err, imageIndex, planeIndex);
		}
		catch (final Ice.LocalException exc) {
			throw versionException(exc);
		}
	}

}
