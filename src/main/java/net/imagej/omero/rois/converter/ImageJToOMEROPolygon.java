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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.Polygon2D;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.PolygonData;

/**
 * Converts a {@link Polygon2D} to an OMERO {@link PolygonData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMEROPolygon extends
	AbstractImageJMaskToOMEROShape<RealLocalizable, Polygon2D<RealPoint>, PolygonData>
{

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<Polygon2D<RealPoint>> getInputType() {
		return (Class) Polygon2D.class;
	}

	@Override
	public Class<PolygonData> getOutputType() {
		return PolygonData.class;
	}

	@Override
	public PolygonData convert(final Polygon2D<RealPoint> mask) {
		final List<Point2D.Double> points = new ArrayList<>();
		for (int i = 0; i < mask.numVertices(); i++) {
			final RealPoint loc = mask.vertex(i);
			points.add(new Point2D.Double(loc.getDoublePosition(0), loc
				.getDoublePosition(1)));
		}
		return new PolygonData(points);
	}

}
