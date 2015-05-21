/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2015 Open Microscopy Environment:
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

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
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
import java.util.Map;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.axis.LinearAxis;
import omero.RDouble;
import omero.RInt;
import omero.RLong;
import omero.RString;
import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.model.ChannelBinding;
import omero.model.Image;
import omero.model.Length;
import omero.model.Pixels;
import omero.model.RenderingDef;
import omero.model.StatsInfo;
import omero.model.Time;
import omero.model.enums.UnitsLength;
import omero.model.enums.UnitsTime;

import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;
import org.scijava.util.ObjectArray;

/**
 * A SCIFIO {@link Format} which provides read/write access to pixels on an
 * OMERO server.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Format.class, priority = Priority.HIGH_PRIORITY)
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
		private OMEROCredentials credentials;

		@Field
		private String name;

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
		private ObjectArray<Channel> channels;

		/** Cached {@code Image} descriptor. */
		private Image image;

		/** Cached {@code Pixels} descriptor. */
		private Pixels pixels;

		// -- io.scif.omero.OMEROFormat.Metadata methods --

		public OMEROCredentials getCredentials() {
			return credentials;
		}

		public String getName() {
			return name;
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

		public Double getChannelMin(final int c) {
			return channels.get(c).dataMin;
		}

		public Double getChannelMax(final int c) {
			return channels.get(c).dataMax;
		}

		public Double getDisplayMin(final int c) {
			return channels.get(c).displayMin;
		}

		public Double getDisplayMax(final int c) {
			return channels.get(c).displayMax;
		}

		public ColorRGB getChannelColor(final int c) {
			return channels.get(c).color;
		}

		public String getChannelLUT(final int c) {
			return channels.get(c).lut;
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

		public void setCredentials(final OMEROCredentials credentials) {
			this.credentials = credentials;
		}

		public void setImageID(final long imageID) {
			this.imageID = imageID;

			// clear stale Image cache
			final Image cachedImage = getImage();
			if (cachedImage != null && v(cachedImage.getId()) != imageID) {
				setImage(null);
			}
		}

		public void setPixelsID(final long pixelsID) {
			this.pixelsID = pixelsID;

			// clear stale Pixels cache
			final Pixels cachedPixels = getPixels();
			if (cachedPixels != null && v(cachedPixels.getId()) != pixelsID) {
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
			channels = new ObjectArray<Channel>(Channel.class, sizeC);
			for (int c = 0; c < sizeC; c++) {
				channels.set(c, new Channel());
			}
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

		public void setChannelMin(final int c, final Double min) {
			channels.get(c).dataMin = min;
		}

		public void setChannelMax(final int c, final Double max) {
			channels.get(c).dataMax = max;
		}

		public void setDisplayMin(final int c, final Double min) {
			channels.get(c).displayMin = min;
		}

		public void setDisplayMax(final int c, final Double max) {
			channels.get(c).displayMax = max;
		}

		public void setChannelColor(final int c, final ColorRGB color) {
			channels.get(c).color = color;
		}

		public void setChannelLUT(final int c, final String lut) {
			channels.get(c).lut = lut;
		}

		public void setImage(final Image image) {
			this.image = image;
			if (image == null) return;

			// sanity check for matching image IDs
			final long existingID = getImageID();
			final Long id = v(image.getId());
			if (existingID != id) {
				throw new IllegalArgumentException("existing image ID (" +
					existingID + ") does not match the given Image (" + id + ")");
			}
		}

		public void setPixels(final Pixels pixels) {
			this.pixels = pixels;
			if (pixels == null) return;

			// sanity check for matching pixels IDs
			final long existingID = getPixelsID();
			final Long id = v(pixels.getId());
			if (existingID != id) {
				throw new IllegalArgumentException("existing pixels ID (" +
					existingID + ") does not match the given Pixels (" + id + ")");
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
		}

		// -- Helper classes --

		/** Struct for channel-specific metadata. */
		private class Channel {
			@Field
			private Double dataMin, dataMax;
			@Field
			private Double displayMin, displayMax;
			@Field
			private ColorRGB color;
			@Field
			private String lut;
		}

	}

	public static class Parser extends AbstractParser<Metadata> {

		@Parameter
		private MetadataService metadataService;

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
			final RenderingDef renderingDef;
			try {
				session = createSession(meta);
				pix = session.loadPixels(meta);
				session.loadImageName(meta);
				renderingDef = session.loadRenderingDef(meta);
			}
			catch (final ServerError err) {
				throw communicationException(err);
			}
			catch (final Ice.LocalException exc) {
				throw versionException(exc);
			}

			// parse pixel sizes
			meta.setSizeX(v(pix.getSizeX()));
			meta.setSizeY(v(pix.getSizeY()));
			meta.setSizeZ(v(pix.getSizeZ()));
			meta.setSizeC(v(pix.getSizeC()));
			meta.setSizeT(v(pix.getSizeT()));

			// parse physical pixel sizes
			meta.setPhysicalSizeX(pix.getPhysicalSizeX());
			meta.setPhysicalSizeY(pix.getPhysicalSizeY());
			meta.setPhysicalSizeZ(pix.getPhysicalSizeZ());
			meta.setWaveStart(pix.getWaveStart());
			meta.setWaveIncrement(pix.getWaveIncrement());
			meta.setTimeIncrement(pix.getTimeIncrement());

			// parse pixel type
			meta.setPixelType(v(pix.getPixelsType().getValue()));

			// parse channel min/max values and rendering settings
			for (int c = 0; c < meta.getSizeC(); c++) {
				final StatsInfo statsInfo = pix.getChannel(c).getStatsInfo();
				meta.setChannelMin(c, v(statsInfo.getGlobalMin()));
				meta.setChannelMax(c, v(statsInfo.getGlobalMax()));

				final ChannelBinding channel = renderingDef.getChannelBinding(c);
				meta.setDisplayMin(c, v(channel.getInputStart()));
				meta.setDisplayMax(c, v(channel.getInputEnd()));

				final Integer r = v(channel.getRed());
				final Integer g = v(channel.getGreen());
				final Integer b = v(channel.getBlue());
				final Integer a = v(channel.getAlpha());
				meta.setChannelColor(c, color(r, g, b, a));

				meta.setChannelLUT(c, v(channel.getLookupTable()));
			}

			// terminate OMERO session
			session.close();
		}

	}

	public static class Reader extends ByteArrayReader<Metadata> {

		private OMEROSession session;
		private RawPixelsStorePrx store;

		@Override
		public ByteArrayPlane openPlane(final int imageIndex,
			final long planeIndex, final ByteArrayPlane plane, final long[] planeMin,
			final long[] planeMax, final SCIFIOConfig config) throws FormatException,
			IOException
		{
			// TODO: Consider whether to reuse OMERO session from the parsing step.
			if (session == null) initSession();

			final int[] zct = zct(imageIndex, planeIndex, getMetadata());
			try {
				// FIXME: Check before array access, and before casting.
				final int x = (int) planeMin[0];
				final int y = (int) planeMin[1];
				final int w = (int) (planeMax[0] - planeMin[0]);
				final int h = (int) (planeMax[1] - planeMin[1]);
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
			if (session != null) session.close();
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
				session = createSession(getMetadata());
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
		private LogService log;

		private OMEROSession session;
		private RawPixelsStorePrx store;

		@Override
		public void writePlane(final int imageIndex, final long planeIndex,
			final Plane plane, final long[] planeMin, final long[] planeMax)
			throws FormatException, IOException
		{
			// TODO: Consider whether to reuse OMERO session from somewhere else.
			if (session == null) initSession();

			final byte[] bytes = plane.getBytes();
			final int[] zct = zct(imageIndex, planeIndex, getMetadata());
			try {
				log.debug("writePlane: bytes = " + bytes.length);
				log.debug("writePlane: z = " + zct[0] + " c = " + zct[1] + " t = " + zct[2]);
				log.debug("writePlane: w = " + plane.getImageMetadata().getAxisLength(0));
				log.debug("writePlane: h = " + plane.getImageMetadata().getAxisLength(1));
				log.debug("writePlane: num planar = " + plane.getImageMetadata().getPlanarAxisCount());
				store.setPlane(bytes, zct[0], zct[1], zct[2]);
			}
			catch (final ServerError err) {
				throw writerException(err, imageIndex, planeIndex);
			}
			catch (final Ice.LocalException exc) {
				throw versionException(exc);
			}
		}

		@Override
		public void close() {
			if (store != null) {
				// save the data
				try {
					// store resultant image ID into the metadata
					final Image image = store.save().getImage();
					getMetadata().setImageID(v(image.getId()));
					store.close();
				}
				catch (final ServerError err) {
					log().error("Error communicating with OMERO", err);
				}
			}
			store = null;
			if (session != null) session.close();
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

				session = createSession(meta);
				store = session.createPixels(meta);
			}
			catch (final ServerError err) {
				throw communicationException(err);
			}
		}

	}

	// -- Utility methods --

	public static int[] zct(final int imageIndex, final long planeIndex,
		final Metadata metadata)
	{
		final AxisType[] axes = {Axes.Z, Axes.CHANNEL, Axes.TIME};
		final long[] zct = rasterToPosition(imageIndex, planeIndex, metadata, axes);
		final int[] result = new int[zct.length];
		for (int i = 0; i < zct.length; i++)
			result[i] = value(axes[i], zct[i]);
		return result;
	}

	/**
	 * Gets the position per axis of the given plane index, reordering the axes as
	 * requested.
	 * 
	 * @param imageIndex TODO
	 * @param planeIndex The plane to convert to axis coordinates.
	 * @param metadata TODO
	 * @param axisTypes The axes whose coordinates are desired. TODO if a type is
	 *          given that is not part of the image, this method gives -1 for that
	 *          axis's position.
	 * @return TODO
	 */
	public static long[]
		rasterToPosition(final int imageIndex, final long planeIndex,
			final Metadata metadata, final AxisType... axisTypes)
	{
		// FIXME: Move this into SCIFIO core in a utility class.
		final long[] nPos =
			FormatTools.rasterToPosition(imageIndex, planeIndex, metadata);

		final ImageMetadata imageMeta = metadata.get(imageIndex);
		final int planarAxisCount = imageMeta.getPlanarAxisCount();

		final long[] kPos = new long[axisTypes.length];
		for (int i = 0; i < kPos.length; i++) {
			final int index = imageMeta.getAxisIndex(axisTypes[i]);
			kPos[i] = index < 0 ? -1 : nPos[index - planarAxisCount];
		}

		return kPos;
	}

	public static void parseArguments(final MetadataService metadataService,
		final String string, final Metadata meta)
	{
		// strip omero prefix and/or suffix
		final String clean =
			string.replaceFirst("^omero:", "").replaceFirst("\\.omero$", "");

		final Map<String, Object> map = metadataService.parse(clean, "&");
		final OMEROCredentials credentials = new OMEROCredentials();
		// populate OMERO credentials
		metadataService.populate(credentials, map);
		meta.setCredentials(credentials);
		// populate other metadata: image ID, etc.
		metadataService.populate(meta, map);
	}

	// -- Helper methods --

	private static OMEROSession createSession(final Metadata meta)
		throws FormatException
	{
		try {
			return new OMEROSession(meta.getCredentials());
		}
		catch (final ServerError err) {
			throw communicationException(err);
		}
		catch (final PermissionDeniedException exc) {
			throw connectionException(exc);
		}
		catch (final CannotCreateSessionException exc) {
			throw connectionException(exc);
		}
	}

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
		calibrate(axis, v(origin), v(scale), "nm");
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

	private static ColorRGB color(final Integer r, final Integer g,
		final Integer b, final Integer a)
	{
		if (r == null || g == null || b == null) return null;
		return a == null ? new ColorRGB(r, g, b) : new ColorRGBA(r, g, b, a);
	}

	private static Double v(final RDouble value) {
		return value == null ? null : value.getValue();
	}

	private static Integer v(final RInt value) {
		return value == null ? null : value.getValue();
	}

	private static Long v(final RLong value) {
		return value == null ? null : value.getValue();
	}

	private static String v(final RString value) {
		return value == null ? null : value.getValue();
	}

}
