/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2023 Open Microscopy Environment:
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.scijava.io.location.Location;
import org.scijava.io.location.URILocation;

/**
 * {@link Location} pointing at an OMERO server.
 *
 * @author Curtis Rueden
 * @author Alison Walter
 */
public class OMEROLocation extends URILocation {

	private static URI uri(final String host, final int port, final String path) {
		try {
			return new URI("omero", null, host, port, path, null, null);
		}
		catch (final URISyntaxException exc) {
			throw new IllegalArgumentException(exc);
		}
	}

	public OMEROLocation(final OMEROServer server, final String path) {
		super(uri(server.host, server.port, path));
	}

	public OMEROLocation(final OMEROServer server, final long imageID) {
		// TODO: Determine if OMERO already has some conventions
		// surrounding URI-style expression of its resources.
		// And reuse those same ones if so!
		super(uri(server.host, server.port, "/image/" + imageID));
	}

	public OMEROLocation(final URI uri) {
		super(validateURI(uri));
	}

	// -- OMEROLocation methods --

	public OMEROServer getServer() {
		return new OMEROServer(getHost(), getPort());
	}

	public String getHost() {
		return getURI().getHost();
	}

	public int getPort() {
		return getURI().getPort();
	}

	public String getPath() {
		return getURI().getPath();
	}

	public long getImageID() {
		String path = getPath();
		return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof OMEROLocation)) return false;
		final OMEROLocation that = (OMEROLocation) obj;
		return Objects.equals(this.getURI(), that.getURI());
	}

	@Override
	public int hashCode() {
		// TODO: Push upstream to URILocation base class!
		// Why not? Answer: because it needs to go hand in hand with equals,
		// but pushing equals upstream is maybe too tricky...
		return getURI().hashCode();
	}

	// -- Helper methods --

	private static URI validateURI(final URI uri) {
		if (!"omero".equals(uri.getScheme())) {
			throw new IllegalArgumentException("Not an omero URI: " + uri);
		}
		return uri;
	}
}
