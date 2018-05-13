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

package net.imagej.omero.roi;

import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;

import org.scijava.convert.AbstractConverter;

import omero.gateway.model.ShapeData;

/**
 * Abstract base class for converting OMERO {@link ShapeData} to
 * {@link RealMask}.
 *
 * @author Alison Walter
 * @param <S> OMERO Shape input type
 * @param <M> {@link RealMaskRealInterval} output type
 */
public abstract class AbstractShapeDataToRealMaskRealInterval<S extends ShapeData, M extends RealMaskRealInterval>
	extends AbstractConverter<S, M>
{

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

		return (T) convert((S) src, boundaryType(getTextValue((S) src)));
	}

	public abstract M convert(S shape, BoundaryType bt);

	public abstract String getTextValue(S shape);

	// -- Helper methods --

	private BoundaryType boundaryType(final String omeroTextValue) {
		if (omeroTextValue.contains(ROIConverters.OPEN_BOUNDARY_TEXT))
			return BoundaryType.OPEN;
		if (omeroTextValue.contains(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT))
			return BoundaryType.UNSPECIFIED;
		return BoundaryType.CLOSED;
	}

}
