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

import net.imagej.omero.roi.AbstractMaskPredicateToShapeData;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.Line;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.LineData;

/**
 * Converts a {@link Line} to an OMERO {@link LineData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMEROLine extends
	AbstractMaskPredicateToShapeData<RealLocalizable, Line, LineData>
{

	@Override
	public Class<Line> getInputType() {
		return Line.class;
	}

	@Override
	public Class<LineData> getOutputType() {
		return LineData.class;
	}

	@Override
	public LineData convert(final Line mask, final String boundaryType) {
		final LineData l = new LineData(mask.endpointOne().getDoublePosition(0),
			mask.endpointOne().getDoublePosition(1), mask.endpointTwo()
				.getDoublePosition(0), mask.endpointTwo().getDoublePosition(1));
		l.setText(boundaryType);
		return l;
	}

}
