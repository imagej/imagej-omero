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
public interface OMEROPolyline extends OMEROPolyshape<PolylineData>,
	WritablePolyline
{

	@Override
	default List<Point2D.Double> getPoints() {
		return getShape().getPoints();
	}

	@Override
	default void setPoints(final List<Point2D.Double> points) {
		getShape().setPoints(points);
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

}
