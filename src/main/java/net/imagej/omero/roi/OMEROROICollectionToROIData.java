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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imagej.omero.OMEROService;

import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.EllipseData;
import omero.gateway.model.LineData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PointData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.PolylineData;
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TextData;

/**
 * Converts an {@link OMEROROICollection} to {@link ROIData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.HIGH)
public class OMEROROICollectionToROIData extends
	AbstractConverter<OMEROROICollection, ROIData>
{

	@Parameter
	private OMEROService omero;

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

		final ROIData r = ((OMEROROICollection) src).data();
		final ROIData roiToUpdate = omero.getUpdatedServerROIData(r.getId());

		final Iterator<List<ShapeData>> itr = r.getIterator();
		while (itr.hasNext()) {
			final List<ShapeData> shapes = itr.next();
			for (final ShapeData shape : shapes) {
				// Set boundary type, if not already specified
				setTextValue(shape, generateBoundaryTypeString(shape));
			}
		}

		// Synchronize the server object, but don't return it. OMERORoiCollections
		// do not get sync-ed to the server object, so updates to the server side
		// object would be lost
		if (roiToUpdate != null) synchronizeShapes(((OMEROROICollection) src)
			.data(), roiToUpdate);

		return (T) r;
	}

	@Override
	public Class<ROIData> getOutputType() {
		return ROIData.class;
	}

	@Override
	public Class<OMEROROICollection> getInputType() {
		return OMEROROICollection.class;
	}

	// -- Helper methods --

	/**
	 * Generates the new text value for the given {@link ShapeData}, which
	 * contains information about the ImageJ boundary behavior. If the
	 * {@link ShapeData} already has text, the boundary information is appended to
	 * the text.
	 *
	 * @param shape the shape to create the text value for
	 * @return the new text value of the {@link ShapeData}
	 */
	private String generateBoundaryTypeString(final ShapeData shape) {
		final String currentText = getTextValue(shape);
		if (currentText.contains(ROIConverters.CLOSED_BOUNDARY_TEXT) || currentText
			.contains(ROIConverters.OPEN_BOUNDARY_TEXT) || currentText.contains(
				ROIConverters.UNSPECIFIED_BOUNDARY_TEXT)) return currentText;
		return currentText + ROIConverters.CLOSED_BOUNDARY_TEXT;
	}

	/**
	 * Returns the {@code getText()} for the given {@link ShapeData}. The
	 * {@code getText()} method is only implemented on the concrete shape classes.
	 *
	 * @param shape {@link ShapeData} whose text will be returned
	 * @return {@code shape}'s text field value, if valid shape class
	 */
	private String getTextValue(final ShapeData shape) {
		if (shape instanceof EllipseData) return ((EllipseData) shape).getText();
		if (shape instanceof LineData) return ((LineData) shape).getText();
		if (shape instanceof MaskData) return ((MaskData) shape).getText();
		if (shape instanceof PointData) return ((PointData) shape).getText();
		if (shape instanceof PolylineData) return ((PolylineData) shape).getText();
		if (shape instanceof PolygonData) return ((PolygonData) shape).getText();
		if (shape instanceof RectangleData) return ((RectangleData) shape)
			.getText();
		if (shape instanceof TextData) return ((TextData) shape).getText();
		throw new IllegalArgumentException("Unsupport type: " + shape.getClass());
	}

	/**
	 * Sets the text field of the given {@link ShapeData} to the given
	 * {@code String}. The {@code setText()} method is only implemented on the
	 * concrete shape classes.
	 *
	 * @param shape {@link ShapeData} whose text field will be set
	 * @param value {@code String} the text field will be set to
	 */
	private void setTextValue(final ShapeData shape, final String value) {
		if (shape instanceof EllipseData) ((EllipseData) shape).setText(value);
		else if (shape instanceof LineData) ((LineData) shape).setText(value);
		else if (shape instanceof MaskData) ((MaskData) shape).setText(value);
		else if (shape instanceof PointData) ((PointData) shape).setText(value);
		else if (shape instanceof PolylineData) ((PolylineData) shape).setText(
			value);
		else if (shape instanceof PolygonData) ((PolygonData) shape).setText(value);
		else if (shape instanceof RectangleData) ((RectangleData) shape).setText(
			value);
		else if (shape instanceof TextData) return;
		else throw new IllegalArgumentException("Unsupport type: " + shape
			.getClass());
	}

	/**
	 * Synchronize the {@link ROIData} objects, such that the stored
	 * {@link ROIData} has the same shapes as the {@link ROIData} backing the
	 * {@link OMEROROICollection}.
	 *
	 * @param updatedRoi {@link ROIData} backing the {@link OMEROROICollection}
	 * @param roiToUpdate {@link ROIData} which will be used to update the server
	 */
	private void synchronizeShapes(final ROIData updatedRoi,
		final ROIData roiToUpdate)
	{
		final List<ShapeData> updateShapes = new ArrayList<>();
		final List<ShapeData> shapesToUpdate = new ArrayList<>();

		final Iterator<List<ShapeData>> updatedItr = updatedRoi.getIterator();
		final Iterator<List<ShapeData>> toUpdateItr = roiToUpdate.getIterator();
		while (updatedItr.hasNext())
			updateShapes.addAll(updatedItr.next());
		while (toUpdateItr.hasNext())
			shapesToUpdate.addAll(toUpdateItr.next());

		final List<ShapeData> updateCopy = new ArrayList<>(updateShapes);
		final List<ShapeData> toUpdateCopy = new ArrayList<>(shapesToUpdate);

		// Synchronize shapes
		for (final ShapeData updated : updateShapes) {
			for (final ShapeData toUpdate : shapesToUpdate) {
				if (updated.getId() == toUpdate.getId()) {
					ROIConverters.synchronizeShapeData(updated, toUpdate);
					updateCopy.remove(updated);
					toUpdateCopy.remove(toUpdate);
					break;
				}
			}
		}

		// Remove shapes without equivalent
		for (final ShapeData remove : toUpdateCopy)
			roiToUpdate.removeShapeData(remove);

		// Add new shapes
		for (final ShapeData add : updateCopy)
			roiToUpdate.addShapeData(add);
	}

}
