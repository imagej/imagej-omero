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

import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
	public RawPixelsStorePrx createPixels() throws ServerError {
		// create a new Image which will house the written pixels
		final ImageData newImage = createImage();
		if (newImage == null) return null;

		// configure the raw pixels store
		final RawPixelsStorePrx store = session.createRawPixelsStore();
		store.setPixelsId(newImage.getDefaultPixels().getId(), false);

		return store;
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

	private ImageData createImage() throws ServerError {
		// create a new Image
		final int sizeX = meta.getSizeX();
		final int sizeY = meta.getSizeY();
		final int sizeZ = meta.getSizeZ();
		final int sizeT = meta.getSizeT();
		final PixelsType pixelsType = getPixelsType();
		if (pixelsType == null) return null;
		final String name = meta.getName();
		final String description = meta.getName();
		final RLong id =
			session.getPixelsService().createImage(sizeX, sizeY, sizeZ, sizeT,
				Arrays.asList(0), pixelsType, name, description);
		if (id == null) return null;

		// retrieve the newly created Image
		final List<Image> results =
			session.getContainerService().getImages(Image.class.getName(),
				Arrays.asList(id.getValue()), null);
		return new ImageData(results.get(0));
	}

	// -- Closeable methods --

	@Override
	public void close() {
		if (client != null) client.__del__();
		client = null;
		session = null;
	}

	// -- Helper methods --

	private PixelsType getPixelsType() throws ServerError {
		final List<IObject> list =
			session.getPixelsService().getAllEnumerations(PixelsType.class.getName());
		final Iterator<IObject> iter = list.iterator();
		final String pixelType = meta.getPixelType();
		while (iter.hasNext()) {
			final PixelsType type = (PixelsType) iter.next();
			final String value = type.getValue().getValue();
			if (value.equals(pixelType)) return type;
		}
		return null;
	}

}
