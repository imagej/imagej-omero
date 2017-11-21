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

import net.imagej.omero.rois.ClosedOMEROPolygon;
import net.imagej.omero.rois.DefaultOMEROPolygon;
import net.imagej.omero.rois.OpenOMEROPolygon;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.Polygon2D;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.PolygonData;

/**
 * Converts an OMERO {@link PolygonData} to {@link Polygon2D}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROToImageJPolygon extends
	AbstractOMEROShapeToImageJMask<PolygonData, Polygon2D<RealPoint>>
{

	@Override
	public Class<PolygonData> getInputType() {
		return PolygonData.class;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<Polygon2D<RealPoint>> getOutputType() {
		return (Class) Polygon2D.class;
	}

	@Override
	public Polygon2D<RealPoint> convert(final PolygonData shape) {
		final BoundaryType bt = RoiConverters.boundaryType(shape);
		if (bt == BoundaryType.OPEN) return new OpenOMEROPolygon(shape);
		else if (bt == BoundaryType.CLOSED) return new ClosedOMEROPolygon(shape);
		return new DefaultOMEROPolygon(shape);
	}

}