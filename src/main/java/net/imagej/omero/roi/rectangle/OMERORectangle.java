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

import net.imagej.omero.roi.OMERORealMaskRealInterval;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.WritableBox;

import omero.gateway.model.RectangleData;

/**
 * A {@link Box} which wraps an OMERO rectangle Roi.
 *
 * @author Alison Walter
 */
public interface OMERORectangle extends
	OMERORealMaskRealInterval<RectangleData>, WritableBox
{

	@Override
	default double sideLength(final int d) {
		if (d > 1) throw new IllegalArgumentException(
			"OMERO Rectangles are only 2D");
		return d == 0 ? getShape().getWidth() : getShape().getHeight();
	}

	@Override
	default void setSideLength(final int d, final double length) {
		if (d >= 2 || d < 0) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (length < 0) throw new IllegalArgumentException(
			"Cannot have negative edge lengths ");

		if (d == 0) {
			final double halfDiff = (getShape().getWidth() - length) / 2;
			getShape().setX(getShape().getX() + halfDiff);
			getShape().setWidth(length);
		}
		else {
			final double halfDiff = (getShape().getHeight() - length) / 2;
			getShape().setY(getShape().getY() + halfDiff);
			getShape().setHeight(length);
		}
	}

	@Override
	default double realMin(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) return getShape().getX();
		return getShape().getY();
	}

	@Override
	default double realMax(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) return getShape().getX() + getShape().getWidth();
		return getShape().getY() + getShape().getHeight();
	}
}
