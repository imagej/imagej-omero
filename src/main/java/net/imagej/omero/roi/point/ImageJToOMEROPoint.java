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

package net.imagej.omero.roi.point;

import net.imagej.omero.roi.AbstractMaskPredicateToShapeData;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.PointMask;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.PointData;

/**
 * Converts a {@link PointMask} to an OMERO {@link PointData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMEROPoint extends
	AbstractMaskPredicateToShapeData<RealLocalizable, PointMask, PointData>
{

	@Override
	public Class<PointMask> getInputType() {
		return PointMask.class;
	}

	@Override
	public Class<PointData> getOutputType() {
		return PointData.class;
	}

	@Override
	public PointData convert(final PointMask mask, final String boundaryType) {
		final PointData p = new PointData(mask.getDoublePosition(0), mask
			.getDoublePosition(1));
		p.setText(boundaryType);
		return p;
	}

}
