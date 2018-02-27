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
import net.imagej.table.Table;

import org.scijava.module.ModuleItem;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.TableData;

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
	 * @throws DSAccessException
	 * @throws DSOutOfServiceException
	 * @throws ExecutionException
	 * @throws CannotCreateSessionException
	 * @throws PermissionDeniedException
	 */
	Object toOMERO(omero.client client, Object value)
		throws omero.ServerError, IOException, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException;

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
	long uploadTable(OMEROCredentials credentials, String name,
		Table<?, ?> imageJTable, final long imageID) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException,
		ExecutionException, DSOutOfServiceException, DSAccessException;

	/** Converts the given ImageJ table to an OMERO table, but does not save the
	 * table to the server.
	 */
	TableData convertOMEROTable(Table<?, ?> imageJTable);

	/**
	 * Downloads the table with the given ID from OMERO, storing the result into a
	 * new ImageJ {@link Table}.
	 */
	Table<?, ?> downloadTable(OMEROCredentials credentials, long tableID)
		throws ServerError, PermissionDeniedException, CannotCreateSessionException,
		ExecutionException, DSOutOfServiceException, DSAccessException;

}
