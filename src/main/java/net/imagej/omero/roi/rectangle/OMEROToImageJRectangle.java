/*
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

package net.imagej.omero.roi.rectangle;

import net.imagej.omero.roi.AbstractShapeDataToRealMaskRealInterval;
import net.imglib2.roi.BoundaryType;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.RectangleData;

/**
 * Converts an OMERO {@link RectangleData} to {@link OMERORectangle}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROToImageJRectangle extends
	AbstractShapeDataToRealMaskRealInterval<RectangleData, OMERORectangle>
{

	@Override
	public Class<RectangleData> getInputType() {
		return RectangleData.class;
	}

	@Override
	public Class<OMERORectangle> getOutputType() {
		return OMERORectangle.class;
	}

	@Override
	public OMERORectangle convert(final RectangleData shape,
		final BoundaryType bt)
	{
		if (bt == BoundaryType.OPEN) return new OpenOMERORectangle(shape);
		// There is no Unspecified boundary behavior for Box
		return new ClosedOMERORectangle(shape);
	}

	@Override
	public String getTextValue(final RectangleData shape) {
		return shape.getText();
	}

}
