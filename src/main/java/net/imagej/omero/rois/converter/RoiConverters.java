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

package net.imagej.omero.rois.converter;

import java.util.List;

import net.imglib2.roi.BoundaryType;

import omero.gateway.model.ShapeData;
import omero.model.Annotation;
import omero.model.Shape;
import omero.model.TagAnnotation;

/**
 * Utility class for working with ROI converters.
 *
 * @author Alison Walter
 */
public class RoiConverters {

	/**
	 * Gets the boundary behavior of this shape. If this shape was previously an
	 * ImageJ mask it will retrieve the boundary behavior from an annotation; if
	 * not, it will default to {@code CLOSED} boundary behavior.
	 *
	 * @param shape OMERO shape which will be checked for a boundaryType
	 *          annotation
	 * @return if {@code shape} was previously converted to an ImageJ mask, then
	 *         its previous boundary behavior is returned otherwise {@code CLOSED}
	 *         is returned.
	 */
	public static <S extends ShapeData> BoundaryType boundaryType(final S shape) {
		final List<Annotation> l = ((Shape) shape.asIObject())
			.linkedAnnotationList();
		for (int i = 0; i < l.size(); i++) {
			if (TagAnnotation.class.isInstance(l.get(i)) && l.get(i).getName()
				.getValue().equals("boundaryType"))
			{
				final TagAnnotation t = (TagAnnotation) l.get(i);
				final String type = t.getTextValue().getValue();
				if (type.toLowerCase().equals("open")) return BoundaryType.OPEN;
				else if (type.toLowerCase().equals("unspecified"))
					return BoundaryType.UNSPECIFIED;
				else return BoundaryType.CLOSED;
			}
		}
		// If no such tag found, use Closed
		return BoundaryType.CLOSED;
	}
}
