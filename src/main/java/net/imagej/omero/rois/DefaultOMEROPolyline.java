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

import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.util.AbstractRealMaskPoint;

import omero.gateway.model.PolylineData;

/**
 * Default implementation of {@link OMEROPolyline}.
 *
 * @author Alison Walter
 */
public class DefaultOMEROPolyline extends AbstractOMEROShape<PolylineData>
	implements OMEROPolyline
{

	public DefaultOMEROPolyline(final PolylineData shape) {
		super(shape, BoundaryType.CLOSED);
	}

	@Override
	public RealPoint vertex(final int pos) {
		return new PolylineVertex(shape.getPoints().get(pos));
	}

	// -- Helper classes --

	private class PolylineVertex extends AbstractRealMaskPoint {

		final Point2D.Double point;

		public PolylineVertex(final Point2D.Double p) {
			super(new double[] { p.getX(), p.getY() });
			point = p;
		}

		@Override
		public void updateBounds() {
			// Bounds depend on wrapped OMERO shape, so by updating the shape we're
			// updating the bounds
			point.setLocation(position[0], position[1]);
		}

	}

}
