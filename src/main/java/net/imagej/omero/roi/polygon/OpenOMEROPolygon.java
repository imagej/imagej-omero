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

import net.imagej.omero.roi.AbstractOMERORealMaskRealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.util.AbstractRealMaskPoint;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import omero.gateway.model.PolygonData;

/**
 * An {@link OMEROPolygon} with open boundary behavior.
 *
 * @author Alison Walter
 */
public class OpenOMEROPolygon extends
	AbstractOMERORealMaskRealInterval<PolygonData> implements OMEROPolygon
{

	public OpenOMEROPolygon(final PolygonData shape) {
		super(shape, BoundaryType.OPEN);
	}

	@Override
	public boolean test(final RealLocalizable l) {
		return PolygonMaths.pnpolyWithBoundary(shape.getPoints(), l, false);
	}

	@Override
	public RealLocalizableRealPositionable vertex(final int pos) {
		return new PolygonVertex(shape.getPoints(), pos);
	}

	@Override
	public String toString() {
		String s = getClass().getSimpleName();
		for (int i = 0; i < numVertices(); i++) {
			s += "\nVertex " + i + ": " + vertex(i).getDoublePosition(0) + ", " +
				vertex(i).getDoublePosition(1);
		}
		return s;
	}

	// -- Helper classes --

	private class PolygonVertex extends AbstractRealMaskPoint {

		private final List<Point2D.Double> pts;
		private final int index;

		public PolygonVertex(final List<Point2D.Double> pts, final int pos) {
			super(new double[] { pts.get(pos).getX(), pts.get(pos).getY() });
			this.pts = pts;
			index = pos;
		}

		@Override
		public void updateBounds() {
			// Bounds depend on wrapped OMERO shape, so by updating the shape we're
			// updating the bounds
			pts.get(index).setLocation(position[0], position[1]);
			shape.setPoints(pts);
		}
	}
}
