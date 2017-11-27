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

import io.scif.FormatException;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.omero.OMEROFormat.Metadata;
import net.imagej.omero.OMEROLocation;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.omero.rois.OMEROMask;
import net.imagej.table.Table;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.MaskPredicate;
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
import org.scijava.Optional;
import org.scijava.Priority;
import org.scijava.convert.ConvertService;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import Ice.AsyncResult;
import Ice.ByteSeqHolder;
import Ice.Callback;
import Ice.Callback_Object_ice_flushBatchRequests;
import Ice.Callback_Object_ice_getConnection;
import Ice.Callback_Object_ice_id;
import Ice.Callback_Object_ice_ids;
import Ice.Callback_Object_ice_invoke;
import Ice.Callback_Object_ice_isA;
import Ice.Callback_Object_ice_ping;
import Ice.Communicator;
import Ice.Connection;
import Ice.EncodingVersion;
import Ice.Endpoint;
import Ice.EndpointSelectionType;
import Ice.Exception;
import Ice.Identity;
import Ice.LocatorPrx;
import Ice.ObjectPrx;
import Ice.OperationMode;
import Ice.RouterPrx;
import Ice.UserException;
import IceInternal.Functional_BoolCallback;
import IceInternal.Functional_GenericCallback1;
import IceInternal.Functional_VoidCallback;
import omero.RLong;
import omero.RType;
import omero.ServerError;
import omero.client;
import omero.api.Callback_IMetadata_countAnnotationsUsedNotOwned;
import omero.api.Callback_IMetadata_countSpecifiedAnnotations;
import omero.api.Callback_IMetadata_getTaggedObjectsCount;
import omero.api.Callback_IMetadata_loadAnnotation;
import omero.api.Callback_IMetadata_loadAnnotations;
import omero.api.Callback_IMetadata_loadAnnotationsUsedNotOwned;
import omero.api.Callback_IMetadata_loadChannelAcquisitionData;
import omero.api.Callback_IMetadata_loadInstrument;
import omero.api.Callback_IMetadata_loadLogFiles;
import omero.api.Callback_IMetadata_loadSpecifiedAnnotations;
import omero.api.Callback_IMetadata_loadSpecifiedAnnotationsLinkedTo;
import omero.api.Callback_IMetadata_loadTagContent;
import omero.api.Callback_IMetadata_loadTagSets;
import omero.api.IMetadataPrx;
import omero.api.RawPixelsStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.EllipseData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.LineData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PointData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.PolylineData;
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TableData;
import omero.grid.Param;
import omero.model.AffineTransformI;
import omero.model.Annotation;
import omero.model.IObject;
import omero.model.Image;
import omero.model.Instrument;
import omero.model.LogicalChannel;
import omero.model.Pixels;
import omero.model.PolygonI;
import omero.model.Shape;
import omero.model.ShapeAnnotationLink;
import omero.model.TagAnnotationI;
import omero.sys.Parameters;

/**
 * Tests OMERO to ImageJ Roi converters.
 *
 * @author Alison Walter
 */
public class OMEROToImageJConverterTest {

	private ConvertService convertService;

	private TestOMEROService omeroService;

	@Before
	public void setUp() {
		final Context context = new Context(ConvertService.class,
			TestOMEROService.class);
		convertService = context.service(ConvertService.class);
		omeroService = context.service(TestOMEROService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- Test Shape Converters --

	@Test
	public void testEllipse() {
		final EllipseData e = new EllipseData(0, 0, 2, 4);
		e.asIObject().setId(omero.rtypes.rlong(10));
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
	public void testRectangle() {
		final RectangleData r = new RectangleData(10, 10, 15, 20);
		r.asIObject().setId(omero.rtypes.rlong(10));
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
	public void testSimpleTransformedShape() {
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
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.boundaryType(), BoundaryType.CLOSED);

		assertTrue(c.test(new RealPoint(new double[] { 130, 105 })));
		assertFalse(c.test(new RealPoint(new double[] { 50, 0 })));
	}

	@Test
	public void testTransformedShape() {
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

		roi.addShapeData(r);
		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.boundaryType(), BoundaryType.CLOSED);

		assertTrue(c.test(new RealPoint(new double[] { 0, 0 })));
		assertTrue(c.test(new RealPoint(new double[] { -10.25, 9.5 })));
		assertFalse(c.test(new RealPoint(new double[] { 4.5, 14 })));
	}

	@Test
	public void testShapeWithAnnotation() {
		final List<Point2D.Double> points = new ArrayList<>();
		points.add(new Point2D.Double(0, 0));
		points.add(new Point2D.Double(0, 10));
		points.add(new Point2D.Double(5, 15));
		points.add(new Point2D.Double(10, 10));
		points.add(new Point2D.Double(10, 0));
		final PolygonData p = new PolygonData(points);
		p.asIObject().setId(omero.rtypes.rlong(10));
		omeroService.setShape(p); // needed for mock classes only!

		final TagAnnotationI tag = new TagAnnotationI();
		tag.setName(omero.rtypes.rstring("boundaryType"));
		tag.setTextValue(omero.rtypes.rstring("unspecified"));

		((PolygonI) p.asIObject()).linkAnnotation(tag);

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
	public void testROISingleShape() {
		final ROIData roi = new ROIData();
		final RectangleData r = new RectangleData(10, 10, 15, 20);
		r.asIObject().setId(omero.rtypes.rlong(10));
		r.setT(12);
		r.setC(2);
		roi.addShapeData(r);

		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 4);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 12, 2 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, 11, 2 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, 11.9, 2 })));
	}

	@Test
	public void testROITwoShapes() {
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

		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 4);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 0, 0 })));
		assertTrue(c.test(new RealPoint(new double[] { 1, 1, 0, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 9, 21, 0, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 2.3, 0.5, 0, 0 })));
	}

	@Test
	public void testROITwoShapesTwoPlanes() {
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

		final RealMask c = convertService.convert(roi, RealMask.class);

		assertEquals(c.numDimensions(), 4);

		assertTrue(c.test(new RealPoint(new double[] { 20, 30, 0, 0 })));
		assertTrue(c.test(new RealPoint(new double[] { 1, 1, 3, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 20, 30, 0, 1 })));
		assertFalse(c.test(new RealPoint(new double[] { 2.3, 0.5, 3, 0 })));
		assertFalse(c.test(new RealPoint(new double[] { 1, 1, 3, 0.1 })));
	}

	@Test
	public void test3DRectangle() {
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
	public void test5DRectangles() {
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
	public void testMultiDRectangles() {
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

	// -- Mock Classes --

	@Plugin(type = Service.class, priority = Priority.LAST)
	public static class TestOMEROService extends AbstractService implements
		OMEROService, Optional
	{

		private ShapeData s;

		@Override
		public Param getJobParam(final ModuleItem<?> item) {
			return null;
		}

		@Override
		public RType prototype(final Class<?> type) {
			return null;
		}

		@Override
		public RType toOMERO(final Object value) {
			return null;
		}

		@Override
		public Object toOMERO(final client client, final Object value)
			throws ServerError, IOException, PermissionDeniedException,
			CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
			DSAccessException
		{
			return null;
		}

		@Override
		public Object toImageJ(final client client, final RType value,
			final Class<?> type) throws ServerError, IOException,
			PermissionDeniedException, CannotCreateSessionException,
			SecurityException, DSOutOfServiceException, ExecutionException,
			DSAccessException
		{
			return null;
		}

		@Override
		public Dataset downloadImage(final client client, final long imageID)
			throws ServerError, IOException
		{
			return null;
		}

		@Override
		public long uploadImage(final client client, final Dataset dataset)
			throws ServerError, IOException
		{
			return 0;
		}

		@Override
		public long uploadTable(final OMEROLocation credentials, final String name,
			final Table<?, ?> imageJTable, final long imageID) throws ServerError,
			PermissionDeniedException, CannotCreateSessionException,
			ExecutionException, DSOutOfServiceException, DSAccessException
		{
			return 0;
		}

		@Override
		public TableData convertOMEROTable(final Table<?, ?> imageJTable) {
			return null;
		}

		@Override
		public Table<?, ?> downloadTable(final OMEROLocation credentials,
			final long tableID) throws ServerError, PermissionDeniedException,
			CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
			DSAccessException
		{
			return null;
		}

		@Override
		public OMEROSession session(final OMEROLocation location) {
			return null;
		}

		@Override
		public OMEROSession session() {
			return new TestOMEROSession(s);
		}

		@Override
		public OMEROSession createSession(final OMEROLocation location) {
			return null;
		}

		@Override
		public void removeSession(final OMEROSession session) {}

		public void setShape(final ShapeData s) {
			this.s = s;
		}

		@Override
		public List<MaskPredicate<?>> downloadROIs(final OMEROLocation credentials,
			final long imageID) throws ServerError, PermissionDeniedException,
			CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
			DSAccessException
		{
			return null;
		}

		@Override
		public long[] uploadROIs(final OMEROLocation credentials,
			final List<MaskPredicate<?>> ijROIs, final long imageID)
			throws ServerError, PermissionDeniedException,
			CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
			DSAccessException
		{
			return null;
		}

	}

	public static class TestOMEROSession implements OMEROSession {

		private final ShapeData s;

		public TestOMEROSession(final ShapeData s) {
			this.s = s;
		}

		@Override
		public client getClient() {
			return null;
		}

		@Override
		public ServiceFactoryPrx getSession() {
			return null;
		}

		@Override
		public SecurityContext getSecurityContext() {
			return new TestSecurityContext(0);
		}

		@Override
		public ExperimenterData getExperimenter() {
			return null;
		}

		@Override
		public Gateway getGateway() {
			return new TestGateway(s);
		}

		@Override
		public String getSessionID() {
			return null;
		}

		@Override
		public Pixels loadPixels(final Metadata meta) throws ServerError {
			return null;
		}

		@Override
		public Image loadImage(final Metadata meta) throws ServerError {
			return null;
		}

		@Override
		public long loadPixelsID(final Metadata meta) throws ServerError {
			return 0;
		}

		@Override
		public String loadImageName(final Metadata meta) throws ServerError {
			return null;
		}

		@Override
		public RawPixelsStorePrx openPixels(final Metadata meta)
			throws ServerError
		{
			return null;
		}

		@Override
		public RawPixelsStorePrx createPixels(final Metadata meta)
			throws ServerError, FormatException
		{
			return null;
		}

		@Override
		public void close() {}

	}

	public static class TestGateway extends Gateway {

		private final ShapeData s;

		public TestGateway(final ShapeData s) {
			super(null, null, null, false);
			this.s = s;
		}

		@Override
		public IMetadataPrx getMetadataService(final SecurityContext ctx)
			throws DSOutOfServiceException
		{
			return new TestIMetadataPrx(s);
		}

	}

	public static class TestSecurityContext extends SecurityContext {

		public TestSecurityContext(final long groupID) {
			super(groupID);
		}

	}

	public static class TestIMetadataPrx implements IMetadataPrx {

		private final ShapeData s;

		public TestIMetadataPrx(final ShapeData s) {
			super();
			this.s = s;
		}

		@Override
		public AsyncResult begin_ice_flushBatchRequests() {
			return null;
		}

		@Override
		public AsyncResult begin_ice_flushBatchRequests(final Callback arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_flushBatchRequests(
			final Callback_Object_ice_flushBatchRequests arg0)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_flushBatchRequests(
			final Functional_VoidCallback arg0,
			final Functional_GenericCallback1<Exception> arg1,
			final Functional_BoolCallback arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_getConnection() {
			return null;
		}

		@Override
		public AsyncResult begin_ice_getConnection(final Callback arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_getConnection(
			final Callback_Object_ice_getConnection arg0)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_getConnection(
			final Functional_GenericCallback1<Connection> arg0,
			final Functional_GenericCallback1<Exception> arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_id() {
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(final Map<String, String> arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(final Callback arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(final Callback_Object_ice_id arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(final Map<String, String> arg0,
			final Callback arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(final Map<String, String> arg0,
			final Callback_Object_ice_id arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(
			final Functional_GenericCallback1<String> arg0,
			final Functional_GenericCallback1<Exception> arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(
			final Functional_GenericCallback1<String> arg0,
			final Functional_GenericCallback1<Exception> arg1,
			final Functional_BoolCallback arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(final Map<String, String> arg0,
			final Functional_GenericCallback1<String> arg1,
			final Functional_GenericCallback1<Exception> arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_id(final Map<String, String> arg0,
			final Functional_GenericCallback1<String> arg1,
			final Functional_GenericCallback1<Exception> arg2,
			final Functional_BoolCallback arg3)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids() {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(final Map<String, String> arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(final Callback arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(final Callback_Object_ice_ids arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(final Map<String, String> arg0,
			final Callback arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(final Map<String, String> arg0,
			final Callback_Object_ice_ids arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(
			final Functional_GenericCallback1<String[]> arg0,
			final Functional_GenericCallback1<Exception> arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(
			final Functional_GenericCallback1<String[]> arg0,
			final Functional_GenericCallback1<Exception> arg1,
			final Functional_BoolCallback arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(final Map<String, String> arg0,
			final Functional_GenericCallback1<String[]> arg1,
			final Functional_GenericCallback1<Exception> arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ids(final Map<String, String> arg0,
			final Functional_GenericCallback1<String[]> arg1,
			final Functional_GenericCallback1<Exception> arg2,
			final Functional_BoolCallback arg3)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final Map<String, String> arg3)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2, final Callback arg3)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final Callback_Object_ice_invoke arg3)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final Map<String, String> arg3, final Callback arg4)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final Map<String, String> arg3, final Callback_Object_ice_invoke arg4)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final FunctionalCallback_Object_ice_invoke_Response arg3,
			final Functional_GenericCallback1<Exception> arg4)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final FunctionalCallback_Object_ice_invoke_Response arg3,
			final Functional_GenericCallback1<Exception> arg4,
			final Functional_BoolCallback arg5)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final Map<String, String> arg3,
			final FunctionalCallback_Object_ice_invoke_Response arg4,
			final Functional_GenericCallback1<Exception> arg5)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_invoke(final String arg0,
			final OperationMode arg1, final byte[] arg2,
			final Map<String, String> arg3,
			final FunctionalCallback_Object_ice_invoke_Response arg4,
			final Functional_GenericCallback1<Exception> arg5,
			final Functional_BoolCallback arg6)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Map<String, String> arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0, final Callback arg1) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Callback_Object_ice_isA arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Map<String, String> arg1, final Callback arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Map<String, String> arg1, final Callback_Object_ice_isA arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Functional_BoolCallback arg1,
			final Functional_GenericCallback1<Exception> arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Functional_BoolCallback arg1,
			final Functional_GenericCallback1<Exception> arg2,
			final Functional_BoolCallback arg3)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Map<String, String> arg1, final Functional_BoolCallback arg2,
			final Functional_GenericCallback1<Exception> arg3)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_isA(final String arg0,
			final Map<String, String> arg1, final Functional_BoolCallback arg2,
			final Functional_GenericCallback1<Exception> arg3,
			final Functional_BoolCallback arg4)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping() {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Map<String, String> arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Callback arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Callback_Object_ice_ping arg0) {
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Map<String, String> arg0,
			final Callback arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Map<String, String> arg0,
			final Callback_Object_ice_ping arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Functional_VoidCallback arg0,
			final Functional_GenericCallback1<Exception> arg1)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Functional_VoidCallback arg0,
			final Functional_GenericCallback1<Exception> arg1,
			final Functional_BoolCallback arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Map<String, String> arg0,
			final Functional_VoidCallback arg1,
			final Functional_GenericCallback1<Exception> arg2)
		{
			return null;
		}

		@Override
		public AsyncResult begin_ice_ping(final Map<String, String> arg0,
			final Functional_VoidCallback arg1,
			final Functional_GenericCallback1<Exception> arg2,
			final Functional_BoolCallback arg3)
		{
			return null;
		}

		@Override
		public void end_ice_flushBatchRequests(final AsyncResult arg0) {}

		@Override
		public Connection end_ice_getConnection(final AsyncResult arg0) {
			return null;
		}

		@Override
		public String end_ice_id(final AsyncResult arg0) {
			return null;
		}

		@Override
		public String[] end_ice_ids(final AsyncResult arg0) {
			return null;
		}

		@Override
		public boolean end_ice_invoke(final ByteSeqHolder arg0,
			final AsyncResult arg1)
		{
			return false;
		}

		@Override
		public boolean end_ice_isA(final AsyncResult arg0) {
			return false;
		}

		@Override
		public void end_ice_ping(final AsyncResult arg0) {}

		@Override
		public ObjectPrx ice_adapterId(final String arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_batchDatagram() {
			return null;
		}

		@Override
		public ObjectPrx ice_batchOneway() {
			return null;
		}

		@Override
		public ObjectPrx ice_collocationOptimized(final boolean arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_compress(final boolean arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_connectionCached(final boolean arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_connectionId(final String arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_context(final Map<String, String> arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_datagram() {
			return null;
		}

		@Override
		public ObjectPrx ice_encodingVersion(final EncodingVersion arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_endpointSelection(final EndpointSelectionType arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_endpoints(final Endpoint[] arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_facet(final String arg0) {
			return null;
		}

		@Override
		public void ice_flushBatchRequests() {}

		@Override
		public String ice_getAdapterId() {
			return null;
		}

		@Override
		public Connection ice_getCachedConnection() {
			return null;
		}

		@Override
		public Communicator ice_getCommunicator() {
			return null;
		}

		@Override
		public Connection ice_getConnection() {
			return null;
		}

		@Override
		public String ice_getConnectionId() {
			return null;
		}

		@Override
		public Map<String, String> ice_getContext() {
			return null;
		}

		@Override
		public EncodingVersion ice_getEncodingVersion() {
			return null;
		}

		@Override
		public EndpointSelectionType ice_getEndpointSelection() {
			return null;
		}

		@Override
		public Endpoint[] ice_getEndpoints() {
			return null;
		}

		@Override
		public String ice_getFacet() {
			return null;
		}

		@Override
		public Identity ice_getIdentity() {
			return null;
		}

		@Override
		public int ice_getInvocationTimeout() {
			return 0;
		}

		@Override
		public LocatorPrx ice_getLocator() {
			return null;
		}

		@Override
		public int ice_getLocatorCacheTimeout() {
			return 0;
		}

		@Override
		public RouterPrx ice_getRouter() {
			return null;
		}

		@Override
		public String ice_id() {
			return null;
		}

		@Override
		public String ice_id(final Map<String, String> arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_identity(final Identity arg0) {
			return null;
		}

		@Override
		public String[] ice_ids() {
			return null;
		}

		@Override
		public String[] ice_ids(final Map<String, String> arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_invocationTimeout(final int arg0) {
			return null;
		}

		@Override
		public boolean ice_invoke(final String arg0, final OperationMode arg1,
			final byte[] arg2, final ByteSeqHolder arg3)
		{
			return false;
		}

		@Override
		public boolean ice_invoke(final String arg0, final OperationMode arg1,
			final byte[] arg2, final ByteSeqHolder arg3,
			final Map<String, String> arg4)
		{
			return false;
		}

		@Override
		public boolean ice_isA(final String arg0) {
			return false;
		}

		@Override
		public boolean ice_isA(final String arg0, final Map<String, String> arg1) {
			return false;
		}

		@Override
		public boolean ice_isBatchDatagram() {
			return false;
		}

		@Override
		public boolean ice_isBatchOneway() {
			return false;
		}

		@Override
		public boolean ice_isCollocationOptimized() {
			return false;
		}

		@Override
		public boolean ice_isConnectionCached() {
			return false;
		}

		@Override
		public boolean ice_isDatagram() {
			return false;
		}

		@Override
		public boolean ice_isOneway() {
			return false;
		}

		@Override
		public boolean ice_isPreferSecure() {
			return false;
		}

		@Override
		public boolean ice_isSecure() {
			return false;
		}

		@Override
		public boolean ice_isTwoway() {
			return false;
		}

		@Override
		public ObjectPrx ice_locator(final LocatorPrx arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_locatorCacheTimeout(final int arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_oneway() {
			return null;
		}

		@Override
		public void ice_ping() {}

		@Override
		public void ice_ping(final Map<String, String> arg0) {}

		@Override
		public ObjectPrx ice_preferSecure(final boolean arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_router(final RouterPrx arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_secure(final boolean arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_timeout(final int arg0) {
			return null;
		}

		@Override
		public ObjectPrx ice_twoway() {
			return null;
		}

		@Override
		public List<LogicalChannel> loadChannelAcquisitionData(final List<Long> ids)
			throws ServerError
		{
			return null;
		}

		@Override
		public List<LogicalChannel> loadChannelAcquisitionData(final List<Long> ids,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids) {
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Callback_IMetadata_loadChannelAcquisitionData __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadChannelAcquisitionData __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Functional_GenericCallback1<List<LogicalChannel>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Functional_GenericCallback1<List<LogicalChannel>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<LogicalChannel>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadChannelAcquisitionData(final List<Long> ids,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<LogicalChannel>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public List<LogicalChannel> end_loadChannelAcquisitionData(
			final AsyncResult __result) throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options)
			throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Callback_IMetadata_loadAnnotations __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadAnnotations __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotations(final String rootType,
			final List<Long> rootIds, final List<String> annotationTypes,
			final List<Long> annotatorIds, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> end_loadAnnotations(
			final AsyncResult __result) throws ServerError
		{
			return null;
		}

		@Override
		public List<Annotation> loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options) throws ServerError
		{
			return null;
		}

		@Override
		public List<Annotation> loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Callback_IMetadata_loadSpecifiedAnnotations __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadSpecifiedAnnotations __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public List<Annotation> end_loadSpecifiedAnnotations(
			final AsyncResult __result) throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> loadTagContent(final List<Long> ids,
			final Parameters options) throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> loadTagContent(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx)
			throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options, final Callback_IMetadata_loadTagContent __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Callback_IMetadata_loadTagContent __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagContent(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> end_loadTagContent(
			final AsyncResult __result) throws ServerError
		{
			return null;
		}

		@Override
		public List<IObject> loadTagSets(final Parameters options)
			throws ServerError
		{
			return null;
		}

		@Override
		public List<IObject> loadTagSets(final Parameters options,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options) {
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Callback_IMetadata_loadTagSets __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadTagSets __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadTagSets(final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public List<IObject> end_loadTagSets(final AsyncResult __result)
			throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, Long> getTaggedObjectsCount(final List<Long> ids,
			final Parameters options) throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, Long> getTaggedObjectsCount(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx)
			throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options,
			final Callback_IMetadata_getTaggedObjectsCount __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Callback_IMetadata_getTaggedObjectsCount __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options,
			final Functional_GenericCallback1<Map<Long, Long>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options,
			final Functional_GenericCallback1<Map<Long, Long>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, Long>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_getTaggedObjectsCount(final List<Long> ids,
			final Parameters options, final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, Long>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public Map<Long, Long> end_getTaggedObjectsCount(final AsyncResult __result)
			throws ServerError
		{
			return null;
		}

		@Override
		public RLong countSpecifiedAnnotations(final String annotationType,
			final List<String> include, final List<String> exclude,
			final Parameters options) throws ServerError
		{
			return null;
		}

		@Override
		public RLong countSpecifiedAnnotations(final String annotationType,
			final List<String> include, final List<String> exclude,
			final Parameters options, final Map<String, String> __ctx)
			throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Callback_IMetadata_countSpecifiedAnnotations __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx,
			final Callback_IMetadata_countSpecifiedAnnotations __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countSpecifiedAnnotations(
			final String annotationType, final List<String> include,
			final List<String> exclude, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public RLong end_countSpecifiedAnnotations(final AsyncResult __result)
			throws ServerError
		{
			return null;
		}

		@Override
		public List<Annotation> loadAnnotation(final List<Long> annotationIds)
			throws ServerError
		{
			return null;
		}

		@Override
		public List<Annotation> loadAnnotation(final List<Long> annotationIds,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds) {
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Callback_IMetadata_loadAnnotation __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadAnnotation __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotation(final List<Long> annotationIds,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<Annotation>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public List<Annotation> end_loadAnnotation(final AsyncResult __result)
			throws ServerError
		{
			return null;
		}

		@Override
		public Instrument loadInstrument(final long id) throws ServerError {
			return null;
		}

		@Override
		public Instrument loadInstrument(final long id,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id) {
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Callback_IMetadata_loadInstrument __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadInstrument __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Functional_GenericCallback1<Instrument> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Functional_GenericCallback1<Instrument> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<Instrument> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadInstrument(final long id,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<Instrument> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public Instrument end_loadInstrument(final AsyncResult __result)
			throws ServerError
		{
			return null;
		}

		@Override
		public List<IObject> loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID) throws ServerError
		{
			return null;
		}

		@Override
		public List<IObject> loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Callback_IMetadata_loadAnnotationsUsedNotOwned __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadAnnotationsUsedNotOwned __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<List<IObject>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public List<IObject> end_loadAnnotationsUsedNotOwned(
			final AsyncResult __result) throws ServerError
		{
			return null;
		}

		@Override
		public RLong countAnnotationsUsedNotOwned(final String annotationType,
			final long userID) throws ServerError
		{
			return null;
		}

		@Override
		public RLong countAnnotationsUsedNotOwned(final String annotationType,
			final long userID, final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Callback_IMetadata_countAnnotationsUsedNotOwned __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx,
			final Callback_IMetadata_countAnnotationsUsedNotOwned __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_countAnnotationsUsedNotOwned(
			final String annotationType, final long userID,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<RLong> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public RLong end_countAnnotationsUsedNotOwned(final AsyncResult __result)
			throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, List<Annotation>> loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options)
			throws ServerError
		{
			if (s == null) return Collections.emptyMap();

			final List<ShapeAnnotationLink> l = ((Shape) s.asIObject())
				.copyAnnotationLinks();
			if (l.isEmpty()) return Collections.emptyMap();

			final List<Annotation> a = new ArrayList<>();
			for (final ShapeAnnotationLink sal : l)
				a.add(sal.getChild());

			final HashMap<Long, List<Annotation>> m = new HashMap<>();
			m.put(rootNodeIds.get(0), a);
			return m;
		}

		@Override
		public Map<Long, List<Annotation>> loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Map<String, String> __ctx) throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Map<String, String> __ctx, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Callback_IMetadata_loadSpecifiedAnnotationsLinkedTo __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Map<String, String> __ctx,
			final Callback_IMetadata_loadSpecifiedAnnotationsLinkedTo __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Functional_GenericCallback1<Map<Long, List<Annotation>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Functional_GenericCallback1<Map<Long, List<Annotation>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<Annotation>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadSpecifiedAnnotationsLinkedTo(
			final String annotationType, final List<String> include,
			final List<String> exclude, final String rootNodeType,
			final List<Long> rootNodeIds, final Parameters options,
			final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<Annotation>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public Map<Long, List<Annotation>> end_loadSpecifiedAnnotationsLinkedTo(
			final AsyncResult __result) throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> loadLogFiles(final String rootType,
			final List<Long> ids) throws ServerError
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> loadLogFiles(final String rootType,
			final List<Long> ids, final Map<String, String> __ctx)
			throws ServerError
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids, final Map<String, String> __ctx)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids, final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids, final Map<String, String> __ctx,
			final Callback __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids, final Callback_IMetadata_loadLogFiles __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids, final Map<String, String> __ctx,
			final Callback_IMetadata_loadLogFiles __cb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids, final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb)
		{
			return null;
		}

		@Override
		public AsyncResult begin_loadLogFiles(final String rootType,
			final List<Long> ids, final Map<String, String> __ctx,
			final Functional_GenericCallback1<Map<Long, List<IObject>>> __responseCb,
			final Functional_GenericCallback1<UserException> __userExceptionCb,
			final Functional_GenericCallback1<Exception> __exceptionCb,
			final Functional_BoolCallback __sentCb)
		{
			return null;
		}

		@Override
		public Map<Long, List<IObject>> end_loadLogFiles(final AsyncResult __result)
			throws ServerError
		{
			return null;
		}

	}

}
