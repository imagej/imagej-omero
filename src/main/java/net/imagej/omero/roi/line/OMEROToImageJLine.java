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

package net.imagej.omero.roi.line;

import net.imagej.omero.roi.AbstractShapeDataToRealMaskRealInterval;
import net.imglib2.roi.BoundaryType;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.LineData;

/**
 * Converts an OMERO {@link LineData} to {@link OMEROLine}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROToImageJLine extends
	AbstractShapeDataToRealMaskRealInterval<LineData, OMEROLine>
{

	@Override
	public Class<LineData> getInputType() {
		return LineData.class;
	}

	@Override
	public Class<OMEROLine> getOutputType() {
		return OMEROLine.class;
	}

	@Override
	public OMEROLine convert(final LineData shape, final BoundaryType bt) {
		// Lines can only be CLOSED
		return new DefaultOMEROLine(shape);
	}

	@Override
	public String getTextValue(final LineData shape) {
		return shape.getText();
	}

}
