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

import net.imagej.omero.roi.AbstractMaskPredicateToShapeData;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.Ellipsoid;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.EllipseData;

/**
 * Converts an {@link Ellipsoid} to an OMERO {@link EllipseData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMEROEllipse extends
	AbstractMaskPredicateToShapeData<RealLocalizable, Ellipsoid, EllipseData>
{

	@Override
	public Class<Ellipsoid> getInputType() {
		return Ellipsoid.class;
	}

	@Override
	public Class<EllipseData> getOutputType() {
		return EllipseData.class;
	}

	@Override
	public EllipseData convert(final Ellipsoid mask, final String boundaryType) {
		final EllipseData e = new EllipseData(mask.center().getDoublePosition(0),
			mask.center().getDoublePosition(1), mask.semiAxisLength(0), mask
				.semiAxisLength(1));
		e.setText(boundaryType);
		return e;
	}

}
