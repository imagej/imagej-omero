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
import java.util.Map;
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

	private final boolean encrypted;

	private final String sessionID;

	public OMEROLocation(final String server, final int port, final String user,
		final String password) throws URISyntaxException
	{
		super(new URI(null, user + ":" + password, server, port, null, null, null));
		encrypted = false;
		sessionID = null;
	}

	public OMEROLocation(final String server, final int port, final String user,
		final String password, final boolean encrypted) throws URISyntaxException
	{
		super(new URI(null, user + ":" + password, server, port, null, null, null));
		this.encrypted = encrypted;
		sessionID = null;
	}

	public OMEROLocation(final String server, final int port,
		final String sessionID) throws URISyntaxException
	{
		super(new URI(null, null, server, port, null, null, null));
		encrypted = false;
		this.sessionID = sessionID;
	}

	public OMEROLocation(final Map<String, Object> args)
		throws URISyntaxException
	{
		super(createURI(args));

		// Set encrypted if present
		if (args.containsKey("encrypted")) encrypted = Boolean.parseBoolean(args
			.get("encrypted").toString());
		else encrypted = false;

		// Set sessionID if present
		if (args.containsKey("sessionID")) sessionID = args.get("sessionID")
			.toString();
		else sessionID = null;
	}

	// -- OMEROLocation methods --

	public String getServer() {
		return getURI().getHost();
	}

	public int getPort() {
		return getURI().getPort();
	}

	public String getUser() {
		return getURI().getUserInfo() == null ? null : getURI().getUserInfo().split(
			":")[0];
	}

	public String getPassword() {
		return getURI().getUserInfo() == null ? null : getURI().getUserInfo().split(
			":")[1];
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public String getSessionID() {
		return sessionID;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof OMEROLocation)) return false;
		final OMEROLocation other = (OMEROLocation) obj;
		return Objects.equals(getServer(), other.getServer()) && Objects.equals(
			getUser(), other.getUser()) && Objects.equals(getPassword(), other
				.getPassword()) && getPort() == other.getPort() &&
			encrypted == other.encrypted;
	}

	@Override
	public int hashCode() {
		return getURI().hashCode();
	}

	// -- Helper methods --

	private static URI createURI(final Map<String, Object> args)
		throws NumberFormatException, URISyntaxException
	{
		final String server = (String) args.get("server");
		final int port = Integer.parseInt(args.get("port").toString());
		if (args.containsKey("user") && args.containsKey("password"))
			return new URI(null, args.get("user") + ":" + args.get("password"),
				server, port, null, null, null);
		else if (args.containsKey("sessionID")) return new URI(null, null, server,
			port, null, null, null);
		throw new IllegalArgumentException(
			"Need username and password OR session ID to create OMEROLocation");
	}
}
