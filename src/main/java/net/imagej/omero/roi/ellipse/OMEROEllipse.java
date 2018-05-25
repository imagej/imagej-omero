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

package net.imagej.omero.roi.ellipse;

import net.imagej.omero.roi.OMERORealMaskRealInterval;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.WritableEllipsoid;

import omero.gateway.model.EllipseData;

/**
 * An {@link Ellipsoid} which wraps an OMERO ellipse Roi.
 *
 * @author Alison Walter
 */
public interface OMEROEllipse extends OMERORealMaskRealInterval<EllipseData>,
	WritableEllipsoid
{

	@Override
	default double exponent() {
		return 2;
	}

	@Override
	default double semiAxisLength(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		return d == 0 ? getShape().getRadiusX() : getShape().getRadiusY();
	}

	@Override
	default void setSemiAxisLength(final int d, final double length) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (length <= 0) throw new IllegalArgumentException(
			"Semi-axis lengths must but positive and non-zero.");
		if (d == 0) getShape().setRadiusX(length);
		else getShape().setRadiusY(length);
	}

	@Override
	default double realMin(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) return getShape().getX() - getShape().getRadiusX();
		return getShape().getY() - getShape().getRadiusY();
	}

	@Override
	default double realMax(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) return getShape().getX() + getShape().getRadiusX();
		return getShape().getY() + getShape().getRadiusY();
	}
}
