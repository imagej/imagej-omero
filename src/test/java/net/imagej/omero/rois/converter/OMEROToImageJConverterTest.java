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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.omero.DefaultOMEROService;
import net.imagej.omero.DefaultOMEROSession;
import net.imagej.omero.OMEROService;
import net.imagej.omero.rois.OMEROMask;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.Line;
import net.imglib2.roi.geom.real.PointMask;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.Polyline;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;

import mockit.Expectations;
import mockit.Mocked;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
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
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TagAnnotationData;
import omero.model.AffineTransformI;
import omero.model.PolygonI;
import omero.model.Shape;
import omero.model.ShapeAnnotationLink;
import omero.model.TagAnnotation;
import omero.model.TagAnnotationI;

/**
 * Tests OMERO to ImageJ Roi converters.
 *
 * @author Alison Walter
 */
public class OMEROToImageJConverterTest {

	private ConvertService convertService;

	@Mocked
	private Gateway gateway;

	@Mocked
	private DefaultOMEROSession session;

	@Mocked
	private DefaultOMEROService omeroService;

	@Mocked
	private MetadataFacility metadataFacility;

	@Mocked
	private SecurityContext securityContext;

	@Before
	public void setUp() {
		final Context context = new Context(ConvertService.class,
			OMEROService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- Test Shape Converters --

	@Test
	public void testEllipse() throws ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final EllipseData e = new EllipseData(0, 0, 2, 4);
		e.asIObject().setId(omero.rtypes.rlong(10));
		setUpMethodCalls(e);
		final Ellipsoid<?> c = convertService.convert(e, Ellipsoid.class);

		assertTrue(c.boundaryType() == BoundaryType.CLOSED);

		assertEquals(c.center().getDoublePosition(0), 0, 0);
		assertEquals(c.center().getDoublePosition(1), 0, 0);
		assertEquals(c.semiAxisLength(0), 2, 0);
		assertEquals(c.semiAxisLength(1), 4, 0);

		assertTrue(c.test(new RealPoint(new double[] { 1, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 2.3, 0.5 })));
	}

	@Test
	public void testLine() {
		final LineData l = new LineData(1, 1, 3.5, 3.5);
		final Line<?> c = convertService.convert(l, Line.class);

		assertTrue(c.boundaryType() == BoundaryType.CLOSED);

		assertEquals(c.endpointOne().getDoublePosition(0), 1, 0);
		assertEquals(c.endpointOne().getDoublePosition(1), 1, 0);
		assertEquals(c.endpointTwo().getDoublePosition(0), 3.5, 0);
		assertEquals(c.endpointTwo().getDoublePosition(1), 3.5, 0);

		assertTrue(c.test(new RealPoint(new double[] { 1.25, 1.25 })));
		assertFalse(c.test(new RealPoint(new double[] { 2, 2.05 })));
	}

	@Test
	public void testMask() {
		// Bits
		// 0 0 0 1
		// 1 0 0 0
		// 1 0 0 1
		// 1 1 1 1
		final MaskData m = new MaskData(0, 0, 4, 4, new byte[] { 24, -97 });
		final RealMask c = convertService.convert(m, RealMask.class);

		assertTrue(c instanceof OMEROMask);
		assertTrue(c.boundaryType() == BoundaryType.UNSPECIFIED);

		final OMEROMask w = (OMEROMask) c;

		assertTrue(w.test(new RealPoint(new double[] { 0, 1 })));
		assertFalse(w.test(new RealPoint(new double[] { 2, 2 })));
	}

	@Test
	public void testPoint() {
		final PointData p = new PointData(12, 15.25);
		final PointMask c = convertService.convert(p, PointMask.class);

		assertEquals(c.getDoublePosition(0), 12, 0);
		assertEquals(c.getDoublePosition(1), 15.25, 0);

		assertTrue(c.test(new RealPoint(new double[] { 12, 15.25 })));
		assertFalse(c.test(new RealPoint(new double[] { 12, 15 })));
	}

	@Test
	public void testPolygon() {
		final List<Point2D.Double> points = new ArrayList<>();
		points.add(new Point2D.Double(0, 0));
		points.add(new Point2D.Double(0, 10));
		points.add(new Point2D.Double(5, 15));
		points.add(new Point2D.Double(10, 10));
		points.add(new Point2D.Double(10, 0));
		final PolygonData p = new PolygonData(points);
		p.asIObject().setId(omero.rtypes.rlong(10));
		final Polygon2D<?> c = convertService.convert(p, Polygon2D.class);

		assertTrue(c.boundaryType() == BoundaryType.CLOSED);

		assertEquals(c.numVertices(), points.size());
		for (int i = 0; i < c.numVertices(); i++) {
			assertEquals(c.vertex(i).getDoublePosition(0), points.get(i).getX(), 0);
			assertEquals(c.vertex(i).getDoublePosition(1), points.get(i).getY(), 0);
		}

		assertTrue(c.test(new RealPoint(new double[] { 5, 5 })));
		assertFalse(c.test(new RealPoint(new double[] { 11, 10 })));
	}

	@Test
	public void testPolyline() {
		final List<Point2D.Double> points = new ArrayList<>();
		points.add(new Point2D.Double(0, 0));
		points.add(new Point2D.Double(3, 6));
		points.add(new Point2D.Double(6, 9));
		final PolylineData p = new PolylineData(points);
		final Polyline<?> c = convertService.convert(p, Polyline.class);

		assertTrue(c.boundaryType() == BoundaryType.CLOSED);

		assertEquals(c.numVertices(), points.size());
		for (int i = 0; i < c.numVertices(); i++) {
			assertEquals(c.vertex(i).getDoublePosition(0), points.get(i).getX(), 0);
			assertEquals(c.vertex(i).getDoublePosition(1), points.get(i).getY(), 0);
		}

		assertTrue(c.test(new RealPoint(new double[] { 4, 7 })));
		assertFalse(c.test(new RealPoint(new double[] { 1, 1 })));
	}

	@Test
	public void testRectangle() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final RectangleData r = new RectangleData(10, 10, 15, 20);
		r.asIObject().setId(omero.rtypes.rlong(10));
		setUpMethodCalls(r);
		final Box<?> c = convertService.convert(r, Box.class);

		assertTrue(c.boundaryType() == BoundaryType.CLOSED);

		assertEquals(c.center().getDoublePosition(0), 17.5, 0);
		assertEquals(c.center().getDoublePosition(1), 20, 0);
		assertEquals(c.sideLength(0), 15, 0);
		assertEquals(c.sideLength(1), 20, 0);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30 })));
		assertFalse(c.test(new RealPoint(new double[] { 9, 21 })));
	}

	@Test
	public void testSimpleTransformedShape() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();
		final RectangleData r = new RectangleData(0, 0, 50, 10);
		r.asIObject().setId(omero.rtypes.rlong(10));
		final omero.model.AffineTransform transform = new AffineTransformI();
		transform.setA00(omero.rtypes.rdouble(1));
		transform.setA01(omero.rtypes.rdouble(0));
		transform.setA02(omero.rtypes.rdouble(100));
		transform.setA10(omero.rtypes.rdouble(0));
		transform.setA11(omero.rtypes.rdouble(1));
		transform.setA12(omero.rtypes.rdouble(100));
		r.setTransform(transform);

		roi.addShapeData(r);
		setUpMethodCalls(r);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.boundaryType(), BoundaryType.CLOSED);

		assertTrue(c.test(new RealPoint(new double[] { 130, 105 })));
		assertFalse(c.test(new RealPoint(new double[] { 50, 0 })));
	}

	@Test
	public void testTransformedShape() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();
		final RectangleData r = new RectangleData(95, 85, 10, 30);
		r.asIObject().setId(omero.rtypes.rlong(10));
		final omero.model.AffineTransform transform = new AffineTransformI();
		transform.setA00(omero.rtypes.rdouble(Math.cos(Math.PI / 4)));
		transform.setA01(omero.rtypes.rdouble(-Math.sin(Math.PI / 4)));
		transform.setA02(omero.rtypes.rdouble(-100 * Math.cos(Math.PI / 4) + -100 *
			-Math.sin(Math.PI / 4)));
		transform.setA10(omero.rtypes.rdouble(Math.sin(Math.PI / 4)));
		transform.setA11(omero.rtypes.rdouble(Math.cos(Math.PI / 4)));
		transform.setA12(omero.rtypes.rdouble(-100 * Math.sin(Math.PI / 4) + -100 *
			Math.cos(Math.PI / 4)));
		r.setTransform(transform);

		setUpMethodCalls(r);
		roi.addShapeData(r);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.boundaryType(), BoundaryType.CLOSED);

		assertTrue(c.test(new RealPoint(new double[] { 0, 0 })));
		assertTrue(c.test(new RealPoint(new double[] { -10.25, 9.5 })));
		assertFalse(c.test(new RealPoint(new double[] { 4.5, 14 })));
	}

	@Test
	public void testShapeWithAnnotation() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final List<Point2D.Double> points = new ArrayList<>();
		points.add(new Point2D.Double(0, 0));
		points.add(new Point2D.Double(0, 10));
		points.add(new Point2D.Double(5, 15));
		points.add(new Point2D.Double(10, 10));
		points.add(new Point2D.Double(10, 0));
		final PolygonData p = new PolygonData(points);
		p.asIObject().setId(omero.rtypes.rlong(10));

		final TagAnnotationI tag = new TagAnnotationI();
		tag.setDescription(omero.rtypes.rstring("boundaryType"));
		tag.setTextValue(omero.rtypes.rstring("unspecified"));

		((PolygonI) p.asIObject()).linkAnnotation(tag);

		setUpMethodCalls(p);
		final RealMask c = convertService.convert(p, RealMask.class);

		assertEquals(c.boundaryType(), BoundaryType.UNSPECIFIED);

		assertTrue(c.test(new RealPoint(new double[] { 0, 0 })));
		assertTrue(c.test(new RealPoint(new double[] { 0, 10 })));
		assertFalse(c.test(new RealPoint(new double[] { 5, 15 })));
		assertFalse(c.test(new RealPoint(new double[] { 10, 10 })));
		assertFalse(c.test(new RealPoint(new double[] { 10, 0 })));
	}

	// -- Test OMERORoi Converter --

	@Test
	public void testROISingleShape() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();
		final RectangleData r = new RectangleData(10, 10, 15, 20);
		r.asIObject().setId(omero.rtypes.rlong(10));
		r.setT(12);
		r.setC(2);
		roi.addShapeData(r);
		setUpMethodCalls(r);

		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 4);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 12, 2 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, 11, 2 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, 11.9, 2 })));
	}

	@Test
	public void testROITwoShapes() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();

		final RectangleData r = new RectangleData(10, 10, 15, 20);
		r.asIObject().setId(omero.rtypes.rlong(10));
		r.setZ(0);
		r.setT(0);
		roi.addShapeData(r);

		final EllipseData e = new EllipseData(0, 0, 2, 4);
		e.asIObject().setId(omero.rtypes.rlong(11));
		e.setZ(0);
		e.setT(0);
		roi.addShapeData(e);

		setUpMethodCalls(r, e);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 4);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 0, 0 })));
		assertTrue(c.test(new RealPoint(new double[] { 1, 1, 0, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 9, 21, 0, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 2.3, 0.5, 0, 0 })));
	}

	@Test
	public void testROITwoShapesTwoPlanes() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();

		final RectangleData r = new RectangleData(10, 10, 15, 20);
		r.asIObject().setId(omero.rtypes.rlong(10));
		r.setZ(0);
		r.setT(0);
		roi.addShapeData(r);

		final EllipseData e = new EllipseData(0, 0, 2, 4);
		e.asIObject().setId(omero.rtypes.rlong(11));
		e.setZ(3);
		e.setT(0);
		roi.addShapeData(e);

		setUpMethodCalls(r, e);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 4);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 0, 0 })));
		assertTrue(c.test(new RealPoint(new double[] { 1, 1, 3, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, 0, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 2.3, 0.5, 3, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 1, 1, 3, 0.1 })));
	}

	@Test
	public void test3DRectangle() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();
		final RectangleData r = new RectangleData(10, 10, 15, 20);
		r.asIObject().setId(omero.rtypes.rlong(10));
		r.setZ(0);
		final RectangleData rOne = new RectangleData(10, 10, 15, 20);
		rOne.asIObject().setId(omero.rtypes.rlong(11));
		rOne.setZ(1);
		final RectangleData rTwo = new RectangleData(10, 10, 15, 20);
		rTwo.asIObject().setId(omero.rtypes.rlong(12));
		rTwo.setZ(2);
		final RectangleData rThree = new RectangleData(10, 10, 15, 20);
		rThree.asIObject().setId(omero.rtypes.rlong(13));
		rThree.setZ(3);
		final RectangleData rFour = new RectangleData(10, 10, 15, 20);
		rFour.asIObject().setId(omero.rtypes.rlong(14));
		rFour.setZ(4);

		roi.addShapeData(r);
		roi.addShapeData(rOne);
		roi.addShapeData(rTwo);
		roi.addShapeData(rThree);
		roi.addShapeData(rFour);

		setUpMethodCalls(r, rOne, rTwo, rThree, rFour);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 3);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 1 })));
		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 1.5 })));
		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 3.25 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, -0.99 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, 4.01 })));
		assertFalse(c.test(new RealPoint(new double[] { 0, 0, 2 })));
	}

	@Test
	public void test5DRectangles() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();
		final RectangleData rZero = new RectangleData(0, 0, 4, 4);
		rZero.asIObject().setId(omero.rtypes.rlong(10));
		rZero.setZ(0);
		rZero.setT(0);
		rZero.setC(1);
		final RectangleData rOne = new RectangleData(0, 0, 4, 4);
		rOne.asIObject().setId(omero.rtypes.rlong(11));
		rOne.setZ(1);
		rOne.setT(0);
		rOne.setC(1);
		final RectangleData rTwo = new RectangleData(0, 0, 4, 4);
		rTwo.asIObject().setId(omero.rtypes.rlong(12));
		rTwo.setZ(0);
		rTwo.setT(1);
		rTwo.setC(1);
		final RectangleData rThree = new RectangleData(0, 0, 4, 4);
		rThree.asIObject().setId(omero.rtypes.rlong(13));
		rThree.setZ(1);
		rThree.setT(1);
		rThree.setC(1);

		roi.addShapeData(rZero);
		roi.addShapeData(rOne);
		roi.addShapeData(rTwo);
		roi.addShapeData(rThree);
		setUpMethodCalls(rZero, rOne, rTwo, rThree);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 5);

		assertTrue(c.test(new RealPoint(new double[] { 0, 0, 0.75, 0.2, 1 })));
		assertTrue(c.test(new RealPoint(new double[] { 0, 0, 0.1, 0.6, 1 })));
		assertTrue(c.test(new RealPoint(new double[] { 0, 0, 0.9, 0.8, 1 })));
		assertTrue(c.test(new RealPoint(new double[] { 0, 0, 0.05, 0.3, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 0, 0, 0.75, 0.2, 1.1 })));
		assertFalse(c.test(new RealPoint(new double[] { 0, 0, 1.2, 0.6, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 0, 0, 0.9, -0.1, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 4.05, 1, 1, 1, 1 })));
	}

	@Test
	public void testMultiDRectangles() throws ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final ROIData roi = new ROIData();
		final RectangleData rZero = new RectangleData(0, 0, 4, 4);
		rZero.asIObject().setId(omero.rtypes.rlong(10));
		rZero.setZ(2);
		rZero.setT(3);
		final RectangleData rOne = new RectangleData(0, 0, 4, 4);
		rOne.asIObject().setId(omero.rtypes.rlong(11));
		rOne.setC(1);
		final RectangleData rTwo = new RectangleData(10, 10, 4, 4);
		rTwo.asIObject().setId(omero.rtypes.rlong(12));
		rTwo.setZ(0);
		rTwo.setT(1);
		rTwo.setC(1);

		roi.addShapeData(rZero);
		roi.addShapeData(rOne);
		roi.addShapeData(rTwo);
		setUpMethodCalls(rZero, rOne, rTwo);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 5);

		assertTrue(c.test(new RealPoint(new double[] { 2, 2, 2, 3, 20 })));
		assertTrue(c.test(new RealPoint(new double[] { 2, 2, 10, 4, 1 })));
		assertTrue(c.test(new RealPoint(new double[] { 12, 12, 0, 1, 1 })));
		assertTrue(c.test(new RealPoint(new double[] { 12, 12, 0.25, 1.25, 1 })));
		assertTrue(c.test(new RealPoint(new double[] { 2, 2, 0.25, 1.25, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 12, 12, 2, 3, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 1, 1, 100, 300, 1.4 })));
		assertFalse(c.test(new RealPoint(new double[] { 3, 3, 1, 1, 2 })));
	}

	// -- Helper methods --

	private void setUpMethodCalls(final ShapeData... shapes)
		throws ExecutionException, DSOutOfServiceException, DSAccessException
	{
		new Expectations() {

			{
				for (int i = 0; i < shapes.length; i++) {
					omeroService.session();
					result = session;
					session.getGateway();
					result = gateway;
					gateway.getFacility(MetadataFacility.class);
					result = metadataFacility;
					session.getSecurityContext();
					result = securityContext;
					metadataFacility.getAnnotations(securityContext, shapes[i]);
					result = getAnnotations(shapes[i]);
				}
			}
		};
	}

	private List<AnnotationData> getAnnotations(final ShapeData s) {
		final List<ShapeAnnotationLink> sals = ((Shape) s.asIObject())
			.copyAnnotationLinks();
		final List<AnnotationData> ad = new ArrayList<>();
		for (final ShapeAnnotationLink sal : sals) {
			if (sal.getChild() instanceof TagAnnotation) {
				ad.add(new TagAnnotationData((TagAnnotation) sal.getChild()));
			}
		}
		return ad;
	}
}
