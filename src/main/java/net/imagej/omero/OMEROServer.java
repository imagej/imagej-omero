/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2022 Open Microscopy Environment:
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

import java.util.Objects;

/**
 * Helper class for storing an OMERO server address (host + port).
 *
 * @author Curtis Rueden
 */
public class OMEROServer {

	public final String host;
	public final int port;

	public OMEROServer(final String host, final boolean encrypted) {
		this(host, encrypted ? 4064 : 4063);
	}

	public OMEROServer(final String host, final int port) {
		this.host = host.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","");
		this.port = port;
	}

	// -- Object methods --

	@Override
	public int hashCode() {
		return host.hashCode() ^ port;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof OMEROServer)) return false;
		final OMEROServer that = (OMEROServer) o;
		return Objects.equals(this.host, that.host) && this.port == that.port;
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}
}
