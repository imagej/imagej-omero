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

package net.imagej.omero.roi.line;

import net.imagej.omero.roi.AbstractOMERORealMaskRealInterval;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.util.AbstractRealMaskPoint;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import omero.gateway.model.LineData;

/**
 * Default implementation of {@link OMEROLine}.
 *
 * @author Alison Walter
 */
public class DefaultOMEROLine extends
	AbstractOMERORealMaskRealInterval<LineData> implements OMEROLine
{

	public DefaultOMEROLine(final LineData shape) {
		super(shape, BoundaryType.CLOSED);
	}

	@Override
	public double realMin(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) return shape.getX1() < shape.getX2() ? shape.getX1() : shape
			.getX2();
		return shape.getY1() < shape.getY2() ? shape.getY1() : shape.getY2();
	}

	@Override
	public double realMax(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) return shape.getX1() > shape.getX2() ? shape.getX1() : shape
			.getX2();
		return shape.getY1() > shape.getY2() ? shape.getY1() : shape.getY2();
	}

	@Override
	public RealLocalizableRealPositionable endpointOne() {
		return new LineEndPoint(new double[] { shape.getX1(), shape.getY1() },
			true);
	}

	@Override
	public RealLocalizableRealPositionable endpointTwo() {
		return new LineEndPoint(new double[] { shape.getX2(), shape.getY2() },
			false);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "\nEndpoint One: " + endpointOne()
			.getDoublePosition(0) + ", " + endpointOne().getDoublePosition(1) +
			"\nEndpoint Two: " + endpointTwo().getDoublePosition(0) + ", " +
			endpointTwo().getDoublePosition(1);
	}

	// -- Helper classes --

	private class LineEndPoint extends AbstractRealMaskPoint {

		private final boolean isOne;

		public LineEndPoint(final double[] pos, final boolean isOne) {
			super(pos);
			this.isOne = isOne;
		}

		@Override
		public void updateBounds() {
			// Bounds depend on wrapped OMERO shape, so by updating the shape we're
			// updating the bounds
			if (isOne) {
				shape.setX1(position[0]);
				shape.setY1(position[1]);
			}
			else {
				shape.setX2(position[0]);
				shape.setY2(position[1]);
			}
		}

	}
}
