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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import net.imagej.omero.OMEROException;
import net.imagej.omero.OMEROServer;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.omero.roi.transform.TransformedOMERORealMaskRealInterval;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.PointMask;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.util.TreeNode;

import mockit.Expectations;
import mockit.Mocked;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.ROIFacility;
import omero.gateway.model.EllipseData;
import omero.gateway.model.PointData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ROIResult;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TextData;
import omero.model.AffineTransformI;
import omero.model.Event;
import omero.model.EventI;
import omero.model.Roi;
import omero.model.RoiI;
import omero.model.Shape;

/**
 * Tests {@link OMEROSession#downloadROIs(long)}. Note, that the actual data
 * structure conversions are not tested here as they are tested elsewhere.
 *
 * @author Alison Walter
 */
public class DownloadROITest {

	private OMEROServer server;
	private OMEROService service;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Mocked
	private OMEROSession session;

	@Mocked
	private Gateway gateway;

	@Mocked
	private ROIFacility roiFac;

	@Before
	public void setup() {
		server = new OMEROServer("localhost", 4064);
		service = new Context(OMEROService.class, ConvertService.class).getService(
			OMEROService.class);
	}

	@After
	public void teardown() {
		service.context().dispose();
	}

	// -- test omeroService.downloadROIs(...) --

	@Test
	public void testDownloadSingleROI() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException
	{
		final EllipseData ed = new EllipseData(22, 22, 3, 5);
		final ROIResult rr = createROIResult(createROIData(ed));
		setUpMethodCalls(1, rr);

		final TreeNode<?> dn = session.downloadROIs(1);
		assertTrue(dn.children().get(0) instanceof OMEROROICollection);

		final OMEROROICollection orc = (OMEROROICollection) dn.children().get(0);
		final List<TreeNode<?>> children = orc.children();
		assertEquals(1, children.size());
		assertTrue(children.get(0) instanceof OMEROROIElement);

		final OMEROROIElement ore = (OMEROROIElement) children.get(0);
		assertTrue(ore.data() instanceof Ellipsoid);
	}

	@Test
	public void testDownloadMultipleROIData() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException
	{
		final EllipseData ed = new EllipseData(13, 15, 0.5, 6);
		final RectangleData rd = new RectangleData(4, 3.5, 90, 65.5);
		final PointData pd = new PointData(14, 6);
		final ROIResult rr = createROIResult(createROIData(ed), createROIData(rd),
			createROIData(pd));
		setUpMethodCalls(4, rr);

		final TreeNode<?> dn = session.downloadROIs(1);

		assertEquals(3, dn.children().size());
		for (final TreeNode<?> child : dn.children()) {
			assertTrue(child instanceof OMEROROICollection);
			assertEquals(1, child.children().size());
			assertTrue(child.children().get(0) instanceof OMEROROIElement);
		}

		assertTrue(dn.children().get(0).children().get(0)
			.data() instanceof Ellipsoid);
		assertTrue(dn.children().get(1).children().get(0).data() instanceof Box);
		assertTrue(dn.children().get(2).children().get(0)
			.data() instanceof PointMask);
	}

	@Test
	public void testDownloadROIDataWithManyShapeData() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException
	{
		final RectangleData rdZero = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdOne = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdTwo = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdThree = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdFour = new RectangleData(10, 22.25, 67, 94);
		final ROIResult rr = createROIResult(createROIData(rdZero, rdOne, rdTwo,
			rdThree, rdFour));
		setUpMethodCalls(5, rr);

		final TreeNode<?> dn = session.downloadROIs(1);

		assertEquals(1, dn.children().size());
		assertTrue(dn.children().get(0) instanceof OMEROROICollection);

		final List<TreeNode<?>> children = dn.children().get(0).children();
		assertEquals(5, children.size());

		for (final TreeNode<?> child : children) {
			assertTrue(child instanceof OMEROROIElement);
			assertTrue(child.data() instanceof Box);
		}
	}

	@Test
	public void testDownloadManyROIResults() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException
	{
		final RectangleData rdZero = new RectangleData(10, 10, 20, 20);
		final RectangleData rdOne = new RectangleData(10, 10, 20, 20);
		final RectangleData rdTwo = new RectangleData(10, 10, 20, 20);
		final EllipseData e = new EllipseData(30, 32, 30.5, 78);
		final PointData pdZero = new PointData(1, 1);
		final PointData pdOne = new PointData(10, 10);
		final PointData pdTwo = new PointData(12, 12);
		final ROIResult rrZero = createROIResult(createROIData(rdZero, rdOne,
			rdTwo), createROIData(e));
		final ROIResult rrOne = createROIResult(createROIData(pdZero));
		final ROIResult rrTwo = createROIResult(createROIData(pdOne), createROIData(
			pdTwo));
		setUpMethodCalls(10, rrZero, rrOne, rrTwo);

		final TreeNode<?> dn = session.downloadROIs(1);
		assertEquals(5, dn.children().size());

		for (final TreeNode<?> node : dn.children()) {
			assertTrue(node instanceof OMEROROICollection);
			for (final TreeNode<?> child : node.children())
				assertTrue(child instanceof OMEROROIElement);
		}

		final List<TreeNode<?>> children = dn.children();
		assertEquals(3, children.get(0).children().size());
		for (final TreeNode<?> child : children.get(0).children())
			assertTrue(child.data() instanceof Box);

		assertEquals(1, children.get(1).children().size());
		assertTrue(children.get(1).children().get(0).data() instanceof Ellipsoid);

		assertEquals(1, children.get(2).children().size());
		assertTrue(children.get(2).children().get(0).data() instanceof PointMask);

		assertEquals(1, children.get(3).children().size());
		assertTrue(children.get(3).children().get(0).data() instanceof PointMask);

		assertEquals(1, children.get(4).children().size());
		assertTrue(children.get(4).children().get(0).data() instanceof PointMask);
	}

	@Test
	public void testDownloadTransformedROIData() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException
	{
		final RectangleData rd = new RectangleData(150, 200, 30.25, 14);
		final omero.model.AffineTransform transform = new AffineTransformI();
		transform.setA00(omero.rtypes.rdouble(1));
		transform.setA01(omero.rtypes.rdouble(0));
		transform.setA02(omero.rtypes.rdouble(-20));
		transform.setA10(omero.rtypes.rdouble(0));
		transform.setA11(omero.rtypes.rdouble(1));
		transform.setA12(omero.rtypes.rdouble(55));
		rd.setTransform(transform);
		final ROIResult rr = createROIResult(createROIData(rd));
		setUpMethodCalls(1, rr);

		final TreeNode<?> dn = session.downloadROIs(1);

		assertEquals(1, dn.children().size());
		assertTrue(dn.children().get(0) instanceof OMEROROICollection);

		final OMEROROICollection orc = (OMEROROICollection) dn.children().get(0);
		final List<TreeNode<?>> children = orc.children();
		assertEquals(1, children.size());
		assertTrue(children.get(0) instanceof OMEROROIElement);

		final OMEROROIElement ore = (OMEROROIElement) children.get(0);
		assertTrue(ore.data() instanceof TransformedOMERORealMaskRealInterval);
	}

	@Test
	public void testDownloadTextData() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException
	{
		// NB: Currently this test should fail, as TextData is not supported
		final TextData td = new TextData("Hello", 121, 68.5);
		final ROIResult rr = createROIResult(createROIData(td));
		setUpMethodCalls(1, rr);

		exception.expect(IllegalArgumentException.class);
		session.downloadROIs(1);
	}

	// -- test downloadROI(...) --

	@Test
	public void testDownloadROIDataViaID() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException
	{
		final EllipseData ed = new EllipseData(22, 22, 3, 5);
		final ROIResult rr = createROIResult(createROIData(ed));
		setUpMethodCallsTwo(rr);

		final TreeNode<?> dn = session.downloadROI(1);

		assertTrue(dn.children().get(0) instanceof OMEROROICollection);

		final OMEROROICollection orc = (OMEROROICollection) dn.children().get(0);
		final List<TreeNode<?>> children = orc.children();
		assertEquals(1, children.size());
		assertTrue(children.get(0) instanceof OMEROROIElement);

		final OMEROROIElement ore = (OMEROROIElement) children.get(0);
		assertTrue(ore.data() instanceof Ellipsoid);
	}

	@Test
	public void testDownloadROIDataWithManyShapesViaID()
		throws ExecutionException, DSOutOfServiceException, DSAccessException,
		OMEROException
	{
		final RectangleData rdZero = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdOne = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdTwo = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdThree = new RectangleData(10, 22.25, 67, 94);
		final RectangleData rdFour = new RectangleData(10, 22.25, 67, 94);
		final ROIResult rr = createROIResult(createROIData(rdZero, rdOne, rdTwo,
			rdThree, rdFour));
		setUpMethodCallsTwo(rr);

		final TreeNode<?> dn = session.downloadROI(1);

		assertEquals(1, dn.children().size());
		assertTrue(dn.children().get(0) instanceof OMEROROICollection);

		final List<TreeNode<?>> children = dn.children().get(0).children();
		assertEquals(5, children.size());

		for (final TreeNode<?> child : children) {
			assertTrue(child instanceof OMEROROIElement);
			assertTrue(child.data() instanceof Box);
		}
	}

	// -- Helper methods --

	private void setUpMethodCalls(final int numROIs, final ROIResult... results)
		throws ExecutionException, DSOutOfServiceException, DSAccessException,
		OMEROException
	{
		final List<ROIResult> rr = Arrays.asList(results);
		new Expectations() {

			{
				session = service.session(server);

				gateway.getFacility(ROIFacility.class);
				result = roiFac;

				roiFac.getROICount((SecurityContext) any, anyLong);
				result = numROIs;

				roiFac.loadROIs((SecurityContext) any, anyLong);
				result = rr;
			}
		};
	}

	private void setUpMethodCallsTwo(final ROIResult rr)
		throws ExecutionException, DSOutOfServiceException, DSAccessException,
		OMEROException
	{
		new Expectations() {

			{
				session = service.session(server);

				gateway.getFacility(ROIFacility.class);
				result = roiFac;

				roiFac.loadROI((SecurityContext) any, anyLong);
				result = rr;
			}
		};
	}

	private ROIData createROIData(final ShapeData... shapes) {
		final Random rand = new Random();
		final Roi r = new RoiI(rand.nextLong() & Long.MAX_VALUE, true);
		final Event e = new EventI(rand.nextLong() & Long.MAX_VALUE, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();

		for (final ShapeData shape : shapes) {
			shape.setId(rand.nextLong() & Long.MAX_VALUE);
			shape.setZ(rand.nextInt() & Integer.MAX_VALUE);
			shape.setT(rand.nextInt() & Integer.MAX_VALUE);
			shape.setC(rand.nextInt() & Integer.MAX_VALUE);
			r.addShape((Shape) shape.asIObject());
		}
		return new ROIData(r);
	}

	private ROIResult createROIResult(final ROIData... rois) {
		final List<ROIData> rd = Arrays.asList(rois);
		return new ROIResult(rd, 147);
	}
}
