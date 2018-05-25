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

import java.lang.reflect.Type;

import net.imglib2.roi.MaskPredicate;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.util.Types;

import omero.gateway.model.ShapeData;

/**
 * Abstract base class for converting {@link MaskPredicate} to OMERO
 * {@link ShapeData}.
 *
 * @author Alison Walter
 * @param <S> OMERO Shape input type
 * @param <M> {@link MaskPredicate} output type
 */
public abstract class AbstractMaskPredicateToShapeData<L, M extends MaskPredicate<L>, S extends ShapeData>
	extends AbstractConverter<M, S>
{

	@Override
	public boolean canConvert(final ConversionRequest request) {
		final Object src = request.sourceObject();
		if (src == null) return false;
		if (request.destType() != null) return canConvert(src, request.destType());
		return canConvert(src, request.destClass());
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean canConvert(final Object src, final Type dest) {
		if (src == null) return false;
		final Class<?> srcClass = src.getClass();
		if (src instanceof MaskPredicate) {
			final MaskPredicate<L> m = (MaskPredicate<L>) src;
			if (m.numDimensions() == 2) return canConvert(srcClass, Types.raw(dest));
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (src == null) return false;
		final Class<?> srcClass = src.getClass();
		if (src instanceof MaskPredicate) {
			final MaskPredicate<L> m = (MaskPredicate<L>) src;
			if (m.numDimensions() == 2) return canConvert(srcClass, dest);
		}
		return false;
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
		return (T) convert((M) src, ROIConverters.createBoundaryTypeString(((M) src)
			.boundaryType()));
	}

	public abstract S convert(M mask, String boundaryType);

}
