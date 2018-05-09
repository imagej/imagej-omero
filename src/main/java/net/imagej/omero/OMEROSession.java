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

import java.io.Closeable;

import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.model.ExperimenterData;
import omero.model.Image;
import omero.model.Pixels;

public interface OMEROSession extends Closeable {

	/** Gets the {@code omero.client} associated with this OMEROSession */
	omero.client getClient();

	/** Gets the {@code ServiceFactoryPrx} associated with this OMEROSession */
	ServiceFactoryPrx getSession();

	/** Gets the {@code SecurityContext} associated with this OMEROSession */
	SecurityContext getSecurityContext();

	/** Gets the {@code Experimenter} associated with this OMEROSession */
	ExperimenterData getExperimenter();

	/** Gets the {@code Gateway} associated with this OMEROSession */
	Gateway getGateway();

	/** Gets the session ID associated with this OMEROSession */
	String getSessionID();

	/** Gets an OMERO {@code Pixels} descriptor, loading remotely as needed. */
	Pixels loadPixels(OMEROFormat.Metadata meta) throws ServerError;

	/** Gets an OMERO {@code Image} descriptor, loading remotely as needed. */
	Image loadImage(OMEROFormat.Metadata meta) throws ServerError;

	/** Gets the metadata's associated pixels ID, loading remotely as needed. */
	long loadPixelsID(OMEROFormat.Metadata meta) throws ServerError;

	/** Gets the metadata's associated image name, loading remotely as needed. */
	String loadImageName(OMEROFormat.Metadata meta) throws ServerError;

	/**
	 * Obtains a raw pixels store for reading from the pixels associated with the
	 * given metadata.
	 */
	RawPixelsStorePrx openPixels(OMEROFormat.Metadata meta) throws ServerError;

	/**
	 * Obtains a raw pixels store for writing to a new image which will be
	 * associated with the given metadata.
	 */
	RawPixelsStorePrx createPixels(OMEROFormat.Metadata meta) throws ServerError,
		FormatException;

	// -- Closeable methods --

	@Override
	void close();

}
