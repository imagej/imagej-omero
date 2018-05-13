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

package net.imagej.omero.roi.project;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.ShapeData;

/**
 * Converts an {@link ProjectedOMERORealMask} to {@link ShapeData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ProjectedOMERORealMaskToShapeData extends
	AbstractConverter<ProjectedOMERORealMask<?>, ShapeData>
{

	@Parameter
	private ConvertService convert;

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<ProjectedOMERORealMask<?>> getInputType() {
		return (Class) ProjectedOMERORealMask.class;
	}

	@Override
	public Class<ShapeData> getOutputType() {
		return ShapeData.class;
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

		return (T) convert.convert(((ProjectedOMERORealMask<?>) src).getSource(),
			ShapeData.class);
	}

}
