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

package net.imagej.omero.roi.polyline;

import java.awt.geom.Point2D;
import java.util.List;

import net.imagej.omero.roi.OMERORealMaskRealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.GeomMaths;
import net.imglib2.roi.geom.real.Polyline;
import net.imglib2.roi.geom.real.WritablePolyline;

import omero.gateway.model.PolylineData;

/**
 * A {@link Polyline} which wraps an OMERO polyline Roi
 *
 * @author Alison Walter
 */
public interface OMEROPolyline extends OMERORealMaskRealInterval<PolylineData>,
	WritablePolyline
{

	@Override
	default int numVertices() {
		return getShape().getPoints().size();
	}

	@Override
	default void addVertex(final int index, final RealLocalizable vertex) {
		final List<Point2D.Double> pts = getShape().getPoints();
		pts.add(index, new Point2D.Double(vertex.getDoublePosition(0), vertex
			.getDoublePosition(1)));
		getShape().setPoints(pts);
	}

	@Override
	default void removeVertex(final int index) {
		final List<Point2D.Double> pts = getShape().getPoints();
		pts.remove(index);
		getShape().setPoints(pts);
	}

	@Override
	default boolean test(final RealLocalizable l) {
		final double[] ptOne = new double[2];
		final double[] ptTwo = new double[2];

		for (int i = 1; i < getShape().getPoints().size(); i++) {
			ptOne[0] = getShape().getPoints().get(i - 1).getX();
			ptOne[1] = getShape().getPoints().get(i - 1).getY();

			ptTwo[0] = getShape().getPoints().get(i).getX();
			ptTwo[1] = getShape().getPoints().get(i).getY();

			final boolean testLineContains = GeomMaths.lineContains(ptOne, ptTwo, l,
				2);
			if (testLineContains) return true;
		}
		return false;
	}

	@Override
	default double realMin(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		final List<Point2D.Double> pts = getShape().getPoints();
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
		final List<Point2D.Double> pts = getShape().getPoints();
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
