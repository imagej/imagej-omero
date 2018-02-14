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

package net.imagej.omero.rois.mask;

import net.imagej.omero.rois.AbstractMaskPredicateToShapeData;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.mask.integer.MaskIntervalAsRandomAccessibleInterval;
import net.imglib2.roi.mask.integer.RandomAccessibleAsMask;
import net.imglib2.roi.mask.integer.RandomAccessibleIntervalAsMaskInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BoolType;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.MaskData;

/**
 * Converts a {@link RandomAccessibleAsMask} to an OMERO {@link MaskData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMEROMask extends
	AbstractMaskPredicateToShapeData<Localizable, MaskInterval, MaskData>
{

	@Override
	public Class<MaskInterval> getInputType() {
		return MaskInterval.class;
	}

	@Override
	public Class<MaskData> getOutputType() {
		return MaskData.class;
	}

	@Override
	public MaskData convert(final MaskInterval mask, final String boundaryType) {
		final MaskData m = toMaskData(mask);
		m.setText(boundaryType);
		return m;
	}

	// -- Helper methods --

	@SuppressWarnings("unchecked")
	private <B extends BooleanType<B>> MaskData toMaskData(
		final MaskInterval interval)
	{
		RandomAccessibleInterval<B> rai = null;
		if (interval instanceof RandomAccessibleIntervalAsMaskInterval) rai =
			((RandomAccessibleIntervalAsMaskInterval<B>) interval).getSource();
		else rai =
			(RandomAccessibleInterval<B>) new MaskIntervalAsRandomAccessibleInterval<>(
				interval, new BoolType());

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
