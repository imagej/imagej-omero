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

import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;

import omero.gateway.model.PolygonData;

/**
 * Default implementation of {@link OMEROPolygon}. The contains method for this
 * is the most efficient of the polygons, but the edge behavior is unspecified.
 *
 * @author Alison Walter
 */
public class DefaultOMEROPolygon extends AbstractOMEROPolygon {

	public DefaultOMEROPolygon(final PolygonData shape) {
		super(shape, BoundaryType.UNSPECIFIED);
	}

	@Override
	public boolean test(final RealLocalizable l) {
		return Polyshapes.pnpoly(shape.getPoints(), l);
	}

}
