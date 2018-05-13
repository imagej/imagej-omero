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

import net.imagej.omero.roi.AbstractMaskPredicateToShapeData;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.mask.integer.RandomAccessibleIntervalAsMaskInterval;
import net.imglib2.type.BooleanType;

import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.MaskData;

/**
 * Converts a {@link MaskInterval} to an OMERO {@link MaskData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class MaskIntervalToMaskData extends
	AbstractMaskPredicateToShapeData<Localizable, MaskInterval, MaskData>
{

	@Parameter
	private ConvertService convert;

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
		final MaskInterval mi)
	{
		RandomAccessibleInterval<B> rai;
		if (mi instanceof RandomAccessibleIntervalAsMaskInterval) rai =
			((RandomAccessibleIntervalAsMaskInterval<B>) mi).getSource();
		else rai = (RandomAccessibleInterval<B>) Masks.toRandomAccessibleInterval(
			mi);

		return convert.convert(rai, MaskData.class);
	}

}
