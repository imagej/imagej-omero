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

import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;

import omero.gateway.model.MaskData;

/**
 * Default implementation for {@link OMEROMask}. The {@code MaskData} is assumed
 * to be positioned at integer coordinates, and have integer width/height. If
 * this is not true, measurements may be wrong.
 *
 * @author Alison Walter
 */
public class DefaultOMEROMask extends AbstractOMEROShape<MaskData> implements
	OMEROMask
{

	public DefaultOMEROMask(final MaskData shape) {
		super(shape, BoundaryType.UNSPECIFIED);
	}

	/**
	 * The given {@link RealLocalizable} will be rounded to the nearest integer
	 * coordinates.
	 */
	@Override
	public boolean test(final RealLocalizable l) {
		int x = (int) Math.round(l.getDoublePosition(0));
		int y = (int) Math.round(l.getDoublePosition(1));

		if (x < shape.getX() || x > shape.getWidth() + shape.getX() || y < shape
			.getY() || y > shape.getHeight() + shape.getY())
		{
			return false;
		}

		x = x - (int) shape.getX();
		y = y - (int) shape.getY();

		final int bitLocation = y * (int) shape.getWidth() + x;
		final byte bit = shape.getBit(shape.getMask(), bitLocation);

		return bit == 1 ? true : false;
	}

	@Override
	public double realMin(final int d) {
		if (d == 0)
			return shape.getX();
		return shape.getY();
	}

	@Override
	public double realMax(final int d) {
		if (d == 0)
			return shape.getX() + shape.getWidth();
		return shape.getY() + shape.getY();
	}

}
