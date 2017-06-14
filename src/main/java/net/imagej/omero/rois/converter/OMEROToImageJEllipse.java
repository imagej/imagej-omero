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

import net.imagej.omero.rois.ClosedOMEROEllipse;
import net.imagej.omero.rois.OpenOMEROEllipse;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.Ellipsoid;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.EllipseData;

/**
 * Converts an OMERO {@link EllipseData} to {@link Ellipsoid}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROToImageJEllipse extends
	AbstractOMEROShapeToImageJMask<EllipseData, Ellipsoid<RealPoint>>
{

	@Override
	public Class<EllipseData> getInputType() {
		return EllipseData.class;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<Ellipsoid<RealPoint>> getOutputType() {
		return (Class) Ellipsoid.class;
	}

	@Override
	public Ellipsoid<RealPoint> convert(final EllipseData shape) {
		final BoundaryType bt = RoiConverters.boundaryType(shape);
		if (bt == BoundaryType.OPEN) return new OpenOMEROEllipse(shape);
		// Ellipsoids can only be OPEN or CLOSED
		return new ClosedOMEROEllipse(shape);
	}

}
