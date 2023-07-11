/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2023 Open Microscopy Environment:
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

package net.imagej.omero.roi.polyshape;

import java.awt.geom.Point2D;
import java.util.List;

import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.WritablePolygon2D;

import omero.gateway.model.PolygonData;

/**
 * A {@link Polygon2D} which wraps an OMERO polygon Roi.
 *
 * @author Alison Walter
 */
public interface OMEROPolygon extends OMEROPolyshape<PolygonData>,
	WritablePolygon2D
{

	@Override
	default List<Point2D.Double> getPoints() {
		return getShape().getPoints();
	}

	@Override
	default void setPoints(final List<Point2D.Double> points) {
		getShape().setPoints(points);
	}
}
