/*
 * #%L
 * SciJava Common shared library for SciJava software.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
