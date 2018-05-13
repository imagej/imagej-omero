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

package net.imagej.omero.roi.project;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.ShapeData;

/**
 * Converts an {@link OMEROZTCProjectedRealMask} to {@link ShapeData},
 * preserving its Z, T, and C positions.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROZTCProjectedRealMaskToShapeData extends
	AbstractConverter<OMEROZTCProjectedRealMask, ShapeData>
{

	@Parameter
	private ConvertService convert;

	@Override
	public Class<OMEROZTCProjectedRealMask> getInputType() {
		return OMEROZTCProjectedRealMask.class;
	}

	@Override
	public Class<ShapeData> getOutputType() {
		return ShapeData.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(final Object src, final Class<T> dest) {
		if (src == null || dest == null) throw new NullPointerException();
		if (!getInputType().isInstance(src)) {
			throw new IllegalArgumentException("Expected: " + getInputType()
				.getSimpleName() + " Received: " + src.getClass().getSimpleName());
		}
		if (!dest.isAssignableFrom(getOutputType())) {
			throw new IllegalArgumentException("Expected: " + getOutputType()
				.getSimpleName() + " Received: " + dest.getSimpleName());
		}

		final OMEROZTCProjectedRealMask rm = (OMEROZTCProjectedRealMask) src;
		final ShapeData s = convert.convert(rm.getSource(), ShapeData.class);
		if (s == null) throw new IllegalArgumentException("Cannot convert " + rm
			.getSource().getClass() + " to ShapeData");

		// NB: For setZ, etc. passing in -1 will result in that position being set
		// to 0
		if (rm.getZPosition() != -1) s.setZ(rm.getZPosition());
		if (rm.getTimePosition() != -1) s.setT(rm.getTimePosition());
		if (rm.getChannelPosition() != -1) s.setC(rm.getChannelPosition());

		return (T) s;
	}

}
