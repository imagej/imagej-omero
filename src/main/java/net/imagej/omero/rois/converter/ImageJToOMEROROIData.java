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

import java.util.List;
import java.util.function.Predicate;

import net.imagej.omero.rois.OMERORoi;
import net.imagej.omero.rois.OMEROShape;
import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.Operators;
import net.imglib2.roi.Operators.RealTransformMaskOperator;
import net.imglib2.roi.composite.CompositeMaskPredicate;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import omero.gateway.model.ROICoordinate;
import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;
import omero.model.RoiI;
import omero.model.Shape;

/**
 * Converts {@link MaskPredicate} to OMERO {@link ROIData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class ImageJToOMEROROIData extends
	AbstractConverter<MaskPredicate<?>, ROIData>
{

	@Parameter
	ConvertService convertService;

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

		final ROIData roi = new ROIData();
		assemble((MaskPredicate<?>) src, null, roi);

		// If this originated from OMERO, set the description and name
		if (src instanceof OMERORoi) {
			final OMERORoi or = (OMERORoi) src;
			((RoiI) roi.asIObject()).setName(or.getRoi().getName());
			((RoiI) roi.asIObject()).setDescription(or.getRoi().getDescription());
		}

		// TODO: create annotation with ImageJ version

		return (T) roi;
	}

	@Override
	public Class<ROIData> getOutputType() {
		return ROIData.class;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<MaskPredicate<?>> getInputType() {
		return (Class) MaskPredicate.class;
	}

	// -- Helper methods --

	private void toShape(final CompositeMaskPredicate<RealLocalizable> mno,
		final AffineTransform2D transform, final ROIData roi)
	{
		if (mno.operator() == Operators.OR) {
			final List<Predicate<?>> ops = mno.operands();
			for (final Predicate<?> m : ops) {
				assemble(m, transform, roi);
			}
		}
		else if (mno.operator() instanceof RealTransformMaskOperator) {
			final RealTransform affine = ((RealTransformMaskOperator) mno.operator())
				.getTransformToSource();
			if (affine instanceof AffineTransform2D) {
				if (transform == null) assemble(mno.operands().get(0),
					((AffineTransform2D) affine).inverse().copy(), roi);
				else assemble(mno.operands().get(0), ((AffineTransform2D) affine)
					.inverse().copy().preConcatenate(transform), roi);
			}
			else {
				throw new IllegalArgumentException(
					"Affine transform must be an instance of AffineTransform2D");
			}
		}
		else {
			throw new IllegalArgumentException("OMERO does not support " + mno
				.operator() + " with ROIs");
		}
	}

	@SuppressWarnings("unchecked")
	private void toShape(final MaskPredicate<?> m,
		final AffineTransform2D transform, final ROIData roi)
	{
		final ShapeData s = convertService.convert(m, ShapeData.class);
		// check if converter found
		if (s == null) throw new IllegalArgumentException("Cannot convert " + m
			.getClass() + " to OMERO ROI");

		if (m instanceof OMEROShape) {
			final ShapeData shape = ((OMEROShape<ShapeData>) m).getShape();
			// Ensure that this is uploaded to the server as a new Shape, and
			// does not update the old object
			((Shape) shape.asIObject()).setId(null);
		}
		else {
			// TODO: set to proper Z, T
			// It has to be set to a coordinate, otherwise this Shape's ROICoordinate
			// is null and it gets stored into the ROIData with a null key and cannot
			// be retrieved.
			final ROICoordinate coor = new ROICoordinate(0, 0);
			s.setROICoordinate(coor);
		}

		// check if transformed
		if (transform != null) s.setTransform(RoiConverters.createAffine(
			transform));
		roi.addShapeData(s);
	}

	@SuppressWarnings("unchecked")
	private void assemble(final Predicate<?> m, final AffineTransform2D transform,
		final ROIData roi)
	{
		if (!(m instanceof MaskPredicate)) throw new IllegalArgumentException(
			"Cannot convert " + m.getClass() + " to ROIData");
		// Convert
		if (m instanceof CompositeMaskPredicate) toShape(
			(CompositeMaskPredicate<RealLocalizable>) m, transform, roi);
		else toShape((MaskPredicate<?>) m, transform, roi);
	}

}
