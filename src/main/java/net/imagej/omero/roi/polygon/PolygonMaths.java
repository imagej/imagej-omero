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

package net.imagej.omero.roi.polygon;

import java.awt.geom.Point2D;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.GeomMaths;

/**
 * Utility class for polygon
 *
 * @author Alison Walter
 */
public class PolygonMaths {

	public static boolean pnpoly(final List<Point2D.Double> pts,
		final RealLocalizable localizable)
	{
		final double xl = localizable.getDoublePosition(0);
		final double yl = localizable.getDoublePosition(1);

		int i;
		int j;
		boolean result = false;
		for (i = 0, j = pts.size() - 1; i < pts.size(); j = i++) {
			final double xj = pts.get(j).getX();
			final double yj = pts.get(j).getY();

			final double xi = pts.get(i).getX();
			final double yi = pts.get(i).getY();

			if ((yi > yl) != (yj > yl) && (xl < (xj - xi) * (yl - yi) / (yj - yi) +
				xi))
			{
				result = !result;
			}
		}
		return result;
	}

	public static boolean pnpolyWithBoundary(final List<Point2D.Double> pts,
		final RealLocalizable localizable, final boolean isClosed)
	{
		// check edges, this needs to be done first because pnpoly has
		// unknown edge behavior
		boolean edge = false;
		final double[] pt1 = new double[2];
		final double[] pt2 = new double[2];

		for (int i = 0; i < pts.size(); i++) {
			pt1[0] = pts.get(i).getX();
			pt1[1] = pts.get(i).getY();

			// 1e-15 is for error caused by double precision
			if (i == pts.size() - 1) {
				pt2[0] = pts.get(0).getX();
				pt2[1] = pts.get(0).getY();
			}
			else {
				pt2[0] = pts.get(i + 1).getX();
				pt2[1] = pts.get(i + 1).getY();
			}

			edge = GeomMaths.lineContains(pt1, pt2, localizable, 2);

			if (edge) return isClosed;
		}

		// not on edge, check inside
		return pnpoly(pts, localizable);
	}
}
