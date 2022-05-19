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

/**
 * Helper class for storing OMERO session credentials.
 *
 * @author Curtis Rueden
 */
public class OMEROCredentials {

	// -- Fields --

	private String sessionID;

	private String user;

	private String password;

	private boolean encrypted;

	// -- OMEROCredentials methods --

	public OMEROCredentials(final String user, final String password) {
		this.user = user;
		this.password = password;
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

	public void setSessionID(final String sessionID) {
		// TODO: This is not even used right now. Should we get rid of it?
		// Or just have an alternate constructor? What if a session times out
		// on the server side -- does the session ID become invalid? If so,
		// we probably really want to keep the user/pass to relogin. But what
		// about security concerns keeping the pass in a String field persistently?
		this.sessionID = sessionID;
		if (sessionID != null) {
			// NB: Drop username & password from memory when we have a session ID.
			user = password = null;
		}
	}

	public void setEncrypted(final boolean encrypted) {
		this.encrypted = encrypted;
	}

	/**
	 * Checks that some credentials were specified.
	 *
	 * @throws IllegalArgumentException if no session ID was given, and no
	 *           username+password was given either.
	 */
	public void validate() {
		if (sessionID == null && (user == null || password == null)) {
			throw new IllegalArgumentException("Invalid credentials: " +
				"must specify either session ID OR username+password");
		}
	}
}
