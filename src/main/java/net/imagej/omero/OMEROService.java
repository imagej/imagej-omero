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

import net.imagej.ImageJService;

import org.scijava.module.ModuleItem;

/**
 * SciJava service interface for managing OMERO server access.
 *
 * @author Curtis Rueden
 */
public interface OMEROService extends ImageJService {

	/**
	 * Returns an {@link OMEROSession} using the given server and access
	 * credentials. If a session with for this server already exists it is
	 * returned, or if not a new one is created.
	 *
	 * @param server OMEROServer for which a session is desired
	 * @param credentials Credentials for obtaining access to the server
	 * @return A session object for working with OMERO
	 * @throw OMEROException if there is no existing OMERO session and a new one
	 *        cannot be established.
	 */
	OMEROSession session(OMEROServer server, OMEROCredentials credentials)
		throws OMEROException;

	/**
	 * Returns the already-existing {@link OMEROSession} for the given server.
	 *
	 * @param server OMEROServer for which a session is desired
	 * @return A session object for working with OMERO
	 * @throw IllegalStateException if no active session for that server exists.
	 */
	OMEROSession session(OMEROServer server);

	/**
	 * Creates an OMEROSession. This <strong>does not</strong> cache the session
	 * nor does it associate this session with the thread.
	 *
	 * @param server OMEROServer for which a session is desired
	 * @param credentials Credentials for obtaining access to the server
	 * @return A new session object for working with OMERO
	 * @throw OMEROException if the OMERO session cannot be established.
	 */
	OMEROSession createSession(OMEROServer server, OMEROCredentials credentials)
		throws OMEROException;

	/** Converts an ImageJ module parameter to an OMERO job parameter. */
	omero.grid.Param getJobParam(ModuleItem<?> item);

	/** Creates an OMERO parameter prototype for the given Java class. */
	omero.RType prototype(Class<?> type);
}
