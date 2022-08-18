/*
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

import io.scif.services.DatasetIOService;

import net.imagej.ImageJService;
import net.imagej.display.ImageDisplayService;
import net.imagej.omero.roi.ROICache;

import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;

import omero.gateway.model.ROIData;

/**
 * SciJava service interface for managing OMERO server access.
 *
 * @author Curtis Rueden
 */
public interface OMEROService extends ImageJService {

	/**
	 * @see #session(OMEROServer, OMEROCredentials)
	 */
	OMEROSession session(OMEROServer server) throws OMEROException;

	/**
	 * Returns an {@link OMEROSession} using the given server and access
	 * credentials. If a session with for this server already exists it is
	 * returned, or if not a new one is created.
	 *
	 * @param server OMEROServer for which a session is desired
	 * @param credentials Credentials for use if a new server session must be
	 *          created
	 * @return A session object for working with OMERO
	 * @throws OMEROException if there is no existing OMERO session and a new one
	 *           cannot be established.
	 * @throws IllegalStateException if no active session for that server exists
	 *           and credentials were not provided.
	 */
	OMEROSession session(OMEROServer server, OMEROCredentials credentials)
		throws OMEROException;

	/**
	 * Creates an OMEROSession. This <strong>does not</strong> cache the session.
	 *
	 * @param server OMEROServer for which a session is desired
	 * @param credentials Credentials for obtaining access to the server
	 * @return A new session object for working with OMERO
	 * @throws OMEROException if the OMERO session cannot be established.
	 */
	OMEROSession createSession(OMEROServer server, OMEROCredentials credentials)
		throws OMEROException;

	/**
	 * Get the current {@link OMEROSession} for this thread
	 *
	 * @see #pushSession(OMEROSession)
	 * @return The top OMEROSession of the current thread's session stack.
	 */
	OMEROSession session();

	/**
	 * Push an {@link OMEROSession} onto this thread's session stack.
	 *
	 * @param omeroSession OMEROSession to push
	 */
	void pushSession(OMEROSession omeroSession);

	/**
	 * Removes the most recent session pushed with
	 * {@link #pushSession(OMEROSession)} from this thread's session stack.
	 */
	void popSession();

	/**
	 * Gets this application context's {@link ConvertService}.
	 *
	 * @return The {@link ConvertService} of this application context.
	 */
	ConvertService convert();

	/**
	 * Gets this application context's {@link ImageDisplayService}.
	 *
	 * @return The {@link ImageDisplayService} of this application context.
	 */
	ImageDisplayService imageDisplay();

	/**
	 * Gets this application context's {@link DatasetIOService}.
	 *
	 * @return The {@link DatasetIOService} of this application context.
	 */
	DatasetIOService datasetIO();

	/**
	 * Gets this application context's {@link ObjectService}.
	 *
	 * @return The {@link ObjectService} of this application context.
	 */
	ObjectService object();

	/**
	 * Gets this application context's {@link DisplayService}.
	 *
	 * @return The {@link DisplayService} of this application context.
	 */
	DisplayService display();

	/**
	 * @return The {@link ROICache} for this session
	 */
	ROICache roiCache();

	/**
	 * Create a mapping from ImageJ to OMERO ROI types
	 *
	 * @param roi ImageJ ROI
	 * @param shape OMERO ROI
	 */
	void addROIMapping(Object roi, ROIData shape);

	/**
	 * @param key ImageJ ROI
	 * @return The mapped OMERO ROI data for the given ImageJ ROI
	 */
	ROIData getROIMapping(Object key);

	/**
	 * @param key ImageJ ROI to remove from ROI map
	 */
	void removeROIMapping(Object key);

	/**
	 * Converts an ImageJ module parameter to an OMERO job parameter.
	 *
	 * @param item Target ImageJ module parameter
	 * @return OMERO equivalent for given module parameter
	 */
	omero.grid.Param getJobParam(ModuleItem<?> item);

	/**
	 * Creates an OMERO parameter prototype for the given Java class.
	 *
	 * @param type Desired Java type
	 * @return OMERO equivalent for the given Java type
	 */
	omero.RType prototype(Class<?> type);
}
