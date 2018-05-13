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

import java.util.function.Predicate;

import net.imagej.omero.roi.OMERORealMaskRealInterval;
import net.imglib2.AbstractWrappedRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.Operators.MaskOperator;
import net.imglib2.roi.Operators.RealTransformMaskOperator;
import net.imglib2.roi.composite.UnaryCompositeMaskPredicate;

import omero.gateway.model.ShapeData;

/**
 * A transformed {@link OMERORealMaskRealInterval} represented as a
 * {@link UnaryCompositeMaskPredicate}.
 *
 * @author Alison Walter
 * @param <S> the original transformed {@link ShapeData} type
 */
public class TransformedOMERORealMaskRealInterval<S extends ShapeData> extends
	AbstractWrappedRealInterval<RealInterval> implements
	UnaryCompositeMaskPredicate<RealLocalizable>, OMERORealMaskRealInterval<S>
{

	private final RealTransformMaskOperator transformToSource;
	private final OMERORealMaskRealInterval<S> omeroRMRI;
	private final Predicate<? super RealLocalizable> predicate;

	public TransformedOMERORealMaskRealInterval(
		final OMERORealMaskRealInterval<S> source,
		final RealInterval transformedBounds,
		final RealTransformMaskOperator transformToSource)
	{
		super(transformedBounds);
		this.transformToSource = transformToSource;
		omeroRMRI = source;
		predicate = transformToSource.predicate(source);
	}

	@Override
	public MaskOperator operator() {
		return transformToSource;
	}

	@Override
	public boolean test(final RealLocalizable t) {
		return predicate.test(t);
	}

	@Override
	public S getShape() {
		return omeroRMRI.getShape();
	}

	@Override
	public Predicate<? super RealLocalizable> arg0() {
		return omeroRMRI;
	}
}
