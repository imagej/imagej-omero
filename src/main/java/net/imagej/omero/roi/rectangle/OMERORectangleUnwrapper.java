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

package net.imagej.omero.roi.rectangle;

import net.imagej.omero.roi.AbstractOMERORealMaskUnwrapper;

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.RectangleData;

/**
 * Unwraps an {@link OMERORectangle}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.HIGH)
public class OMERORectangleUnwrapper extends
	AbstractOMERORealMaskUnwrapper<RectangleData, OMERORectangle>
{

	@Override
	public Class<RectangleData> getOutputType() {
		return RectangleData.class;
	}

	@Override
	public Class<OMERORectangle> getInputType() {
		return OMERORectangle.class;
	}

	@Override
	public void setBoundaryType(final RectangleData shape,
		final String textValue)
	{
		shape.setText(textValue);
	}

	@Override
	public String getTextValue(final RectangleData shape) {
		return shape.getText();
	}

}
