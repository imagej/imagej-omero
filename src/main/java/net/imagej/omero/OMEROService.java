/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2016 Open Microscopy Environment:
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

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;

import org.scijava.module.ModuleItem;
import org.scijava.service.Service;

/**
 * Interface for ImageJ services that manage OMERO data conversion.
 * 
 * @author Curtis Rueden
 */
public interface OMEROService extends Service {

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
	 */
	omero.RType toOMERO(omero.client client, Object value)
		throws omero.ServerError, IOException;

	/**
	 * Converts an OMERO parameter value to an ImageJ value of the given type.
	 * <p>
	 * If the requested type is an image type (i.e., {@link Dataset},
	 * {@link DatasetView} or {@link ImageDisplay}) then the
	 * {@link #downloadImage} method will be used transparently to convert the
	 * OMERO image ID into such an object.
	 */
	Object toImageJ(omero.client client, omero.RType value, Class<?> type)
		throws omero.ServerError, IOException;

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

}
