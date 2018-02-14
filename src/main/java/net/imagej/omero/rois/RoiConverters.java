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

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.BoundaryType;

import omero.model.AffineTransformI;

/**
 * Utility class for working with ROI converters.
 *
 * @author Alison Walter
 */
public class RoiConverters {

	/** String identifiers for boundary imagej boundary behaviors */
	public final static String CLOSED_BOUNDARY_TEXT = "ij-bt:[C]";
	public final static String OPEN_BOUNDARY_TEXT = "ij-bt:[O]";
	public final static String UNSPECIFIED_BOUNDARY_TEXT = "ij-bt:[U]";

	/** Description value of imagej-omero version tags */
	public final static String IJO_VERSION_DESC = "ij-omero-version";

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
	 * @param transformToSource an AffineTransform2D to be converted
	 * @return an OMERO AffineTransform with the same transformation as the
	 *         AffineTransform2D
	 */
	public static omero.model.AffineTransform createAffine(
		final AffineTransform2D transformToSource)
	{
		final AffineTransform2D transformFromSource = transformToSource.inverse();
		final omero.model.AffineTransform transform = new AffineTransformI();
		transform.setA00(omero.rtypes.rdouble(transformFromSource.get(0, 0)));
		transform.setA01(omero.rtypes.rdouble(transformFromSource.get(0, 1)));
		transform.setA02(omero.rtypes.rdouble(transformFromSource.get(0, 2)));
		transform.setA10(omero.rtypes.rdouble(transformFromSource.get(1, 0)));
		transform.setA11(omero.rtypes.rdouble(transformFromSource.get(1, 1)));
		transform.setA12(omero.rtypes.rdouble(transformFromSource.get(1, 2)));
		return transform;
	}

	public static String createBoundaryTypeString(final BoundaryType bt) {
		if (bt == BoundaryType.OPEN) return OPEN_BOUNDARY_TEXT;
		else if (bt == BoundaryType.UNSPECIFIED) return UNSPECIFIED_BOUNDARY_TEXT;
		else return CLOSED_BOUNDARY_TEXT;
	}
}
