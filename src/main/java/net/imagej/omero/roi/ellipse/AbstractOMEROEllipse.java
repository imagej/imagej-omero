/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2022 Open Microscopy Environment:
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

package net.imagej.omero.roi.ellipse;

import net.imagej.omero.roi.AbstractOMERORealMaskRealInterval;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.SuperEllipsoid;
import net.imglib2.roi.util.AbstractRealMaskPoint;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import omero.gateway.model.EllipseData;

/**
 * Base class for {@link OMEROEllipse}.
 *
 * @author Curtis Rueden
 * @author Alison Walter
 */
public abstract class AbstractOMEROEllipse extends
	AbstractOMERORealMaskRealInterval<EllipseData> implements OMEROEllipse
{

	public AbstractOMEROEllipse(final EllipseData shape, final BoundaryType bt) {
		super(shape, bt);
	}

	@Override
	public RealLocalizableRealPositionable center() {
		final double x = shape.getX(), y = shape.getY();
		return new AbstractRealMaskPoint(new double[] { x, y }) {

			@Override
			public void updateBounds() {
				// Bounds depend on wrapped OMERO shape, so by
				// updating the shape we're updating the bounds.
				shape.setX(position[0]);
				shape.setY(position[1]);
			}
		};
	}

	@Override
	public int hashCode() {
		return SuperEllipsoid.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SuperEllipsoid && //
			SuperEllipsoid.equals(this, (SuperEllipsoid) obj);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "\nCenter: " + center()
			.getDoublePosition(0) + ", " + center().getDoublePosition(1) + "Radii: " +
			semiAxisLength(0) + ", " + semiAxisLength(1);
	}

}
