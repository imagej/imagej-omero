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

package net.imagej.omero.roi.ellipse;

import net.imagej.omero.roi.AbstractShapeDataToRealMaskRealInterval;
import net.imglib2.roi.BoundaryType;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.EllipseData;

/**
 * Converts an OMERO {@link EllipseData} to {@link OMEROEllipse}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROToImageJEllipse extends
	AbstractShapeDataToRealMaskRealInterval<EllipseData, OMEROEllipse>
{

	@Override
	public Class<EllipseData> getInputType() {
		return EllipseData.class;
	}

	@Override
	public Class<OMEROEllipse> getOutputType() {
		return OMEROEllipse.class;
	}

	@Override
	public OMEROEllipse convert(final EllipseData shape, final BoundaryType bt) {
		if (bt == BoundaryType.OPEN) return new OpenOMEROEllipse(shape);
		// OMEROEllipses can only be OPEN or CLOSED
		return new ClosedOMEROEllipse(shape);
	}

	@Override
	public String getTextValue(final EllipseData shape) {
		return shape.getText();
	}

}
