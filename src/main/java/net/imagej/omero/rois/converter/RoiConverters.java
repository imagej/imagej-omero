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

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.MaskPredicate;

import omero.gateway.model.ShapeData;
import omero.model.AffineTransformI;
import omero.model.Annotation;
import omero.model.Shape;
import omero.model.TagAnnotation;
import omero.model.TagAnnotationI;

/**
 * Utility class for working with ROI converters.
 *
 * @author Alison Walter
 */
public class RoiConverters {

	/**
	 * Gets the boundary behavior of this shape. If this shape was previously an
	 * ImageJ mask it will retrieve the boundary behavior from an annotation; if
	 * not, it will default to {@code CLOSED} boundary behavior.
	 *
	 * @param shape OMERO shape which will be checked for a boundaryType
	 *          annotation
	 * @return if {@code shape} was previously converted to an ImageJ mask, then
	 *         its previous boundary behavior is returned otherwise {@code CLOSED}
	 *         is returned.
	 */
	public static <S extends ShapeData> BoundaryType boundaryType(final S shape) {
		final List<Annotation> l = ((Shape) shape.asIObject())
			.linkedAnnotationList();
		for (int i = 0; i < l.size(); i++) {
			if (TagAnnotation.class.isInstance(l.get(i)) && l.get(i).getName()
				.getValue().equals("boundaryType"))
			{
				final TagAnnotation t = (TagAnnotation) l.get(i);
				final String type = t.getTextValue().getValue();
				if (type.toLowerCase().equals("open")) return BoundaryType.OPEN;
				else if (type.toLowerCase().equals("unspecified"))
					return BoundaryType.UNSPECIFIED;
				else return BoundaryType.CLOSED;
			}
		}
		// If no such tag found, use Closed
		return BoundaryType.CLOSED;
	}

	/**
	 * Converts an OMERO {@link omero.model.AffineTransform AffineTransform} to an
	 * ImgLib2 {@link AffineTransform2D}.
	 *
	 * @param transformFromSource the OMERO AffineTransform to be converted
	 * @return an AffineTransform2D with the same transformation as the OMERO
	 *         AffineTransform
	 */
	public static AffineGet createAffine(
		final omero.model.AffineTransform transformFromSource)
	{
		final AffineTransform2D transformToSource = new AffineTransform2D();
		transformToSource.set(new double[] { transformFromSource.getA00()
			.getValue(), transformFromSource.getA01().getValue(), transformFromSource
				.getA02().getValue(), transformFromSource.getA10().getValue(),
			transformFromSource.getA11().getValue(), transformFromSource.getA12()
				.getValue() });
		return transformToSource.inverse();
	}

	/**
	 * Converts an ImgLib2 {@link AffineTransform2D} to an OMERO
	 * {@link omero.model.AffineTransform AffineTransform}.
	 *
	 * @param transformFromSource an AffineTransform2D to be converted
	 * @return an OMERO AffineTransform with the same transformation as the
	 *         AffineTransform2D
	 */
	public static omero.model.AffineTransform createAffine(
		final AffineTransform2D transformFromSource)
	{
		final omero.model.AffineTransform transform = new AffineTransformI();
		transform.setA00(omero.rtypes.rdouble(transformFromSource.get(0, 0)));
		transform.setA01(omero.rtypes.rdouble(transformFromSource.get(0, 1)));
		transform.setA02(omero.rtypes.rdouble(transformFromSource.get(0, 2)));
		transform.setA10(omero.rtypes.rdouble(transformFromSource.get(1, 0)));
		transform.setA11(omero.rtypes.rdouble(transformFromSource.get(1, 1)));
		transform.setA12(omero.rtypes.rdouble(transformFromSource.get(1, 2)));
		return transform;
	}

	/**
	 * Creates a {@link TagAnnotation} which contains the boundary behavior of
	 * this Mask.
	 *
	 * @param mask a {@link MaskPredicate} whose boundary behavior will be
	 *          represented in the annotation
	 * @return an annotation which contains information about the masks boundary
	 *         behavior
	 */
	public static <L> TagAnnotation createAnnotation(
		final MaskPredicate<L> mask)
	{
		final TagAnnotation tag = new TagAnnotationI();
		tag.setName(omero.rtypes.rstring("boundaryType"));
		tag.setTextValue(omero.rtypes.rstring(mask.boundaryType().toString()
			.toLowerCase()));
		return tag;
	}
}
