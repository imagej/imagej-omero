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

import java.awt.Point;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.omero.OMEROSession;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.BoundaryType;

import org.scijava.log.LogService;

import ome.formats.model.UnitsFactory;
import ome.model.units.BigResult;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.model.AnnotationData;
import omero.gateway.model.EllipseData;
import omero.gateway.model.LineData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PointData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.PolylineData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.ShapeSettingsData;
import omero.gateway.model.TagAnnotationData;
import omero.gateway.model.TextData;
import omero.model.AffineTransformI;
import omero.model.Shape;

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

	public static String createBoundaryTypeString(final BoundaryType bt) {
		if (bt == BoundaryType.OPEN) return OPEN_BOUNDARY_TEXT;
		else if (bt == BoundaryType.UNSPECIFIED) return UNSPECIFIED_BOUNDARY_TEXT;
		else return CLOSED_BOUNDARY_TEXT;
	}

	/**
	 * Checks if the two given {@link ShapeData} objects are equivalent. This
	 * means they are the same shape, have the same shape characteristics, have
	 * the same text, and are one the same ZTC plane.
	 *
	 * @param shapeOne first shape to compare with
	 * @param shapeTwo second shape to compare with
	 * @return true if shapes are equivalent, false otherwise
	 */
	public static boolean shapeDataEquals(final ShapeData shapeOne,
		final ShapeData shapeTwo)
	{
		if (!shapeOne.getClass().isInstance(shapeTwo)) return false;
		if (shapeOne.getC() != shapeTwo.getC() || shapeOne.getT() != shapeTwo
			.getT() || shapeOne.getZ() != shapeTwo.getZ()) return false;
		if (shapeOne instanceof EllipseData) return ellipseDataEquals(
			(EllipseData) shapeOne, (EllipseData) shapeTwo);
		if (shapeOne instanceof LineData) return lineDataEquals((LineData) shapeOne,
			(LineData) shapeTwo);
		if (shapeOne instanceof MaskData) return maskDataEquals((MaskData) shapeOne,
			(MaskData) shapeTwo);
		if (shapeOne instanceof PointData) return pointDataEquals(
			(PointData) shapeOne, (PointData) shapeTwo);
		if (shapeOne instanceof PolygonData) return polygonDataEquals(
			(PolygonData) shapeOne, (PolygonData) shapeTwo);
		if (shapeOne instanceof PolylineData) return polylineDataEquals(
			(PolylineData) shapeOne, (PolylineData) shapeTwo);
		if (shapeOne instanceof RectangleData) return rectangleDataEquals(
			(RectangleData) shapeOne, (RectangleData) shapeTwo);
		if (shapeOne instanceof TextData) return textDataEquals((TextData) shapeOne,
			(TextData) shapeTwo);
		return false;
	}

	/**
	 * Updates the {@code oldSettings} to be equivalent to
	 * {@code updatedSettings}.
	 *
	 * @param updatedSettings {@link ShapeSettingsData} to transfer
	 * @param oldSettings {@link ShapeSettingsData} to update
	 */
	public static void synchronizeShapeSettings(
		final ShapeSettingsData updatedSettings,
		final ShapeSettingsData oldSettings)
	{
		oldSettings.setFillRule(updatedSettings.getFillRule());
		oldSettings.setFill(updatedSettings.getFill());
		oldSettings.setStroke(updatedSettings.getStroke());
		oldSettings.setStrokeDashArray(updatedSettings.getStrokeDashArray());
		oldSettings.setFontFamily(updatedSettings.getFontFamily());
		oldSettings.setFontStyle(updatedSettings.getFontStyle());
		oldSettings.setMarkerStart(updatedSettings.getMarkerStart());
		oldSettings.setMarkerEnd(updatedSettings.getMarkerEnd());

		try {
			oldSettings.setStrokeWidth(updatedSettings.getStrokeWidth(
				UnitsFactory.Shape_StrokeWidth));
		}
		catch (final BigResult exc) {
			// Do nothing
		}

		try {
			oldSettings.setFontSize(updatedSettings.getFontSize(
				UnitsFactory.Shape_FontSize));
		}
		catch (final BigResult exc) {
			// Do nothing
		}
	}

	/**
	 * Synchronizes {@code oldShape} to be equivalent to {@code updatedShape},
	 * with the exception of ID. If the {@link ShapeData} are not the same type an
	 * exception is thrown.
	 *
	 * @param updatedShape the {@link ShapeData} whose state will be copied
	 * @param oldShape the {@link ShapeData} who will be updated
	 */
	public static void synchronizeShapeData(final ShapeData updatedShape,
		final ShapeData oldShape)
	{
		if (oldShape == null || !updatedShape.getClass().isInstance(oldShape))
			throw new IllegalArgumentException("Cannot synchronize shapes!");

		synchronizeShapeSettings(updatedShape.getShapeSettings(), oldShape
			.getShapeSettings());

		// Set position on Ice objects
		final Shape oldIceShape = (Shape) oldShape.asIObject();
		final Shape updatedIceShape = (Shape) updatedShape.asIObject();
		oldIceShape.setTheZ(updatedIceShape.getTheZ());
		oldIceShape.setTheT(updatedIceShape.getTheT());
		oldIceShape.setTheC(updatedIceShape.getTheC());

		// Set transform
		oldShape.setTransform(updatedShape.getTransform());

		if (updatedShape instanceof EllipseData) synchronizeEllipseData(
			(EllipseData) updatedShape, (EllipseData) oldShape);
		else if (updatedShape instanceof LineData) synchronizeLineData(
			(LineData) updatedShape, (LineData) oldShape);
		else if (updatedShape instanceof MaskData) synchronizeMaskData(
			(MaskData) updatedShape, (MaskData) oldShape);
		else if (updatedShape instanceof PointData) synchronizePointData(
			(PointData) updatedShape, (PointData) oldShape);
		else if (updatedShape instanceof PolygonData) synchronizePolygonData(
			(PolygonData) updatedShape, (PolygonData) oldShape);
		else if (updatedShape instanceof PolylineData) synchronizePolylineData(
			(PolylineData) updatedShape, (PolylineData) oldShape);
		else if (updatedShape instanceof RectangleData) synchronizeRectangleData(
			(RectangleData) updatedShape, (RectangleData) oldShape);
		else if (updatedShape instanceof TextData) synchronizeTextData(
			(TextData) updatedShape, (TextData) oldShape);
		else throw new IllegalArgumentException("Unsupported type: " + updatedShape
			.getClass());
	}

	// -- Helper methods --

	private static boolean ellipseDataEquals(final EllipseData one,
		final EllipseData two)
	{
		return one.getText().equals(two.getText()) && one.getX() == two.getX() &&
			one.getY() == two.getY() && one.getRadiusX() == two.getRadiusX() && one
				.getRadiusY() == two.getRadiusY();
	}

	private static boolean lineDataEquals(final LineData one,
		final LineData two)
	{
		return one.getText().equals(two.getText()) && one.getX1() == two.getX1() &&
			one.getX2() == two.getX2() && one.getY1() == two.getY1() && one
				.getY2() == two.getY2();
	}

	private static boolean maskDataEquals(final MaskData one,
		final MaskData two)
	{
		if (!one.getText().equals(two.getText()) || one.getX() != two.getX() || one
			.getY() != two.getY() || one.getHeight() != two.getHeight() || one
				.getWidth() != two.getWidth()) return false;

		final byte[] oneValues = one.getMask();
		final byte[] twoValues = two.getMask();
		if (oneValues.length != twoValues.length) return false;

		for (int i = 0; i < oneValues.length; i++) {
			if (oneValues[i] != twoValues[i]) return false;
		}

		return true;
	}

	private static boolean pointDataEquals(final PointData one,
		final PointData two)
	{
		return one.getText().equals(two.getText()) && one.getX() == two.getX() &&
			one.getY() == two.getY();
	}

	private static boolean polygonDataEquals(final PolygonData one,
		final PolygonData two)
	{
		final List<Point.Double> onePoints = one.getPoints();
		final List<Point.Double> twoPoints = two.getPoints();
		if (onePoints.size() != twoPoints.size() || !one.getText().equals(two
			.getText())) return false;

		for (int i = 0; i < onePoints.size(); i++) {
			if (onePoints.get(i).getX() != twoPoints.get(i).getX() || onePoints.get(i)
				.getY() != twoPoints.get(i).getY()) return false;
		}
		return true;
	}

	private static boolean polylineDataEquals(final PolylineData one,
		final PolylineData two)
	{
		final List<Point.Double> onePoints = one.getPoints();
		final List<Point.Double> twoPoints = two.getPoints();
		if (onePoints.size() != twoPoints.size() || !one.getText().equals(two
			.getText())) return false;

		for (int i = 0; i < onePoints.size(); i++) {
			if (onePoints.get(i).getX() != twoPoints.get(i).getX() || onePoints.get(i)
				.getY() != twoPoints.get(i).getY()) return false;
		}
		return true;
	}

	private static boolean rectangleDataEquals(final RectangleData one,
		final RectangleData two)
	{
		return one.getText().equals(two.getText()) && one.getX() == two.getX() &&
			one.getY() == two.getY() && one.getHeight() == two.getHeight() && one
				.getWidth() == two.getWidth();
	}

	private static boolean textDataEquals(final TextData one,
		final TextData two)
	{
		return one.getText().equals(two.getText()) && one.getX() == two.getX() &&
			one.getY() == two.getY();
	}

	private static void synchronizeEllipseData(final EllipseData current,
		final EllipseData old)
	{
		old.setX(current.getX());
		old.setY(current.getY());
		old.setRadiusX(current.getRadiusX());
		old.setRadiusY(current.getRadiusY());
		old.setText(current.getText());
	}

	private static void synchronizeLineData(final LineData current,
		final LineData old)
	{
		old.setX1(current.getX1());
		old.setY1(current.getY1());
		old.setX2(current.getX2());
		old.setY2(current.getY2());
		old.setText(current.getText());
	}

	private static void synchronizeMaskData(final MaskData current,
		final MaskData old)
	{
		old.setX(current.getX());
		old.setY(current.getY());
		old.setWidth(current.getWidth());
		old.setHeight(current.getHeight());
		old.setMask(current.getMask());
		old.setText(current.getText());
	}

	private static void synchronizePointData(final PointData current,
		final PointData old)
	{
		old.setX(current.getX());
		old.setY(current.getY());
		old.setText(current.getText());
	}

	private static void synchronizePolygonData(final PolygonData current,
		final PolygonData old)
	{
		old.setPoints(current.getPoints());
		old.setText(current.getText());
	}

	private static void synchronizePolylineData(final PolylineData current,
		final PolylineData old)
	{
		old.setPoints(current.getPoints());
		old.setText(current.getText());
	}

	private static void synchronizeRectangleData(final RectangleData current,
		final RectangleData old)
	{
		old.setX(current.getX());
		old.setY(current.getY());
		old.setWidth(current.getWidth());
		old.setHeight(current.getHeight());
		old.setText(current.getText());
	}

	private static void synchronizeTextData(final TextData current,
		final TextData old)
	{
		old.setX(current.getX());
		old.setY(current.getY());
		old.setText(current.getText());
	}
}
