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
import java.util.Iterator;
import java.util.List;

import net.imglib2.roi.MaskPredicate;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;

/**
 * Converts an OMERO {@link ROIData} to an {@link OMEROROICollection}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ROIDataToOMEROROICollection extends
	AbstractConverter<ROIData, OMEROROICollection>
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
		if (super.canConvert(src, dest)) return checkShapeData((ROIData) src);
		return false;
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (super.canConvert(src, dest)) return checkShapeData((ROIData) src);
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!(src instanceof ROIData)) throw new IllegalArgumentException(
			"Invalid input type: " + src.getClass());
		if (!dest.isAssignableFrom(getOutputType()))
			throw new IllegalArgumentException("Invalid destination class: " + dest);

		return (T) new DefaultOMEROROICollection((ROIData) src, getContext()
			.getService(ConvertService.class));
	}

	@Override
	public Class<OMEROROICollection> getOutputType() {
		return OMEROROICollection.class;
	}

	@Override
	public Class<ROIData> getInputType() {
		return ROIData.class;
	}

	// -- Helper methods --

	/**
	 * Ensure all the shapes in the ROIData can be converted.
	 *
	 * @param rd ROIData whose shapes should be checked
	 * @return true if all supported, false otherwise
	 */
	private boolean checkShapeData(final ROIData rd) {
		final Iterator<List<ShapeData>> itr = rd.getIterator();
		while (itr.hasNext()) {
			for (final ShapeData shape : itr.next())
				if (!convert.supports(shape, MaskPredicate.class)) return false;
		}
		return true;
	}

}
