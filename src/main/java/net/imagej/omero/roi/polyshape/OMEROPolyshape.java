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

package net.imagej.omero.roi.polyshape;

import java.awt.geom.Point2D;
import java.util.List;

import net.imagej.omero.roi.OMERORealMaskRealInterval;

import omero.gateway.model.ShapeData;

/**
 * Base interface for OMERO polygons and polylines.
 *
 * @author Curtis Rueden
 * @author Alison Walter
 */
public interface OMEROPolyshape<SD extends ShapeData> extends
	OMERORealMaskRealInterval<SD>
{

	List<Point2D.Double> getPoints();
	void setPoints(List<Point2D.Double> points);

	@Override
	default double realMin(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		final List<Point2D.Double> pts = getPoints();
		double min = d == 0 ? pts.get(0).getX() : pts.get(0).getY();
		for (int i = 1; i < pts.size(); i++) {
			if (d == 0) {
				if (pts.get(i).getX() < min) min = pts.get(i).getX();
			}
			else {
				if (pts.get(i).getY() < min) min = pts.get(i).getY();
			}
		}
		return min;
	}

	@Override
	default double realMax(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		final List<Point2D.Double> pts = getPoints();
		double max = d == 0 ? pts.get(0).getX() : pts.get(0).getY();
		for (int i = 1; i < pts.size(); i++) {
			if (d == 0) {
				if (pts.get(i).getX() > max) max = pts.get(i).getX();
			}
			else {
				if (pts.get(i).getY() > max) max = pts.get(i).getY();
			}
		}
		return max;
	}

}
