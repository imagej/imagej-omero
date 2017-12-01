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

import net.imagej.omero.rois.DefaultOMEROMask;
import net.imglib2.roi.RealMaskRealInterval;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.MaskData;

/**
 * Converts an OMERO {@link MaskData} to {@link RealMaskRealInterval}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROToImageJMask extends
	AbstractOMEROShapeToImageJMask<MaskData, RealMaskRealInterval>
{

	@Override
	public Class<MaskData> getInputType() {
		return MaskData.class;
	}

	@Override
	public Class<RealMaskRealInterval> getOutputType() {
		return RealMaskRealInterval.class;
	}

	@Override
	public RealMaskRealInterval convert(final MaskData shape) {
		// Masks have no defined boundary behavior
		return new DefaultOMEROMask(shape);
	}

}
