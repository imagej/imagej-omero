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

package net.imagej.omero.roi.polyline;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import net.imagej.omero.roi.AbstractMaskPredicateToShapeData;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.Polyline;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.PolylineData;

/**
 * Converts a {@link Polyline} to an OMERO {@link PolylineData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMEROPolyline extends
	AbstractMaskPredicateToShapeData<RealLocalizable, Polyline, PolylineData>
{

	@Override
	public Class<Polyline> getInputType() {
		return Polyline.class;
	}

	@Override
	public Class<PolylineData> getOutputType() {
		return PolylineData.class;
	}

	@Override
	public PolylineData convert(final Polyline mask, final String boundaryType) {
		final List<Point2D.Double> points = new ArrayList<>();
		for (int i = 0; i < mask.numVertices(); i++) {
			final RealLocalizable loc = mask.vertex(i);
			points.add(new Point2D.Double(loc.getDoublePosition(0), loc
				.getDoublePosition(1)));
		}
		final PolylineData p = new PolylineData(points);
		p.setText(boundaryType);
		return p;
	}

}
