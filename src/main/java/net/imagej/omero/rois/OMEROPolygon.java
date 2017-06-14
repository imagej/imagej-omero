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

package net.imagej.omero.rois;

import java.awt.geom.Point2D;
import java.util.List;

import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.Polygon2D;

import omero.gateway.model.PolygonData;

/**
 * A {@link Polygon2D} which wraps an OMERO polygon Roi.
 *
 * @author Alison Walter
 */
public interface OMEROPolygon extends OMEROShape<PolygonData>,
	Polygon2D<RealPoint>
{

	@Override
	default int numVertices() {
		return getShape().getPoints().size();
	}

	@Override
	default void addVertex(final int index, final double[] vertex) {
		getShape().getPoints().add(index, new Point2D.Double(vertex[0], vertex[1]));
	}

	@Override
	default void removeVertex(final int index) {
		getShape().getPoints().remove(index);
	}

	@Override
	default double realMin(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		final List<Point2D.Double> pts = getShape().getPoints();
		double min = d == 0 ? pts.get(0).getX() : pts.get(0).getY();
		for (int i = 1; i < pts.size(); i++) {
			if (d == 0) {
				if (pts.get(0).getX() < min) min = pts.get(0).getX();
			}
			else {
				if (pts.get(0).getY() < min) min = pts.get(0).getY();
			}
		}
		return min;
	}

	@Override
	default double realMax(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		final List<Point2D.Double> pts = getShape().getPoints();
		double max = d == 0 ? pts.get(0).getX() : pts.get(0).getY();
		for (int i = 1; i < pts.size(); i++) {
			if (d == 0) {
				if (pts.get(0).getX() > max) max = pts.get(0).getX();
			}
			else {
				if (pts.get(0).getY() > max) max = pts.get(0).getY();
			}
		}
		return max;
	}

}
