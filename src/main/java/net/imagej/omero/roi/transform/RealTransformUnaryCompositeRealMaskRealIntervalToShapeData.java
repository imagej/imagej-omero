/*-
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

package net.imagej.omero.roi.transform;

import java.lang.reflect.Type;
import java.util.function.Predicate;

import net.imagej.omero.roi.ROIConverters;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.Operators.RealTransformMaskOperator;
import net.imglib2.roi.composite.CompositeMaskPredicate;
import net.imglib2.roi.composite.RealTransformUnaryCompositeRealMaskRealInterval;
import net.imglib2.roi.composite.UnaryCompositeMaskPredicate;

import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.Types;

import omero.gateway.model.ShapeData;

/**
 * Converts 2D affine transformed
 * {@link RealTransformUnaryCompositeRealMaskRealInterval}s to
 * {@link ShapeData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.HIGH)
public class RealTransformUnaryCompositeRealMaskRealIntervalToShapeData extends
	AbstractConverter<RealTransformUnaryCompositeRealMaskRealInterval, ShapeData>
{

	@Parameter
	private ConvertService convert;

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
	public boolean canConvert(final Object src, final Type dest) {
		if (src == null) return false;
		final Class<?> srcClass = src.getClass();
		if (src instanceof RealTransformUnaryCompositeRealMaskRealInterval)
			return canConvert(
				(RealTransformUnaryCompositeRealMaskRealInterval) src) && canConvert(
					srcClass, Types.raw(dest));
		return false;
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (src == null) return false;
		final Class<?> srcClass = src.getClass();
		if (src instanceof RealTransformUnaryCompositeRealMaskRealInterval)
			return canConvert(
				(RealTransformUnaryCompositeRealMaskRealInterval) src) && canConvert(
					srcClass, Types.raw(dest));
		return false;
	}

	@Override
	public Class<ShapeData> getOutputType() {
		return ShapeData.class;
	}

	@Override
	public Class<RealTransformUnaryCompositeRealMaskRealInterval> getInputType() {
		return RealTransformUnaryCompositeRealMaskRealInterval.class;
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

		final AffineTransform2D transformToSource = createTransform(
			(UnaryCompositeMaskPredicate<?>) src);
		if (transformToSource == null) throw new IllegalArgumentException(
			"Cannot create transform");

		final ShapeData s = convert.convert(getBase(
			(UnaryCompositeMaskPredicate<?>) src), ShapeData.class);
		s.setTransform(ROIConverters.createAffine(transformToSource));

		return (T) s;
	}

	// -- Helper methods --

	/**
	 * Checks that the {@link CompositeMaskPredicate} is 2D and has an
	 * AffineTransform. This also checks that the operator tree only contains
	 * transformations. If it contains any other operator, then false is returned.
	 *
	 * @param src the {@link UnaryCompositeMaskPredicate} to check
	 * @return true if can be converted, false otherwise
	 */
	private boolean canConvert(final UnaryCompositeMaskPredicate<?> src) {
		if (src.numDimensions() == 2 && (src
			.operator() instanceof RealTransformMaskOperator) &&
			(((RealTransformMaskOperator) src.operator())
				.getTransformToSource() instanceof AffineTransform2D))
		{
			final Predicate<?> rm = src.arg0();
			if (!(rm instanceof CompositeMaskPredicate)) return true;
			else if (((CompositeMaskPredicate<?>) rm)
				.operator() instanceof RealTransformMaskOperator &&
				rm instanceof UnaryCompositeMaskPredicate) return canConvert(
					(UnaryCompositeMaskPredicate<?>) rm);
			else return false;
		}
		return false;
	}

	/**
	 * Recurse through the layers, and return the original {@link Predicate} that
	 * was transformed.
	 *
	 * @param src {@link UnaryCompositeMaskPredicate} to get base mask from
	 * @return the base Mask which was transformed
	 */
	private Predicate<?> getBase(final UnaryCompositeMaskPredicate<?> src) {
		final Predicate<?> operand = src.arg0();
		if (operand instanceof UnaryCompositeMaskPredicate) return getBase(
			(UnaryCompositeMaskPredicate<?>) operand);
		return operand;
	}

	/**
	 * Create the {@link AffineTransform2D} related to this {@link Predicate}.
	 */
	private AffineTransform2D createTransform(final Predicate<?> src) {
		if (src instanceof RealTransformUnaryCompositeRealMaskRealInterval)
			// NB: pre-concatenating a transform modifies it, so make a copy to avoid
			// mutating the input
			return createTransform(
				((RealTransformUnaryCompositeRealMaskRealInterval) src).arg0(),
				(AffineTransform2D) ((RealTransformMaskOperator) ((RealTransformUnaryCompositeRealMaskRealInterval) src)
					.operator()).getTransformToSource().copy());
		else if (src instanceof TransformedOMERORealMaskRealInterval)
			return (AffineTransform2D) ((RealTransformMaskOperator) ((TransformedOMERORealMaskRealInterval<?>) src)
				.operator()).getTransformToSource();
		return null;
	}

	/**
	 * Create the {@link AffineTransform2D} related to this {@link Predicate}.
	 */
	private AffineTransform2D createTransform(final Predicate<?> src,
		final AffineTransform2D transform)
	{
		if (src instanceof RealTransformUnaryCompositeRealMaskRealInterval) {
			final AffineTransform2D affine =
				(AffineTransform2D) ((RealTransformMaskOperator) ((RealTransformUnaryCompositeRealMaskRealInterval) src)
					.operator()).getTransformToSource();
			return createTransform(
				((RealTransformUnaryCompositeRealMaskRealInterval) src).arg0(),
				transform.preConcatenate(affine));
		}
		else if (src instanceof TransformedOMERORealMaskRealInterval) {
			final AffineTransform2D affine =
				(AffineTransform2D) ((RealTransformMaskOperator) ((TransformedOMERORealMaskRealInterval<?>) src)
					.operator()).getTransformToSource();
			return transform.preConcatenate(affine);
		}
		return transform;
	}
}
