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

import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.MaskPredicate;

import org.scijava.log.LogService;

import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.AnnotationData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TagAnnotationData;
import omero.model.AffineTransformI;
import omero.model.TagAnnotation;
import omero.model.TagAnnotationI;

/**
 * Utility class for working with ROI converters.
 *
 * @author Alison Walter
 */
public class ROIConverters {

	/** String identifiers for boundary imagej boundary behaviors */
	public final static String CLOSED_BOUNDARY_TEXT = "ij-bt:[C]";
	public final static String OPEN_BOUNDARY_TEXT = "ij-bt:[O]";
	public final static String UNSPECIFIED_BOUNDARY_TEXT = "ij-bt:[U]";

	/** Description value of imagej-omero version tags */
	public final static String IJO_VERSION_DESC = "ij-omero-version";

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
	public static <S extends ShapeData> BoundaryType boundaryType(final S shape,
		final OMEROSession session, final LogService log)
	{
		List<AnnotationData> annotations;
		try {
			final MetadataFacility proxy = session.getGateway().getFacility(
				MetadataFacility.class);
			annotations = proxy.getAnnotations(session.getSecurityContext(), shape);
		}
		catch (final DSOutOfServiceException | ExecutionException
				| DSAccessException exc)
		{
			log.warn("Error encountered when attempting to retrieve annotations for" +
				" boundary behavior, defaulting to closed boundary behavior.", exc);
			return BoundaryType.CLOSED;
		}

		if (annotations == null || annotations.isEmpty())
			return BoundaryType.CLOSED;
		for (final AnnotationData ad : annotations) {
			if (ad instanceof TagAnnotationData && ((TagAnnotationData) ad)
				.getDescription().equals("boundaryType"))
			{
				final TagAnnotationData t = (TagAnnotationData) ad;
				final String type = t.getTagValue();
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

	/**
	 * Creates/Retrieves a {@link TagAnnotation} which contains the boundary
	 * behavior of this Mask.
	 *
	 * @param mask a {@link MaskPredicate} whose boundary behavior will be
	 *          represented in the annotation
	 * @param omero an {@link OMEROService} for retrieving the tag
	 * @param log a {@link LogService} to record any issues
	 * @return an annotation which contains information about the masks boundary
	 *         behavior
	 */
	public static <L> TagAnnotationI getAnnotation(final MaskPredicate<L> mask,
		final OMEROService omero, final LogService log)
	{
		TagAnnotationI tag = null;
		try {
			tag = omero.getAnnotation("imagej:boundaryType", mask.boundaryType()
				.toString().toLowerCase());
		}
		catch (ServerError | ExecutionException | DSOutOfServiceException
				| DSAccessException exc)
		{
			log.error("Cannot retrieve/create tag", exc);
		}
		return tag;
	}

	public static String createBoundaryTypeString(final BoundaryType bt) {
		if (bt == BoundaryType.OPEN) return OPEN_BOUNDARY_TEXT;
		else if (bt == BoundaryType.UNSPECIFIED) return UNSPECIFIED_BOUNDARY_TEXT;
		else return CLOSED_BOUNDARY_TEXT;
	}
}
