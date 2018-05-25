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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import net.imagej.axis.Axes;
import net.imagej.axis.DefaultTypedAxis;
import net.imagej.axis.TypedAxis;
import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.project.OMEROZTCProjectedRealMaskRealInterval;
import net.imagej.omero.roi.project.OMEROZTCProjectedRealMaskToShapeData;
import net.imagej.omero.roi.project.ProjectedOMERORealMaskToShapeData;
import net.imagej.omero.roi.rectangle.ClosedOMERORectangle;
import net.imagej.omero.roi.transform.RealTransformUnaryCompositeRealMaskRealIntervalToShapeData;
import net.imagej.omero.roi.transform.TransformedOMERORealMaskRealInterval;
import net.imagej.omero.roi.transform.TransformedOMERORealMaskRealIntervalToShapeData;
import net.imagej.space.DefaultTypedSpace;
import net.imagej.space.TypedSpace;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.Bounds;
import net.imglib2.roi.Operators.RealTransformMaskOperator;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.ClosedWritableBox;
import net.imglib2.roi.geom.real.ClosedWritableEllipsoid;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.OpenWritableBox;
import net.imglib2.roi.geom.real.OpenWritableEllipsoid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.util.DefaultTreeNode;
import org.scijava.util.TreeNode;

import omero.gateway.model.EllipseData;
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.model.Event;
import omero.model.EventI;
import omero.model.Roi;
import omero.model.RoiI;
import omero.model.Shape;

/**
 * Tests converting {@link TreeNode} to {@link ROIData}.
 *
 * @author Alison Walter
 */
public class TreeNodeToROIDataConversionTest {

	public ConvertService convert;
	public String imagejOmeroVersion;

	@Before
	public void setUp() {
		final Context c = new Context(OMEROService.class, ConvertService.class,
			LogService.class);
		convert = c.getService(ConvertService.class);
		imagejOmeroVersion = c.getService(OMEROService.class).getVersion();
	}

	@After
	public void tearDown() {
		convert.getContext().dispose();
	}

	@Test
	public void testConverterMatching() {
		final RectangleData rect = new RectangleData(0, 0, 5, 5);
		rect.setZ(0);
		rect.setT(0);
		final Box b = new ClosedWritableBox(new double[] { 0, 0 }, new double[] { 5,
			5 });
		final RealMaskRealInterval bTransform = b.transform(
			new AffineTransform2D());
		final RealMask rectTransform = new TransformedOMERORealMaskRealInterval<>(
			new ClosedOMERORectangle(rect), new Bounds.RealTransformRealInterval(
				new ClosedOMERORectangle(rect), new AffineTransform2D()),
			new RealTransformMaskOperator(new AffineTransform2D()));
		final ROIData rd = new ROIData();
		rd.addShapeData(rect);
		final OMEROROICollection orc = new DefaultOMEROROICollection(null, rd,
			convert);
		final OMEROROIElement ore = (OMEROROIElement) orc.children().get(0);
		final TypedSpace<TypedAxis> omeroSpace = new DefaultTypedSpace(
			new DefaultTypedAxis(Axes.X), new DefaultTypedAxis(Axes.Y),
			new DefaultTypedAxis(Axes.Z), new DefaultTypedAxis(Axes.TIME),
			new DefaultTypedAxis(Axes.CHANNEL));
		final OMERORealMask<?> orm = ore.projectIntoSpace(omeroSpace);
		final RealMask rm = new OMEROZTCProjectedRealMaskRealInterval(b, 0, 0, 0);

		// check transform matching
		assertTrue(convert.getHandler(rectTransform,
			ShapeData.class) instanceof TransformedOMERORealMaskRealIntervalToShapeData);
		assertTrue(convert.getHandler(bTransform,
			ShapeData.class) instanceof RealTransformUnaryCompositeRealMaskRealIntervalToShapeData);

		// check TreeNode matching
		assertTrue(convert.getHandler(orc,
			ROIData.class) instanceof OMEROROICollectionToROIData);
		assertTrue(convert.getHandler(ore,
			ROIData.class) instanceof OMEROROIElementToROIData);
		assertTrue(convert.getHandler(new DefaultTreeNode<>(b, null),
			ROIData.class) instanceof TreeNodeMaskPredicateToROIData);

		// check projection matching
		assertTrue(convert.getHandler(orm,
			ShapeData.class) instanceof ProjectedOMERORealMaskToShapeData);
		assertTrue(convert.getHandler(rm,
			ShapeData.class) instanceof OMEROZTCProjectedRealMaskToShapeData);
	}

	@Test
	public void testDefaultTreeNodeToROIData() {
		final Box b = new OpenWritableBox(new double[] { 10, 10 }, new double[] {
			22, 35.5 });
		final TreeNode<Box> dn = new DefaultTreeNode<>(b, null);
		final ROIData omeroRoi = convert.convert(dn, ROIData.class);

		// HACK: The new shape doesn't have a ROICoordinate, so it got added to
		// ROIData with a null key. This causes omeroRoi.getShapeCount() to
		// throw NPE. Setting the ROICoordinate to -1, -1 would actually set the
		// Z and T to 0. See ShapeData#setZ(). If no ZTC is specified, we want
		// the shape to have Z=T=C=-1.
		int count = 0;
		final Iterator<List<ShapeData>> itr = omeroRoi.getIterator();
		while (itr.hasNext())
			count += itr.next().size();

		assertEquals(1, count);
		assertTrue(omeroRoi.getIterator().next().get(0) instanceof RectangleData);

		assertShapeDataConversionCorrect((RectangleData) omeroRoi.getIterator()
			.next().get(0), -1, -1, -1, ROIConverters.OPEN_BOUNDARY_TEXT, -1);

		assertROIDataConversionCorrect(omeroRoi, -1);
	}

	@Test
	public void testOMERORoiCollectionToROIData() {
		final RectangleData rdOne = new RectangleData(10, 10, 30, 40);
		rdOne.setId(22);
		rdOne.setZ(0);
		rdOne.setT(0);
		final RectangleData rdTwo = new RectangleData(10, 10, 30, 40);
		rdTwo.setId(23);
		rdTwo.setZ(0);
		rdTwo.setT(1);
		final RectangleData rdThree = new RectangleData(10, 10, 30, 40);
		rdThree.setId(24);
		rdThree.setZ(0);
		rdThree.setT(2);

		// NB: Create ROIData as though it came from server. This means it has
		// loaded shapes but unloaded annotations
		final Roi r = new RoiI(33, true);
		final Event e = new EventI(24, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();
		r.addShape((Shape) rdOne.asIObject());
		r.addShape((Shape) rdTwo.asIObject());
		r.addShape((Shape) rdThree.asIObject());
		final ROIData rd = new ROIData(r);

		final OMEROROICollection orc = new DefaultOMEROROICollection(null, rd,
			convert);

		final ROIData crd = convert.convert(orc, ROIData.class);

		assertROIDataConversionCorrect(crd, rd.getId());

		assertEquals(3, crd.getShapeCount());
		assertTrue(crd.getShapes(0, 0).get(0) instanceof RectangleData);
		assertTrue(crd.getShapes(0, 1).get(0) instanceof RectangleData);
		assertTrue(crd.getShapes(0, 2).get(0) instanceof RectangleData);

		assertShapeDataConversionCorrect((RectangleData) crd.getShapes(0, 0).get(0),
			rdOne.getZ(), rdOne.getT(), rdOne.getC(),
			ROIConverters.CLOSED_BOUNDARY_TEXT, rdOne.getId());
		assertShapeDataConversionCorrect((RectangleData) crd.getShapes(0, 1).get(0),
			rdTwo.getZ(), rdTwo.getT(), rdTwo.getC(),
			ROIConverters.CLOSED_BOUNDARY_TEXT, rdTwo.getId());
		assertShapeDataConversionCorrect((RectangleData) crd.getShapes(0, 2).get(0),
			rdThree.getZ(), rdThree.getT(), rdThree.getC(),
			ROIConverters.CLOSED_BOUNDARY_TEXT, rdThree.getId());
	}

	@Test
	public void testOMERORoiElementToROIData() {
		final RectangleData rdOne = new RectangleData(10, 10, 30, 40);
		rdOne.setId(22);
		rdOne.setZ(0);
		rdOne.setT(0);
		final RectangleData rdTwo = new RectangleData(10, 10, 30, 40);
		rdTwo.setId(23);
		rdTwo.setZ(0);
		rdTwo.setT(1);
		final RectangleData rdThree = new RectangleData(10, 10, 30, 40);
		rdThree.setId(24);
		rdThree.setZ(0);
		rdThree.setT(2);

		// NB: Create ROIData as though it came from server. This means it has
		// loaded shapes but unloaded annotations
		final Roi r = new RoiI(33, true);
		final Event e = new EventI(24, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();
		r.addShape((Shape) rdOne.asIObject());
		r.addShape((Shape) rdTwo.asIObject());
		r.addShape((Shape) rdThree.asIObject());
		final ROIData rd = new ROIData(r);

		final OMEROROICollection orc = new DefaultOMEROROICollection(null, rd,
			convert);
		final List<TreeNode<?>> children = orc.children();

		final ROIData crd = convert.convert(children.get(0), ROIData.class);

		assertROIDataConversionCorrect(crd, -1);
		assertEquals(1, crd.getShapeCount());
		assertTrue(crd.getIterator().next().get(0) instanceof RectangleData);

		assertShapeDataConversionCorrect((RectangleData) crd.getIterator().next()
			.get(0), rdOne.getZ(), rdOne.getT(), rdOne.getC(),
			ROIConverters.CLOSED_BOUNDARY_TEXT, rdOne.getId());
	}

	@Test
	public void testProjectedOMERORealMaskToROIData() {
		final RectangleData rect = new RectangleData(10, 10, 30, 40);
		rect.setId(22);
		rect.setZ(0);
		rect.setT(0);

		// NB: Create ROIData as though it came from server. This means it has
		// loaded shapes but unloaded annotations
		final Roi r = new RoiI(33, true);
		final Event e = new EventI(24, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();
		r.addShape((Shape) rect.asIObject());
		final ROIData rd = new ROIData(r);

		final OMEROROICollection orc = new DefaultOMEROROICollection(null, rd,
			convert);
		final TypedSpace<TypedAxis> omeroSpace = new DefaultTypedSpace(
			new DefaultTypedAxis(Axes.X), new DefaultTypedAxis(Axes.Y),
			new DefaultTypedAxis(Axes.Z), new DefaultTypedAxis(Axes.TIME),
			new DefaultTypedAxis(Axes.CHANNEL));
		final OMERORealMask<?> projected = ((OMEROROIElement) orc.children().get(0))
			.projectIntoSpace(omeroSpace);

		final ROIData crd = convert.convert(new DefaultTreeNode<>(projected, null),
			ROIData.class);

		assertROIDataConversionCorrect(crd, -1);
		assertEquals(1, crd.getShapeCount());
		assertTrue(crd.getIterator().next().get(0) instanceof RectangleData);

		assertShapeDataConversionCorrect((RectangleData) crd.getIterator().next()
			.get(0), rect.getZ(), rect.getT(), rect.getC(),
			ROIConverters.CLOSED_BOUNDARY_TEXT, rect.getId());
	}

	@Test
	public void testProjectedRealMaskToROIData() {
		final Ellipsoid e = new OpenWritableEllipsoid(new double[] { 24.5, 62 },
			new double[] { 3, 9.5 });
		final OMEROZTCProjectedRealMaskRealInterval projected =
			new OMEROZTCProjectedRealMaskRealInterval(e, 5, 22, 0);

		final ROIData crd = convert.convert(new DefaultTreeNode<>(projected, null),
			ROIData.class);

		// NB: count hack not needed since it was put into ROIData with non-null
		// ROICoordinate
		assertROIDataConversionCorrect(crd, -1);
		assertEquals(1, crd.getShapeCount());
		assertTrue(crd.getIterator().next().get(0) instanceof EllipseData);

		assertShapeDataConversionCorrect((EllipseData) crd.getIterator().next().get(
			0), projected.getZPosition(), projected.getTimePosition(), projected
				.getChannelPosition(), ROIConverters.OPEN_BOUNDARY_TEXT, -1);
	}

	@Test
	public void testRealTransformUnaryCompositeRealMaskRealIntervalToROIData() {
		final Ellipsoid e = new ClosedWritableEllipsoid(new double[] { 12, 12 },
			new double[] { 3, 5 });
		final AffineTransform2D rot = new AffineTransform2D();
		rot.rotate(Math.PI / 3);
		final RealMaskRealInterval rotE = e.transform(rot.inverse());
		final TreeNode<RealMaskRealInterval> dn = new DefaultTreeNode<>(rotE, null);

		final ROIData crd = convert.convert(dn, ROIData.class);
		// HACK: The new shape doesn't have a ROICoordinate, so it got added to
		// ROIData with a null key. This causes omeroRoi.getShapeCount() to
		// throw NPE. Setting the ROICoordinate to -1, -1 would actually set the
		// Z and T to 0. See ShapeData#setZ(). If no ZTC is specified, we want
		// the shape to have Z=T=C=-1.
		int count = 0;
		final Iterator<List<ShapeData>> itr = crd.getIterator();
		while (itr.hasNext())
			count += itr.next().size();

		assertROIDataConversionCorrect(crd, -1);
		assertEquals(1, count);
		assertTrue(crd.getIterator().next().get(0) instanceof EllipseData);

		final EllipseData ced = (EllipseData) crd.getIterator().next().get(0);
		assertShapeDataConversionCorrect(ced, -1, -1, -1,
			ROIConverters.CLOSED_BOUNDARY_TEXT, -1);

		// NB: OMERO transforms are fromSource and ImageJ transforms are toSource.
		// And we transformed the original ellipsoid with the inverse of rot.
		// Therefore, rot should be equivalent to the omero transform of ced.
		assertEquals(rot.get(0, 0), ced.getTransform().getA00().getValue(), 0);
		assertEquals(rot.get(0, 1), ced.getTransform().getA01().getValue(), 0);
		assertEquals(rot.get(0, 2), ced.getTransform().getA02().getValue(), 0);
		assertEquals(rot.get(1, 0), ced.getTransform().getA10().getValue(), 0);
		assertEquals(rot.get(1, 1), ced.getTransform().getA11().getValue(), 0);
		assertEquals(rot.get(1, 2), ced.getTransform().getA12().getValue(), 0);
	}

	@Test
	public void testTransformedOMERORealMaskRealIntervalToROIData() {
		final omero.model.AffineTransform transform =
			new omero.model.AffineTransformI();
		transform.setA00(omero.rtypes.rdouble(0));
		transform.setA01(omero.rtypes.rdouble(-1));
		transform.setA02(omero.rtypes.rdouble(0));
		transform.setA10(omero.rtypes.rdouble(1));
		transform.setA11(omero.rtypes.rdouble(0));
		transform.setA12(omero.rtypes.rdouble(0));
		final RectangleData rect = new RectangleData(13, 82, 90, 12);
		rect.setZ(0);
		rect.setT(0);
		rect.setId(222);
		rect.setTransform(transform);

		// NB: Create ROIData as though it came from server. This means it has
		// loaded shapes but unloaded annotations
		final Roi r = new RoiI(33, true);
		final Event e = new EventI(24, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();
		r.addShape((Shape) rect.asIObject());
		final ROIData rd = new ROIData(r);

		final OMEROROICollection orc = new DefaultOMEROROICollection(null, rd,
			convert);
		final ROIData crd = convert.convert(orc, ROIData.class);

		assertROIDataConversionCorrect(crd, rd.getId());
		assertEquals(1, crd.getShapeCount());
		assertTrue(crd.getIterator().next().get(0) instanceof RectangleData);

		final RectangleData cRect = (RectangleData) crd.getIterator().next().get(0);
		assertShapeDataConversionCorrect(cRect, rect.getZ(), rect.getT(), rect
			.getC(), ROIConverters.CLOSED_BOUNDARY_TEXT, rect.getId());

		assertEquals(transform.getA00().getValue(), cRect.getTransform().getA00()
			.getValue(), 0);
		assertEquals(transform.getA01().getValue(), cRect.getTransform().getA01()
			.getValue(), 0);
		assertEquals(transform.getA02().getValue(), cRect.getTransform().getA02()
			.getValue(), 0);
		assertEquals(transform.getA10().getValue(), cRect.getTransform().getA10()
			.getValue(), 0);
		assertEquals(transform.getA11().getValue(), cRect.getTransform().getA11()
			.getValue(), 0);
		assertEquals(transform.getA12().getValue(), cRect.getTransform().getA12()
			.getValue(), 0);
	}

	@Test
	public void testDoublyTransformedRealMaskToROIData() {
		final omero.model.AffineTransform transform =
			new omero.model.AffineTransformI();
		transform.setA00(omero.rtypes.rdouble(1));
		transform.setA01(omero.rtypes.rdouble(0.5));
		transform.setA02(omero.rtypes.rdouble(0));
		transform.setA10(omero.rtypes.rdouble(0));
		transform.setA11(omero.rtypes.rdouble(1));
		transform.setA12(omero.rtypes.rdouble(0));
		final EllipseData ed = new EllipseData(15, 16, 1, 3);
		ed.setZ(0);
		ed.setT(0);
		ed.setId(24);
		ed.setTransform(transform);

		// NB: Create ROIData as though it came from server. This means it has
		// loaded shapes but unloaded annotations
		final Roi r = new RoiI(33, true);
		final Event e = new EventI(24, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();
		r.addShape((Shape) ed.asIObject());
		final ROIData rd = new ROIData(r);

		final OMEROROICollection orc = new DefaultOMEROROICollection(null, rd,
			convert);

		// Now pretend you want to transform this OMERO Roi again
		final AffineTransform2D rotate = new AffineTransform2D();
		rotate.rotate(Math.PI / 4);
		final RealMaskRealInterval rmi = ((RealMaskRealInterval) orc.children().get(
			0).data()).transform(rotate.inverse());

		// You need to create a new DefaultTreeNode because performing operations
		// (transform, negate, and, etc.) on OMERO Rois does not modify the
		// underlying ROI. It creates a new wrapped version.
		final ROIData crd = convert.convert(new DefaultTreeNode<>(rmi, null),
			ROIData.class);

		assertROIDataConversionCorrect(crd, -1);
		assertEquals(1, crd.getShapeCount());
		assertTrue(crd.getIterator().next().get(0) instanceof EllipseData);

		final EllipseData ced = (EllipseData) crd.getIterator().next().get(0);
		assertShapeDataConversionCorrect((EllipseData) crd.getIterator().next().get(
			0), ed.getZ(), ed.getT(), ed.getC(), ROIConverters.CLOSED_BOUNDARY_TEXT,
			ed.getId());

		final AffineTransform2D targetTransform = ((AffineTransform2D) ROIConverters
			.createAffine(transform)).concatenate(rotate.inverse()).inverse();

		assertEquals(targetTransform.get(0, 0), ced.getTransform().getA00()
			.getValue(), 0);
		assertEquals(targetTransform.get(0, 1), ced.getTransform().getA01()
			.getValue(), 0);
		assertEquals(targetTransform.get(0, 2), ced.getTransform().getA02()
			.getValue(), 0);
		assertEquals(targetTransform.get(1, 0), ced.getTransform().getA10()
			.getValue(), 0);
		assertEquals(targetTransform.get(1, 1), ced.getTransform().getA11()
			.getValue(), 0);
		assertEquals(targetTransform.get(1, 2), ced.getTransform().getA12()
			.getValue(), 0);
	}

	// -- Helper methods --

	private void assertROIDataConversionCorrect(final ROIData converted,
		final long originalId)
	{
		assertEquals(originalId, converted.getId());
	}

	private void assertShapeDataConversionCorrect(final RectangleData converted,
		final int z, final int t, final int c, final String boundaryType,
		final long originalId)
	{
		checkIdZTC(converted, z, t, c, originalId);

		// check boundary type
		assertEquals(boundaryType, converted.getText());
	}

	private void assertShapeDataConversionCorrect(final EllipseData converted,
		final int z, final int t, final int c, final String boundaryType,
		final long originalId)
	{
		checkIdZTC(converted, z, t, c, originalId);

		// check boundary type
		assertEquals(boundaryType, converted.getText());
	}

	private void checkIdZTC(final ShapeData converted, final int z, final int t,
		final int c, final long originalId)
	{
		assertEquals(originalId, converted.getId());

		// check ZTC
		assertEquals(z, converted.getZ());
		assertEquals(t, converted.getT());
		assertEquals(c, converted.getC());
	}
}
