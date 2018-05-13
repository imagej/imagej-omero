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

package net.imagej.omero.rois;

import java.util.concurrent.ExecutionException;

import net.imagej.omero.OMEROService;

import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;
import omero.model.Roi;
import omero.model.TagAnnotationI;

/**
 * Converts an {@link OMERORoiElement} to {@link ROIData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.HIGH)
public class OMERORoiElementToROIData extends
	AbstractConverter<OMERORoiElement, ROIData>
{

	@Parameter
	private ConvertService convert;

	@Parameter
	private OMEROService omero;

	@Parameter
	private LogService log;

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

		final OMERORoiElement ore = (OMERORoiElement) src;
		final ROIData r = new ROIData();
		if (ore.parent() instanceof OMERORoiCollection) {
			final Roi parent = (Roi) ((ROIData) ore.parent().data())
				.asIObject();
			((Roi) r.asIObject()).setDescription(parent.getDescription());
			((Roi) r.asIObject()).setName(parent.getName());
		}

		final ShapeData s = convert.convert(((OMERORoiElement) src).data(),
			ShapeData.class);
		r.addShapeData(s);

		try {
			final TagAnnotationI tag = omero.getAnnotation(
				RoiConverters.IJO_VERSION_DESC, omero.getVersion());

			// created new ROIData so its already loaded, and annotation can just be
			// attached
			((Roi) r.asIObject()).linkAnnotation(tag);

		}
		catch (ServerError | ExecutionException | DSOutOfServiceException
				| DSAccessException exc)
		{
			log.error("Cannot create/retrieve imagej-omero version tag", exc);
		}
		return (T) r;
	}

	@Override
	public Class<ROIData> getOutputType() {
		return ROIData.class;
	}

	@Override
	public Class<OMERORoiElement> getInputType() {
		return OMERORoiElement.class;
	}

}
