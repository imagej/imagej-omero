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

package net.imagej.omero.roi.mask;

import java.lang.reflect.Type;

import net.imagej.omero.roi.ROIConverters;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.util.Util;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.MaskData;

/**
 * Converts a {@link RandomAccessibleInterval} to an OMERO {@link MaskData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class RAIToMaskData extends
	AbstractConverter<RandomAccessibleInterval<?>, MaskData>
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
	public boolean canConvert(final Object src, final Type dest) {
		if (src == null) return false;
		return canConvert(src) && super.canConvert(src, dest);
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (src == null) return false;
		return canConvert(src) && super.canConvert(src, dest);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<RandomAccessibleInterval<?>> getInputType() {
		return (Class) RandomAccessibleInterval.class;
	}

	@Override
	public Class<MaskData> getOutputType() {
		return MaskData.class;
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

		final MaskData md = toMaskData((RandomAccessibleInterval<?>) src);
		md.setText(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT);
		return (T) md;
	}

	// -- Helper methods --

	private boolean canConvert(final Object src) {
		if (src instanceof RandomAccessibleInterval) {
			final RandomAccessibleInterval<?> rai = (RandomAccessibleInterval<?>) src;
			return rai.numDimensions() == 2 && Util.getTypeFromInterval(
				rai) instanceof BooleanType;
		}
		return false;
	}

	private <B extends BooleanType<B>> MaskData toMaskData(
		final RandomAccessibleInterval<?> src)
	{

		@SuppressWarnings("unchecked")
		final RandomAccessibleInterval<B> rai = (RandomAccessibleInterval<B>) src;

		final long xd = rai.dimension(0) - 1;
		final long yd = rai.dimension(1) - 1;
		long numElements = (xd * yd) / 8;
		if ((xd * yd) % 8 != 0) numElements++;
		if (numElements > Integer.MAX_VALUE) throw new IllegalArgumentException("");

		// Don't use Cursor, since iteration order may vary
		final byte[] data = new byte[(int) numElements];
		final RandomAccess<B> ra = rai.randomAccess();
		byte b = 0;
		for (int y = 0; y < yd; y++) {
			for (int x = 0; x < xd; x++) {
				ra.setPosition(x + rai.min(0), 0);
				ra.setPosition(y + rai.min(1), 1);

				b = (byte) (b << 1);
				b += ra.get().get() ? 1 : 0;
				if ((y * xd + x + 1) % 8 == 0) {
					data[(int) (((y * xd + x + 1) / 8) - 1)] = b;
					b = 0;
				}
			}
		}

		// append zeros if number of elements is not a factor of 8
		if ((xd * yd) % 8 != 0) {
			long count = xd * yd;
			while (count % 8 != 0) {
				b = (byte) (b << 1);
				count++;
			}
			data[data.length - 1] = b;
		}

		return new MaskData(rai.min(0), rai.min(1), xd, yd, data);
	}

}
