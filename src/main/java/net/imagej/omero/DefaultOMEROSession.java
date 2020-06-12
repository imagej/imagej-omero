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
import io.scif.util.FormatTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.RLong;
import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.log.Logger;
import omero.log.SimpleLogger;
import omero.model.IObject;
import omero.model.Image;
import omero.model.Pixels;
import omero.model.PixelsType;

/**
 * Helper class for managing OMERO client sessions.
 *
 * @author Curtis Rueden
 */
public class DefaultOMEROSession implements OMEROSession {

	// -- Fields --

	private omero.client client;
	private ServiceFactoryPrx session;
	private ExperimenterData experimenter;
	private Gateway gateway;
	private SecurityContext ctx;
	private final OMEROService omeroService;

	// -- Constructors --

	public DefaultOMEROSession(final OMEROLocation credentials,
		final OMEROService omeroService) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException
	{
		this(credentials, null, omeroService);
	}

	public DefaultOMEROSession(final OMEROLocation credentials,
		final omero.client c, final OMEROService omeroService) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException
	{
		if ((credentials.getUser() == null || credentials.getPassword() == null) &&
			credentials.getSessionID() == null) throw new IllegalArgumentException(
				"Cannot create OMEROSession: OMEROLocation must specify username and " +
					"password OR session ID");

		// initialize the client
		boolean close = false;
		if (c == null) {
			final String server = credentials.getServer();
			if (server != null) {
				client = new omero.client(server, credentials.getPort());
			}
			else client = new omero.client();
		}
		else {
			client = c;
			close = true;
		}

		if (credentials.getUser() != null && credentials.getPassword() != null) {
			final String user = credentials.getUser();
			final String password = credentials.getPassword();
			session = client.createSession(user, password);
		}
		else {
			session = client.joinSession(credentials.getSessionID());
		}

		setGateway();
		setExperimenter(credentials);
		setSecurityContext(credentials);

		// Until imagej-omero #30 is resolved; see:
		// https://github.com/imagej/imagej-omero/issues/30
//		if (client.isSecure() && !credentials.isEncrypted()) {
//			client = client.createClient(false);
//			session = client.getSession();
//		}

		session.detachOnDestroy();
		this.omeroService = omeroService;
	}

	// -- OMEROSession methods --

	@Override
	public omero.client getClient() {
		return client;
	}

	@Override
	public ServiceFactoryPrx getSession() {
		return session;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return ctx;
	}

	@Override
	public ExperimenterData getExperimenter() {
		return experimenter;
	}

	@Override
	public Gateway getGateway() {
		return gateway;
	}

	@Override
	public String getSessionID() {
		return client.getSessionId();
	}

	@Override
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
		pixels = session.getPixelsService().retrievePixDescription(pixelsID);
		meta.setPixels(pixels);

		return pixels;
	}

	@Override
	public Image loadImage(final OMEROFormat.Metadata meta) throws ServerError {
		// return cached Image if available
		Image image = meta.getImage();
		if (image != null) return image;

		final long imageID = meta.getImageID();
		if (imageID == 0) throw new IllegalArgumentException("Image ID is unset");

		// load the Image from the remote server
		final List<Long> ids = Arrays.asList(imageID);
		final List<Image> images = session.getContainerService().getImages("Image",
			ids, null);
		if (images == null || images.isEmpty()) {
			throw new IllegalArgumentException("Invalid image ID: " + imageID);
		}
		image = images.get(0);
		meta.setImage(image);

		return image;
	}

	@Override
	public long loadPixelsID(final OMEROFormat.Metadata meta) throws ServerError {
		// return cached pixels ID if available
		long pixelsID = meta.getPixelsID();
		if (pixelsID != 0) return pixelsID;

		// obtain pixels ID from image ID
		pixelsID = loadImage(meta).getPixels(0).getId().getValue();
		meta.setPixelsID(pixelsID);

		return pixelsID;
	}

	@Override
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

	@Override
	public RawPixelsStorePrx openPixels(final OMEROFormat.Metadata meta)
		throws ServerError
	{
		final RawPixelsStorePrx store = session.createRawPixelsStore();
		store.setPixelsId(loadPixelsID(meta), false);
		return store;
	}

	@Override
	public RawPixelsStorePrx createPixels(final OMEROFormat.Metadata meta)
		throws ServerError, FormatException
	{
		// create a new Image which will house the written pixels
		final ImageData newImage = createImage(meta);
		final long imageID = newImage.getId();
		meta.setImageID(imageID);

		// configure the raw pixels store
		final RawPixelsStorePrx store = session.createRawPixelsStore();
		final long pixelsID = newImage.getDefaultPixels().getId();
		store.setPixelsId(pixelsID, false);
		meta.setPixelsID(pixelsID);

		return store;
	}

	// -- Closeable methods --

	@Override
	public void close() {
		if (client != null) client.__del__();
		client = null;
		session = null;
		if (gateway != null) gateway.disconnect();
		omeroService.removeSession(this);
	}

	// -- Helper methods --

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
		final RLong id = session.getPixelsService().createImage(sizeX, sizeY, sizeZ,
			sizeT, channelList, pixelsType, name, description);
		if (id == null) throw new FormatException("Cannot create image");

		// retrieve the newly created Image
		final List<Image> results = session.getContainerService().getImages(
			Image.class.getName(), Arrays.asList(id.getValue()), null);
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
		final List<IObject> list = session.getPixelsService().getAllEnumerations(
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
	 * Creates a SecurityConext which is linked to the group the user belongs to.
	 */
	private void setSecurityContext(final OMEROLocation credentials)
		throws ServerError
	{
		if (experimenter == null) setExperimenter(credentials);
		ctx = new SecurityContext(experimenter.getGroupId());
	}

	/**
	 * Attempts to connect to the gateway using the given credentials. If it can
	 * successfully connect, then it sets experimenter.
	 */
	private void setExperimenter(final OMEROLocation credentials)
		throws ServerError
	{
		final LoginCredentials cred;
		if (credentials.getSessionID() != null) cred = new LoginCredentials(
			credentials.getSessionID(), null, credentials.getServer(), credentials
				.getPort());
		else cred = new LoginCredentials(credentials.getUser(), credentials
			.getPassword(), credentials.getServer(), credentials.getPort());

		if (gateway == null) setGateway();

		try {
			experimenter = gateway.connect(cred);
		}
		catch (final DSOutOfServiceException exc) {
			final ServerError err = new ServerError();
			err.initCause(exc);
			throw err;
		}
	}

	/**
	 * Creates a new gateway for the session.
	 */
	private void setGateway() {
		final Logger simpleLogger = new SimpleLogger();
		gateway = new Gateway(simpleLogger);
	}

}
