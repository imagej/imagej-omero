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

import net.imagej.omero.roi.OMERORealMaskRealInterval;
import net.imagej.omero.roi.ROIConverters;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.roi.Bounds;
import net.imglib2.roi.Operators.RealTransformMaskOperator;

import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.ShapeData;
import omero.model.AffineTransform;

/**
 * Converts a transformed {@link ShapeData} to a
 * {@link TransformedOMERORealMaskRealInterval}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.HIGH)
public class ShapeDataToTransformedOMERORealMaskRealInterval extends
	AbstractConverter<ShapeData, TransformedOMERORealMaskRealInterval<?>>
{

	@Parameter
	private ConvertService convert;

	@Override
	public boolean canConvert(final Object src, final Type dest) {
		if (super.canConvert(src, dest)) return ((ShapeData) src)
			.getTransform() != null;
		return false;
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (super.canConvert(src, dest)) return ((ShapeData) src)
			.getTransform() != null;
		return false;
	}

	@Override
	public Class<ShapeData> getInputType() {
		return ShapeData.class;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<TransformedOMERORealMaskRealInterval<?>> getOutputType() {
		return (Class) TransformedOMERORealMaskRealInterval.class;
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

		final ShapeData shape = (ShapeData) src;

		// Keep a copy of the transform, and then set transform to null. This
		// ensures that the convert(...) call does not match this converter again
		final AffineTransform copy = shape.getTransform();
		shape.setTransform(null);
		final OMERORealMaskRealInterval<?> rmri = convert.convert(shape,
			OMERORealMaskRealInterval.class);

		// Reset transform back to original
		rmri.getShape().setTransform(copy);

		final AffineGet transformToSource = ROIConverters.createAffine(copy);
		return (T) new TransformedOMERORealMaskRealInterval<>(rmri,
			new Bounds.RealTransformRealInterval(rmri, transformToSource),
			new RealTransformMaskOperator(transformToSource));
	}
}
