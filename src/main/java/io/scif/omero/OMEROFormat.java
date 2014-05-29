/*
 * #%L
 * OME database I/O package for communicating with OME and OMERO servers.
 * %%
 * Copyright (C) 2005 - 2014 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
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

package io.scif.omero;

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

import net.imglib2.meta.Axes;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.axis.DefaultLinearAxis;
import net.imglib2.meta.axis.LinearAxis;
import omero.RDouble;
import omero.RInt;
import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.model.Image;
import omero.model.Pixels;

import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

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
		private Double physSizeX;

		@Field
		private Double physSizeY;

		@Field
		private Double physSizeZ;

		@Field
		private Integer physSizeC;

		@Field
		private Double physSizeT;

		@Field
		private String pixelType;

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

		public Double getPhysicalSizeX() {
			return physSizeX;
		}

		public Double getPhysicalSizeY() {
			return physSizeY;
		}

		public Double getPhysicalSizeZ() {
			return physSizeZ;
		}

		public Integer getPhysicalSizeC() {
			return physSizeC;
		}

		public Double getPhysicalSizeT() {
			return physSizeT;
		}

		public String getPixelType() {
			return pixelType;
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

		public void setPhysicalSizeX(final double physSizeX) {
			this.physSizeX = physSizeX;
		}

		public void setPhysicalSizeY(final double physSizeY) {
			this.physSizeY = physSizeY;
		}

		public void setPhysicalSizeZ(final double physSizeZ) {
			this.physSizeZ = physSizeZ;
		}

		public void setPhysicalSizeC(final int physSizeC) {
			this.physSizeC = physSizeC;
		}

		public void setPhysicalSizeT(final double physSizeT) {
			this.physSizeT = physSizeT;
		}

		public void setPixelType(final String pixelType) {
			this.pixelType = pixelType;
		}

		public void setImage(final Image image) {
			this.image = image;
			if (image == null) return;

			// sanity check for matching image IDs
			final long existingID = getImageID();
			final long id = image.getId().getValue();
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
			final long id = pixels.getId().getValue();
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
			final LinearAxis xAxis = new DefaultLinearAxis(Axes.X);
			if (physSizeX != null) xAxis.setScale(physSizeX);
			final LinearAxis yAxis = new DefaultLinearAxis(Axes.Y);
			if (physSizeY != null) yAxis.setScale(physSizeY);
			final LinearAxis zAxis = new DefaultLinearAxis(Axes.Z);
			if (physSizeZ != null) zAxis.setScale(physSizeZ);
			final LinearAxis cAxis = new DefaultLinearAxis(Axes.CHANNEL);
			if (physSizeC != null) cAxis.setScale(physSizeC);
			final LinearAxis tAxis = new DefaultLinearAxis(Axes.TIME);
			if (physSizeT != null) tAxis.setScale(physSizeT);
			final CalibratedAxis[] axes = { xAxis, yAxis, zAxis, cAxis, tAxis };
			final long[] axisLengths = { sizeX, sizeY, sizeZ, sizeC, sizeT };

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
			try {
				session = createSession(meta);
				pix = session.getPixelsInfo(meta);
			}
			catch (final ServerError err) {
				throw communicationException(err);
			}

			// parse image name
			meta.setName(pix.getImage().getName().getValue());

			// parse pixel sizes
			meta.setSizeX(pix.getSizeX().getValue());
			meta.setSizeY(pix.getSizeY().getValue());
			meta.setSizeZ(pix.getSizeZ().getValue());
			meta.setSizeC(pix.getSizeC().getValue());
			meta.setSizeT(pix.getSizeT().getValue());

			// parse physical pixel sizes
			final RDouble physSizeX = pix.getPhysicalSizeX();
			if (physSizeX != null) meta.setPhysicalSizeX(physSizeX.getValue());
			final RDouble physSizeY = pix.getPhysicalSizeY();
			if (physSizeY != null) meta.setPhysicalSizeY(physSizeY.getValue());
			final RDouble physSizeZ = pix.getPhysicalSizeZ();
			if (physSizeZ != null) meta.setPhysicalSizeZ(physSizeZ.getValue());
			final RInt physSizeC = pix.getWaveIncrement();
			if (physSizeC != null) meta.setPhysicalSizeC(physSizeC.getValue());
			final RDouble physSizeT = pix.getTimeIncrement();
			if (physSizeT != null) meta.setPhysicalSizeT(physSizeT.getValue());

			// parse pixel type
			meta.setPixelType(pix.getPixelsType().getValue().getValue());

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
				throw new FormatException("Error writing to OMERO: imageIndex=" +
					imageIndex + ", planeIndex=" + planeIndex, err);
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
		final long[] pos =
			FormatTools.rasterToPosition(imageIndex, planeIndex, metadata);
		final int zIndex = metadata.get(imageIndex).getAxisIndex(Axes.Z);
		final int cIndex = metadata.get(imageIndex).getAxisIndex(Axes.CHANNEL);
		final int tIndex = metadata.get(imageIndex).getAxisIndex(Axes.TIME);
		final int z = value(pos, zIndex);
		final int c = value(pos, cIndex);
		final int t = value(pos, tIndex);
		return new int[] { z, c, t };
	}

	// -- Utility methods --

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

	private static int value(final long[] pos, final int i) {
		if (i < 0 || i >= pos.length) return 0;
		final long value = pos[i];
		if (value > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Index too large: " + value);
		}
		return (int) value;
	}

}
