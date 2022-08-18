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

package net.imagej.omero.roi.rectangle;

import net.imagej.omero.roi.AbstractOMERORealMaskRealInterval;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.util.AbstractRealMaskPoint;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import omero.gateway.model.RectangleData;

/**
 * Base class for {@link OMERORectangle}.
 *
 * @author Curtis Rueden
 * @author Alison Walter
 */
public abstract class AbstractOMERORectangle extends
	AbstractOMERORealMaskRealInterval<RectangleData> implements OMERORectangle
{

	public AbstractOMERORectangle(final RectangleData shape,
		final BoundaryType bt)
	{
		super(shape, bt);
	}

	@Override
	public RealLocalizableRealPositionable center() {
		final double px = getShape().getX() + getShape().getWidth() / 2;
		final double py = getShape().getY() + getShape().getHeight() / 2;
		return new AbstractRealMaskPoint(new double[] { px, py }) {

			@Override
			public void updateBounds() {
				// Bounds depend on wrapped OMERO shape, so by
				// updating the shape we're updating the bounds.
				shape.setX(position[0] - shape.getWidth() / 2);
				shape.setY(position[1] - shape.getHeight() / 2);
			}
		};
	}

	@Override
	public int hashCode() {
		return Box.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Box && Box.equals(this, (Box) obj);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "\nCenter: " + center()
			.getDoublePosition(0) + ", " + center().getDoublePosition(1) +
			"\nWidth: " + sideLength(0) + "\nHeight: " + sideLength(1);
	}

}
