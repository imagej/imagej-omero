/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2018 Open Microscopy Environment:
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

package net.imagej.omero.roi;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.TypedAxis;
import net.imglib2.roi.RealMask;

import omero.gateway.model.ShapeData;

/**
 * {@link RealMask} wrapper for OMERO {@link ShapeData} object.
 *
 * @author Alison Walter
 * @param <S> The type of {@link ShapeData} being wrapped
 */
public interface OMERORealMask<S extends ShapeData> extends RealMask {

	S getShape();

	// TODO: Consider generalizing this to an "opinionated ROI" interface.
	default boolean testPosition(final TypedAxis axis, final double position) {
		int pos;
		if (axis == Axes.Z) pos = getShape().getZ();
		if (axis == Axes.TIME) pos = getShape().getT();
		if (axis == Axes.CHANNEL) pos = getShape().getC();
		else return true;
		return pos == -1 || pos == position;
	}

	// TODO: Consider generalizing this to an "opinionated ROI" interface.
	default AxisType[] requiredAxisTypes() {
		return new AxisType[] { Axes.X, Axes.Y };
	}
}
