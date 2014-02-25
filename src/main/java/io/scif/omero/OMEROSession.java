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
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.util.FormatTools;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.imglib2.meta.Axes;
import omero.RLong;
import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.model.IObject;
import omero.model.Image;
import omero.model.Pixels;
import omero.model.PixelsType;
import pojos.ImageData;

/**
 * Helper class for managing OMERO client sessions.
 * 
 * @author Curtis Rueden
 */
public class OMEROSession implements Closeable {

	// -- Fields --

	private OMEROFormat.Metadata meta;
	private omero.client client;
	private ServiceFactoryPrx session;

	// -- Constructors --

	public OMEROSession(final OMEROFormat.Metadata meta) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException
	{
		this(meta, null);
	}

	public OMEROSession(final OMEROFormat.Metadata meta, final omero.client c)
		throws ServerError, PermissionDeniedException, CannotCreateSessionException
	{
		this.meta = meta;

		// initialize the client
		if (c == null) {
			if (meta.getServer() != null) {
				client = new omero.client(meta.getServer(), meta.getPort());
			}
			else client = new omero.client();
		}
		else client = c;

		// initialize the session (i.e., log in)
		final String sessionID = meta.getSessionID();
		if (sessionID != null) {
			session = client.createSession(sessionID, sessionID);
		}
		else if (meta.getUser() != null && meta.getPassword() != null) {
			session = client.createSession(meta.getUser(), meta.getPassword());
		}
		else {
			session = client.createSession();
		}

		if (!meta.isEncrypted()) {
			client = client.createClient(false);
			session = client.getSession();
		}

		session.detachOnDestroy();
	}

	// -- OMEROSession methods --

	public omero.client getClient() {
		return client;
	}
	
	public ServiceFactoryPrx getSession() {
		return session;
	}

	/** Gets an OMERO {@code Pixels} descriptor */
	public Pixels getPixelsInfo() throws ServerError {
		return session.getPixelsService().retrievePixDescription(getPixelsID());
	}

	/** Obtains a raw pixels store for reading from the configured pixels ID. */
	public RawPixelsStorePrx openPixels() throws ServerError {
		final RawPixelsStorePrx store = session.createRawPixelsStore();
		store.setPixelsId(getPixelsID(), false);
		return store;
	}

	/** Obtains a raw pixels store for writing to a newly created pixels ID. */
	public RawPixelsStorePrx createPixels() throws ServerError, FormatException {
		// create a new Image which will house the written pixels
		final ImageData newImage = createImage();

		// configure the raw pixels store
		final RawPixelsStorePrx store = session.createRawPixelsStore();
		store.setPixelsId(newImage.getDefaultPixels().getId(), false);

		return store;
	}

	// -- Closeable methods --

	@Override
	public void close() {
		if (client != null) client.__del__();
		client = null;
		session = null;
	}

	// -- Helper methods --

	private long getPixelsID() throws ServerError {
		final long pixelsID = meta.getPixelsID();
		if (pixelsID != 0) return pixelsID;

		// obtain pixels ID from image ID
		final long imageID = meta.getImageID();
		if (imageID == 0) return 0;
		final List<Image> images =
				session.getContainerService().getImages("Image",
					Arrays.asList(imageID), null);
		if (images == null || images.isEmpty()) {
			throw new IllegalArgumentException("Invalid image ID: " + imageID);
		}
		return images.get(0).getPixels(0).getId().getValue();
	}

	private ImageData createImage() throws ServerError, FormatException {
		// create a new Image
		final ImageMetadata imageMeta = meta.get(0);
		// FIXME: Check before casting.
		final int xLen = (int) imageMeta.getAxisLength(Axes.X);
		final int yLen = (int) imageMeta.getAxisLength(Axes.Y);
		final int zLen = (int) imageMeta.getAxisLength(Axes.Z);
		final int tLen = (int) imageMeta.getAxisLength(Axes.TIME);
		final int sizeX = xLen == 0 ? 1 : xLen;
		final int sizeY = yLen == 0 ? 1 : yLen;
		final int sizeZ = zLen == 0 ? 1 : zLen;
		final int sizeT = tLen == 0 ? 1 : tLen;
		final int pixelType = imageMeta.getPixelType();
		final PixelsType pixelsType = getPixelsType(pixelType);
		final String name = meta.getName();
		final String description = meta.getName();
		final RLong id =
			session.getPixelsService().createImage(sizeX, sizeY, sizeZ, sizeT,
				Arrays.asList(0), pixelsType, name, description);
		if (id == null) throw new FormatException("Cannot create image");

		// retrieve the newly created Image
		final List<Image> results =
			session.getContainerService().getImages(Image.class.getName(),
				Arrays.asList(id.getValue()), null);
		return new ImageData(results.get(0));
	}

	private PixelsType getPixelsType(int pixelType) throws ServerError,
		FormatException
	{
		return getPixelsType(FormatTools.getPixelTypeString(pixelType));
	}

	private PixelsType getPixelsType(final String pixelType) throws ServerError,
		FormatException
	{
		final List<IObject> list =
			session.getPixelsService().getAllEnumerations(PixelsType.class.getName());
		final Iterator<IObject> iter = list.iterator();
		while (iter.hasNext()) {
			final PixelsType type = (PixelsType) iter.next();
			final String value = type.getValue().getValue();
			if (value.equals(pixelType)) return type;
		}
		throw new FormatException("Invalid pixel type: " + pixelType);
	}

}
