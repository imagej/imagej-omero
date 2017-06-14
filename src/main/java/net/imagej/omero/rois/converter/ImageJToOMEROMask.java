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

import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.mask.integer.MaskIntervalAsRandomAccessibleInterval;
import net.imglib2.roi.mask.integer.RandomAccessibleAsMask;
import net.imglib2.roi.mask.integer.RandomAccessibleIntervalAsMaskInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;

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
	AbstractImageJMaskToOMEROShape<Localizable, MaskInterval, MaskData>
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
	MaskData convert(final MaskInterval mask) {
		return toMaskData(mask);
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
				interval, new BitType());

		long numElements = (rai.dimension(0) * rai.dimension(1)) / 8;
		if ((rai.dimension(0) * rai.dimension(1)) % 8 != 0) numElements++;
		if (numElements > Integer.MAX_VALUE) throw new IllegalArgumentException("");

		// Don't use Cursor, since iteration order may vary
		final byte[] data = new byte[(int) numElements];
		int count = 0;
		int index = 0;
		String b = "";
		final int[] pos = new int[2];
		final RandomAccess<B> ra = rai.randomAccess();
		for (int y = 0; y < rai.dimension(1); y++) {
			pos[1] = y;
			for (int x = 0; x < rai.dimension(0); x++) {
				pos[0] = x;
				ra.setPosition(pos);
				if (ra.get().get()) b += "1";
				else b += "0";
				count++;
				if (count % 8 == 0) {
					data[index] = (byte) Integer.parseInt(b, 2);
					b = "";
					index++;
				}
			}
		}

		if (count % 8 != 0) {
			final int missing = 8 - b.length();
			for (int i = 0; i < missing; i++) {
				b += "0";
			}
			data[index] = (byte) Integer.parseInt(b, 2);
		}

		return new MaskData(rai.min(0), rai.min(1), rai.dimension(0), rai.dimension(
			1), data);
	}

}
