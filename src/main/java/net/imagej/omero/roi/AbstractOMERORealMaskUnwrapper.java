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

import org.scijava.convert.AbstractConverter;

import omero.gateway.model.ShapeData;

/**
 * Unwraps {@link OMERORealMask} objects.
 *
 * @author Alison Walter
 * @param <S> type of the wrapped {@link ShapeData}
 * @param <O> type of the {@link OMERORealMask} being unwrapped
 */
public abstract class AbstractOMERORealMaskUnwrapper<S extends ShapeData, O extends OMERORealMask<S>>
	extends AbstractConverter<O, S>
{

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

		final S shape = ((OMERORealMask<S>) src).getShape();

		final String bt = generateBoundaryTypeString(shape, ROIConverters
			.createBoundaryTypeString(((O) src).boundaryType()));

		setBoundaryType(shape, bt);
		return (T) shape;
	}

	public abstract void setBoundaryType(S shape, String textValue);

	public abstract String getTextValue(S shape);

	// -- Helper methods --

	private String generateBoundaryTypeString(final S shape,
		final String boundaryType)
	{
		final String currentText = getTextValue(shape);

		if (currentText.isEmpty()) return boundaryType;
		if (currentText.contains(boundaryType)) return currentText;
		if (currentText.contains("ij-bt:[")) {
			final int pos = currentText.indexOf("ij-bt:[");
			final char c = currentText.charAt(pos + 7);
			final String text = currentText.replace("ij-bt:[" + c + "]",
				boundaryType);
			return text;
		}

		return currentText + " " + boundaryType;
	}

}
