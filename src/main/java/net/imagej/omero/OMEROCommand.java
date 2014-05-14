/*
 * #%L
 * Server- and client-side communication between ImageJ and OMERO.
 * %%
 * Copyright (C) 2013 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
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

import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;

/** An ImageJ command for interacting with an OMERO server. */
public abstract class OMEROCommand extends ContextCommand {

	@Parameter
	private String server = "localhost";

	@Parameter
	private int port = 4064;

	@Parameter
	private String user;

	// TEMP: Use TextWidget.PASSWORD_STYLE constant once it is released.
	@Parameter(style = "password")
	private String password;

	// -- OMEROCommand methods --

	public String getServer() {
		return server;
	}

	public int getPort() {
		return port;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

}
