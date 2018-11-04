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

import net.imagej.omero.roi.AbstractOMERORealMaskRealInterval;
import net.imglib2.roi.BoundaryType;

import omero.gateway.model.PolygonData;

/**
 * Base class for {@link OMEROPolygon}.
 *
 * @author Curtis Rueden
 * @author Alison Walter
 */
public abstract class AbstractOMEROPolygon extends
	AbstractOMERORealMaskRealInterval<PolygonData> implements OMEROPolygon
{

	public AbstractOMEROPolygon(final PolygonData shape, final BoundaryType bt) {
		super(shape, bt);
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

}
