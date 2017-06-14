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

import java.lang.reflect.Type;

import net.imagej.omero.rois.OMEROShape;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.composite.CompositeMaskPredicate;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.util.GenericUtils;

import omero.gateway.model.ShapeData;
import omero.model.Shape;

/**
 * Abstract base class for converting {@link MaskPredicate} to OMERO
 * {@link ShapeData}.
 *
 * @author Alison Walter
 * @param <S> OMERO Shape input type
 * @param <M> {@link MaskPredicate} output type
 */
public abstract class AbstractImageJMaskToOMEROShape<L, M extends MaskPredicate<L>, S extends ShapeData>
	extends AbstractConverter<M, S>
{

	@Override
	public boolean canConvert(final ConversionRequest request) {
		final Object src = request.sourceObject();
		if (src == null) {
			return false;
		}
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
			if (m.numDimensions() == 2 && !(m instanceof CompositeMaskPredicate)) {
				return true && canConvert(srcClass, GenericUtils.getClass(dest));
			}
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
			if (m.numDimensions() == 2 && !(m instanceof CompositeMaskPredicate)) {
				return true && canConvert(srcClass, dest);
			}
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

		final M mask = (M) src;
		S shape = null;

		if (mask instanceof OMEROShape) shape = ((OMEROShape<S>) mask).getShape();
		else shape = convert(mask);

		// Create Annotation and link to Shape
		((Shape) shape.asIObject()).linkAnnotation(RoiConverters.createAnnotation(
			mask));

		return (T) shape;
	}

	abstract S convert(M mask);

}
