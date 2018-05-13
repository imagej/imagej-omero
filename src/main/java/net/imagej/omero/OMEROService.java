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

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.roi.ROITree;
import net.imagej.table.Table;

import org.scijava.module.ModuleItem;
import org.scijava.util.TreeNode;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ROIData;
import omero.gateway.model.TableData;
import omero.model.TagAnnotationI;

/**
 * Interface for ImageJ services that manage OMERO data conversion.
 *
 * @author Curtis Rueden
 */
public interface OMEROService extends ImageJService {

	/** Converts an ImageJ module parameter to an OMERO job parameter. */
	omero.grid.Param getJobParam(ModuleItem<?> item);

	/** Creates an OMERO parameter prototype for the given Java class. */
	omero.RType prototype(Class<?> type);

	/**
	 * Converts an ImageJ parameter value to an OMERO parameter value.
	 * <p>
	 * This method only handles basic types, not image types; see
	 * {@link #toOMERO(omero.client, Object)} for details.
	 * </p>
	 */
	omero.RType toOMERO(Object value);

	/**
	 * Converts an ImageJ parameter value to an OMERO parameter value.
	 * <p>
	 * If the given object is an image type (i.e., {@link Dataset},
	 * {@link DatasetView} or {@link ImageDisplay}) then the {@link #uploadImage}
	 * method will be used transparently to convert the object into an OMERO image
	 * ID.
	 * </p>
	 * <p>
	 * In the case of {@link Table}s, it will be converted to a {@link TableData}.
	 * </p>
	 *
	 * @throws DSAccessException
	 * @throws DSOutOfServiceException
	 * @throws ExecutionException
	 * @throws CannotCreateSessionException
	 * @throws PermissionDeniedException
	 */
	Object toOMERO(omero.client client, Object value) throws omero.ServerError,
		IOException, PermissionDeniedException, CannotCreateSessionException,
		ExecutionException, DSOutOfServiceException, DSAccessException;

	/**
	 * Converts an OMERO parameter value to an ImageJ value of the given type.
	 * <p>
	 * If the requested type is an image type (i.e., {@link Dataset},
	 * {@link DatasetView} or {@link ImageDisplay}) then the
	 * {@link #downloadImage} method will be used transparently to convert the
	 * OMERO image ID into such an object.
	 */
	Object toImageJ(omero.client client, omero.RType value, Class<?> type)
		throws omero.ServerError, IOException, PermissionDeniedException,
		CannotCreateSessionException, SecurityException, DSOutOfServiceException,
		ExecutionException, DSAccessException;

	/**
	 * Downloads the image with the given image ID from OMERO, storing the result
	 * into a new ImageJ {@link Dataset}.
	 */
	Dataset downloadImage(omero.client client, long imageID)
		throws omero.ServerError, IOException;

	/**
	 * Uploads the given ImageJ {@link Dataset}'s image to OMERO, returning the
	 * new image ID on the OMERO server.
	 */
	long uploadImage(omero.client client, Dataset dataset)
		throws omero.ServerError, IOException;

	/**
	 * Uploads an ImageJ table to OMERO, returning the new table ID on the OMERO
	 * server. Tables must be attached to a DataObject, thus the given image ID
	 * must be valid or this method will throw an exception.
	 */
	long uploadTable(OMEROLocation credentials, String name,
		Table<?, ?> imageJTable, final long imageID) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException;

	/**
	 * Converts the given ImageJ table to an OMERO table, but does not save the
	 * table to the server.
	 */
	TableData convertOMEROTable(Table<?, ?> imageJTable);

	/**
	 * Downloads the table with the given ID from OMERO, storing the result into a
	 * new ImageJ {@link Table}.
	 */
	Table<?, ?> downloadTable(OMEROLocation credentials, long tableID)
		throws ServerError, PermissionDeniedException, CannotCreateSessionException,
		ExecutionException, DSOutOfServiceException, DSAccessException;

	/**
	 * Downloads the ROIs associated with the given {@code imageID} from OMERO,
	 * and returns them as a {@link ROITree}.
	 */
	ROITree downloadROIs(OMEROLocation credentials, long imageID)
		throws ServerError, PermissionDeniedException, CannotCreateSessionException,
		ExecutionException, DSOutOfServiceException, DSAccessException;

	/**
	 * Downloads the {@link ROIData} with the given {@code roiID} from OMERO, and
	 * returns it as a {@link ROITree}.
	 */
	ROITree downloadROI(final OMEROLocation credentials, final long roiID)
		throws DSOutOfServiceException, DSAccessException, ExecutionException;

	/**
	 * Converts the given {@link TreeNode} to OMERO ROI(s), uploads them to the
	 * OMEROServer, and attaches them to the image with the specified ID. All ROIs
	 * are uploaded as new Objects, regardless of their origin.
	 */
	long[] uploadROIs(OMEROLocation credentials, TreeNode<?> ijROIs, long imageID)
		throws ExecutionException, DSOutOfServiceException, DSAccessException;

	/**
	 * Returns an {@link OMEROSession} using the given {@link OMEROLocation}. If a
	 * session with this location already exists it is returned, if not a new one
	 * is created.
	 *
	 * @param location OMEROLocation
	 * @return OMEROSession
	 */
	OMEROSession session(OMEROLocation location);

	/**
	 * Returns the {@link OMEROSession} related to the running thread.
	 *
	 * @return the OMEROSession with the current thread
	 */
	OMEROSession session();

	/**
	 * Creates an OMEROSession. This <strong>does not</strong> cache the session
	 * nor does it associate this session with the thread.
	 *
	 * @param location OMEROLocation to be used to create the session
	 * @return a new OMEROSession
	 */
	OMEROSession createSession(OMEROLocation location);

	/**
	 * Remove the specified {@link OMEROSession} for the cache of stored sessions.
	 *
	 * @param session The session to be removed
	 */
	void removeSession(OMEROSession session);

	/**
	 * Returns a {@link TagAnnotationI} with the given description and text value.
	 * If no such {@link TagAnnotationI} is found on the server, one is created,
	 * saved to the server, and returned.
	 *
	 * @param description the description of the tag
	 * @param value the text value of the tag
	 * @param location credentials for connecting to the server, if these isn't a
	 *          current {@link OMEROSession} for these credentials one is created
	 * @return a {@link TagAnnotationI} with the given description and text value
	 */
	TagAnnotationI getAnnotation(String description, String value,
		OMEROLocation location) throws ExecutionException, ServerError,
		DSOutOfServiceException, DSAccessException;

	/**
	 * Returns a {@link TagAnnotationI} with the given description and text value.
	 * If no such {@link TagAnnotationI} is found on the server, one is created
	 * and returned. The credentials used for this query are those of the
	 * {@link OMEROSession} related with the current running thread.
	 *
	 * @param description the description of the tag
	 * @param value the text value of the tag
	 * @return a {@link TagAnnotationI} with the given description and text value
	 */
	TagAnnotationI getAnnotation(String description, String value)
		throws ExecutionException, ServerError, DSOutOfServiceException,
		DSAccessException;

}
