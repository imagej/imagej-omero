/*
 * #%L
 * OME database I/O package for communicating with OME and OMERO servers.
 * %%
 * Copyright (C) 2005 - 2013 Open Microscopy Environment:
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
import io.scif.Plane;
import io.scif.formats.FakeFormat;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.imglib2.meta.Axes;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.DefaultCalibratedAxis;
import omero.RDouble;
import omero.RInt;
import omero.ServerError;
import omero.api.IPixelsPrx;
import omero.api.RawPixelsStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.model.Image;
import omero.model.Pixels;

import org.scijava.plugin.Plugin;

/**
 * A SCIFIO {@link Format} which provides read/write access to pixels on an
 * OMERO server.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Format.class)
public class OMEROFormat extends AbstractFormat {

	// -- Format methods --

	@Override
	public String getFormatName() {
		return "OMERO";
	}

	@Override
	public String[] getSuffixes() {
		return new String[] { "omero" };
	}

	// -- Nested classes --

	public static class Metadata extends AbstractMetadata {

		private transient omero.client client;
		private transient ServiceFactoryPrx session;
		private transient Pixels pix;
		private transient RawPixelsStorePrx store;

		@Field
		private String name;

		@Field
		private String server;

		@Field
		private int port = 4664;

		@Field
		private String sessionID;

		@Field
		private String user;

		@Field
		private String password;

		@Field
		private boolean encrypted;

		@Field(label = "Image ID")
		private long imageID;

		@Field(label = "Pixels ID")
		private long pixelsID;

		// -- io.scif.omero.OMEROFormat.Metadata methods --

		public String getName() {
			return name;
		}

		public String getServer() {
			return server;
		}

		public int getPort() {
			return port;
		}

		public String getSessionID() {
			return sessionID;
		}

		public String getUser() {
			return user;
		}

		public String getPassword() {
			return password;
		}

		public boolean isEncrypted() {
			return encrypted;
		}

		public long getImageID() {
			return imageID;
		}

		public long getPixelsID() {
			return pixelsID;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public void setServer(final String server) {
			this.server = server;
		}

		public void setPort(final int port) {
			this.port = port;
		}

		public void setSessionID(final String sessionID) {
			this.sessionID = sessionID;
		}

		public void setUser(final String user) {
			this.user = user;
		}

		public void setPassword(final String password) {
			this.password = password;
		}

		public void setEncrypted(final boolean encrypted) {
			this.encrypted = encrypted;
		}

		public void setImageID(final long imageID) {
			this.imageID = imageID;
		}

		public void setPixelsID(final long pixelsID) {
			this.pixelsID = pixelsID;
		}

		public void createSession() {
			if (client != null) endSession();

			// create OMERO client
			if (server != null) {
				client = new omero.client(server, port);
			}
			else {
				client = new omero.client();
			}

			try {
				// initialize the session (i.e., log in)
				try {
					if (sessionID != null) {
						session = client.createSession(sessionID, sessionID);
					}
					else if (user != null && password != null) {
						session = client.createSession(user, password);
					}
					else {
						session = client.createSession();
					}

					if (!encrypted) {
						client = client.createClient(false);
						session = client.getSession();
					}
				}
				catch (final CannotCreateSessionException exc) {
					log().error(exc);
				}
				catch (final PermissionDeniedException exc) {
					log().error(exc);
				}

				session.detachOnDestroy();

				// obtain pixels ID from image ID if necessary
				if (pixelsID == 0 && imageID != 0) {
					final List<Image> images =
						session.getContainerService().getImages("Image",
							Arrays.asList(imageID), null);
					if (images == null || images.isEmpty()) {
						throw new IllegalArgumentException("Invalid image ID: " + imageID);
					}
					pixelsID = images.get(0).getPixels(0).getId().getValue();
				}

				if (pixelsID == 0) {
					throw new IllegalArgumentException("No pixels ID given!");
				}

				// access pixels metadata
				final IPixelsPrx pixelsService = session.getPixelsService();
				pix = pixelsService.retrievePixDescription(pixelsID);

				// configure the raw pixels store
				store = session.createRawPixelsStore();
				store.setPixelsId(pixelsID, false);
			}
			catch (final ServerError err) {
				log().error(err);
			}
		}

		public void endSession() {
			if (client == null) return;
			client.__del__();
			client = null;
			session = null;
			pix = null;
		}

		// -- io.scif.Metadata methods --

		@Override
		public void populateImageMetadata() {
			// extract dimensional axis order and extents
			// TODO: Check whether this logic already exists as a helper somewhere.
			final CalibratedAxis[] axes = new CalibratedAxis[5];
			final int[] axisLengths = new int[5];
//			final String dimOrder = pix.getDimensionOrder().getValue().getValue();
			final String dimOrder = "XYZCT";
			for (int i = 0; i < dimOrder.length(); i++) {
				final char c = dimOrder.charAt(i);
				switch (c) {
					case 'X':
						axes[i] = new DefaultCalibratedAxis(Axes.X);
						final RDouble physSizeX = pix.getPhysicalSizeX();
						if (physSizeX != null) axes[i].setCalibration(physSizeX.getValue());
						axisLengths[i] = pix.getSizeX().getValue();
						break;
					case 'Y':
						axes[i] = new DefaultCalibratedAxis(Axes.Y);
						final RDouble physSizeY = pix.getPhysicalSizeY();
						if (physSizeY != null) axes[i].setCalibration(physSizeY.getValue());
						axisLengths[i] = pix.getSizeY().getValue();
						break;
					case 'Z':
						axes[i] = new DefaultCalibratedAxis(Axes.Z);
						final RDouble physSizeZ = pix.getPhysicalSizeZ();
						if (physSizeZ != null) axes[i].setCalibration(physSizeZ.getValue());
						axisLengths[i] = pix.getSizeZ().getValue();
						break;
					case 'T':
						axes[i] = new DefaultCalibratedAxis(Axes.TIME);
						final RDouble timeInc = pix.getTimeIncrement();
						if (timeInc != null) axes[i].setCalibration(timeInc.getValue());
						axisLengths[i] = pix.getSizeT().getValue();
						break;
					case 'C':
						axes[i] = new DefaultCalibratedAxis(Axes.CHANNEL);
						// final double originC = pix.getWaveStart().getValue();
						final RInt waveInc = pix.getWaveIncrement();
						if (waveInc != null) axes[i].setCalibration(waveInc.getValue());
						axisLengths[i] = pix.getSizeC().getValue();
						break;
					default:
						throw new IllegalArgumentException("Unknown dimension order: " +
							dimOrder);
				}
			}

			// obtain pixel type
			final String pixelTypeString = pix.getPixelsType().getValue().getValue();
			final int pixelType = FormatTools.pixelTypeFromString(pixelTypeString);

			// populate SCIFIO ImageMetadata
			createImageMetadata(1);
			final ImageMetadata imageMeta = get(0);
			imageMeta.setAxes(axes, axisLengths);
			imageMeta.setPixelType(pixelType);
			imageMeta.setMetadataComplete(true);
			imageMeta.setOrderCertain(true);

			// TEMP: Until SCIFIO issue #62 is resolved
			// https://github.com/scifio/scifio/issues/62
			imageMeta.setBitsPerPixel(FormatTools.getBitsPerPixel(pixelType));
		}

	}

	public static class Parser extends AbstractParser<Metadata> {

		@Override
		public void typedParse(final RandomAccessInputStream stream,
			final Metadata meta) throws IOException, FormatException
		{
			// TEMP: Use io.scif.MetadataService instead, once it has been released.
			final HashMap<String, String> map =
				FakeFormat.FakeUtils
					.extractFakeInfo(getContext(), stream.getFileName());

			for (final String key : map.keySet()) {
				final String value = map.get(key);
				if (key.equals("name")) {
					meta.setName(value);
				}
				else if (key.equals("server")) {
					meta.setServer(value);
				}
				else if (key.equals("port")) {
					try {
						meta.setPort(Integer.parseInt(value));
					}
					catch (final NumberFormatException exc) {
						log().warn("Invalid port: " + value, exc);
					}
				}
				else if (key.equals("sessionID")) {
					meta.setSessionID(value);
				}
				else if (key.equals("user")) {
					meta.setUser(value);
				}
				else if (key.equals("password")) {
					meta.setPassword(value);
				}
				else if (key.equals("encrypted")) {
					meta.setEncrypted(Boolean.parseBoolean(value));
				}
				else if (key.equals("imageID")) {
					try {
						meta.setImageID(Long.parseLong(value));
					}
					catch (final NumberFormatException exc) {
						log().warn("Invalid image ID: " + value, exc);
					}
				}
				else if (key.equals("pixelsID")) {
					try {
						meta.setPixelsID(Integer.parseInt(value));
					}
					catch (final NumberFormatException exc) {
						log().warn("Invalid pixels ID: " + value, exc);
					}
				}
			}

			// initialize OMERO client session
			meta.createSession();
		}

	}

	public static class Reader extends ByteArrayReader<Metadata> {

		@Override
		public ByteArrayPlane openPlane(final int imageIndex, final int planeIndex,
			final ByteArrayPlane plane, final int x, final int y, final int w,
			final int h) throws FormatException, IOException
		{
			final int[] zct = FormatTools.getZCTCoords(this, imageIndex, planeIndex);
			try {
				final byte[] tile =
					getMetadata().store.getTile(zct[0], zct[1], zct[2], x, y, w, h);
				plane.setData(tile);
			}
			catch (final ServerError e) {
				throw new FormatException(e);
			}

			return plane;
		}

		@Override
		public void close() {
			final Metadata meta = getMetadata();
			if (meta != null) meta.endSession();
		}

	}

	public static class Writer extends AbstractWriter<Metadata> {

		@Override
		public void savePlane(final int imageIndex, final int planeIndex,
			final Plane plane, final int x, final int y, final int w, final int h)
			throws FormatException, IOException
		{
			final byte[] bytes = plane.getBytes();

			System.out.println(bytes.length);
		}
	}

}
