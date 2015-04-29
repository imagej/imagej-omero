/*
 * #%L
 * OME database I/O package for communicating with OME and OMERO servers.
 * %%
 * Copyright (C) 2013 - 2015 Open Microscopy Environment:
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

import io.scif.Field;

/**
 * Helper class for storing OMERO session credentials.
 * 
 * @author Curtis Rueden
 */
public class OMEROCredentials {

	// -- Fields --

	@Field
	private String server;

	@Field
	private int port = 4064;

	@Field
	private String sessionID;

	@Field
	private String user;

	@Field
	private String password;

	@Field
	private boolean encrypted;

	// -- OMEROCredentials methods --

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

	public void setServer(final String server) {
		this.server = server;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setSessionID(final String sessionID) {
		this.sessionID = sessionID;
		if (sessionID != null) {
			// NB: Drop username & password from memory when we have a session ID.
			user = password = null;
		}
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

}
