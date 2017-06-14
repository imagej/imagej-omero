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

package net.imagej.omero.rois.converter;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.Box;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.RectangleData;

/**
 * Converts an {@link Box} to an OMERO {@link RectangleData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMERORectangle extends
	AbstractImageJMaskToOMEROShape<RealLocalizable, Box<RealPoint>, RectangleData>
{

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<Box<RealPoint>> getInputType() {
		return (Class) Box.class;
	}

	@Override
	public Class<RectangleData> getOutputType() {
		return RectangleData.class;
	}

	@Override
	public RectangleData convert(final Box<RealPoint> mask) {
		final double upperLeftX = mask.center().getDoublePosition(0) - (mask
			.sideLength(0) / 2);
		final double upperLeftY = mask.center().getDoublePosition(1) - (mask
			.sideLength(1) / 2);
		return new RectangleData(upperLeftX, upperLeftY, mask.sideLength(0), mask
			.sideLength(1));
	}

}
