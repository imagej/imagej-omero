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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.omero.DefaultOMEROSession;
import net.imagej.omero.OMEROLocation;
import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.project.OMEROZTCProjectedRealMask;
import net.imagej.omero.roi.project.OMEROZTCProjectedRealMaskRealInterval;
import net.imagej.roi.DefaultROITree;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.Sphere;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.util.DefaultTreeNode;
import org.scijava.util.TreeNode;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import omero.ServerError;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TagAnnotationData;
import omero.model.Event;
import omero.model.EventI;
import omero.model.Roi;
import omero.model.RoiI;
import omero.model.Shape;

/**
 * Tests {@link OMEROService#uploadROIs(OMEROLocation, TreeNode, long)}. Note,
 * that the actual data structure conversions are not tested here as they are
 * tested elsewhere.
 *
 * @author Alison Walter
 */
public class UploadROITest {

	private OMEROLocation location;
	private OMEROService service;
	private List<omero.model.IObject> tags;

	@Mocked
	private DefaultOMEROSession session;

	@Mocked
	private Gateway gateway;

	@Mocked
	private ROIFacility roiFac;

	@Mocked
	private BrowseFacility browse;

	@Mocked
	private ImageData image;

	@Mocked
	private PixelsData pixels;

	@Before
	public void setUp() throws URISyntaxException {
		location = new OMEROLocation("localhost", 4064, "omero", "omero");
		service = new Context(OMEROService.class, ConvertService.class).getService(
			OMEROService.class);

		tags = new ArrayList<>();
		final TagAnnotationData t = new TagAnnotationData(service.getVersion());
		t.setDescription(ROIConverters.IJO_VERSION_DESC);
		tags.add(t.asIObject());
	}

	@After
	public void tearDown() {
		service.dispose();
	}

	@Test
	public void testUploadSingleRMRI() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final Box b = GeomMasks.closedBox(new double[] { 12.5, 16 }, new double[] {
			83, 92 });
		final TreeNode<Box> dn = new DefaultTreeNode<>(b, null);
		setUpMethodCalls(false, 1, null);

		final long[] ids = service.uploadROIs(location, dn, 12);

		assertEquals(1, ids.length);
		assertEquals(50, ids[0]);

		// NB: run verification to capture ROIData
		checkROIData(ids.length, 1);
	}

	@Test
	public void testUploadCompositeMaskPredicate() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final Box b = GeomMasks.openBox(new double[] { 10, 11.25 }, new double[] {
			66, 92.5 });
		final Ellipsoid e = GeomMasks.closedEllipsoid(new double[] { 83, 74.5 },
			new double[] { 11, 7.5 });
		final RealMaskRealInterval or = b.or(e);
		final TreeNode<RealMaskRealInterval> dn = new DefaultTreeNode<>(or, null);
		setUpMethodCalls(false, 1, null);

		final long[] ids = service.uploadROIs(location, dn, 22);

		assertEquals(1, ids.length);
		assertEquals(50, ids[0]);

		// NB: run verification to capture ROIData
		checkROIData(ids.length, 1);
	}

	@Test
	public void testUploadOMERORoiCollecton() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final RectangleData rd = new RectangleData(12, 15.5, 4, 6);
		rd.setId(122);
		rd.setZ(0);
		rd.setT(0);
		final RectangleData rdOne = new RectangleData(12, 15.5, 4, 6);
		rd.setId(123);
		rd.setZ(0);
		rd.setT(1);
		final RectangleData rdTwo = new RectangleData(12, 15.5, 4, 6);
		rd.setId(124);
		rd.setZ(0);
		rd.setT(2);

		// NB: Create ROIData as though it came from server. This means it has
		// loaded shapes but unloaded annotations
		final Roi r = new RoiI(33, true);
		final Event e = new EventI(24, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();
		r.addShape((Shape) rd.asIObject());
		r.addShape((Shape) rdOne.asIObject());
		r.addShape((Shape) rdTwo.asIObject());
		final ROIData data = new ROIData(r);
		final OMEROROICollection orc = new DefaultOMEROROICollection(null, data,
			service.getContext().getService(ConvertService.class));
		setUpMethodCalls(false, 1, Collections.singletonList(data));

		final long[] ids = service.uploadROIs(location, orc, 13);

		assertEquals(1, ids.length);
		assertEquals(50, ids[0]);

		// NB: run verification to capture ROIData
		checkROIData(ids.length, 3);
	}

	@Test
	public void testUploadOMERORoiElement() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final RectangleData rd = new RectangleData(12, 15.5, 4, 6);
		rd.setId(122);
		rd.setZ(0);
		rd.setT(0);
		final RectangleData rdOne = new RectangleData(12, 15.5, 4, 6);
		rd.setId(123);
		rd.setZ(0);
		rd.setT(1);
		final RectangleData rdTwo = new RectangleData(12, 15.5, 4, 6);
		rd.setId(124);
		rd.setZ(0);
		rd.setT(2);

		// NB: Create ROIData as though it came from server. This means it has
		// loaded shapes but unloaded annotations
		final Roi r = new RoiI(33, true);
		final Event e = new EventI(24, true);
		r.getDetails().setUpdateEvent(e);
		r.unloadAnnotationLinks();
		r.addShape((Shape) rd.asIObject());
		r.addShape((Shape) rdOne.asIObject());
		r.addShape((Shape) rdTwo.asIObject());
		final ROIData data = new ROIData(r);
		final OMEROROICollection orc = new DefaultOMEROROICollection(null, data,
			service.getContext().getService(ConvertService.class));
		final TreeNode<?> ore = orc.children().get(1);
		setUpMethodCalls(false, 1, null);

		final long[] ids = service.uploadROIs(location, ore, 13);

		assertEquals(1, ids.length);
		assertEquals(50, ids[0]);

		// NB: run verification to capture ROIData
		checkROIData(ids.length, 1);
	}

	@Test
	public void testUploadUnboundedMaskPredicate() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final Ellipsoid e = GeomMasks.closedEllipsoid(new double[] { 14, 15.5 },
			new double[] { 2, 3 });
		final RealMask rm = e.negate();
		final TreeNode<RealMask> dn = new DefaultTreeNode<>(rm, null);
		setUpMethodCalls(true, 1, null);

		final long[] ids = service.uploadROIs(location, dn, 22);

		assertEquals(1, ids.length);
		assertEquals(50, ids[0]);

		// NB: run verification to capture ROIData
		checkROIData(ids.length, 1);
	}

	@Test
	public void testUploadMultipleRois() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final Ellipsoid e = GeomMasks.closedEllipsoid(new double[] { 33, 27 },
			new double[] { 8, 3.5 });
		final Box b = GeomMasks.closedBox(new double[] { 100, 101 }, new double[] {
			120, 130.5 });
		final Box b2 = GeomMasks.closedBox(new double[] { 50, 64 }, new double[] {
			242, 136 });
		final Box b3 = GeomMasks.closedBox(new double[] { 1, 1 }, new double[] { 33,
			56 });
		final Sphere s = GeomMasks.openSphere(new double[] { 22, 36 }, 5.5);
		final OMEROZTCProjectedRealMask proj =
			new OMEROZTCProjectedRealMaskRealInterval(s, 2, 2, 0);

		final List<TreeNode<?>> rois = new ArrayList<>(5);
		rois.add(new DefaultTreeNode<>(e, null));
		rois.add(new DefaultTreeNode<>(b, null));
		rois.add(new DefaultTreeNode<>(b2, null));
		rois.add(new DefaultTreeNode<>(b3, null));
		rois.add(new DefaultTreeNode<>(proj, null));
		final TreeNode<?> parent = new DefaultROITree();

		final List<ROIData> test = new ArrayList<>(5);
		for (TreeNode<?> t : rois) {
			final ShapeData r = service.getContext().getService(ConvertService.class)
				.convert(t.data(), ShapeData.class);
			final ROIData roi = new ROIData();
			roi.addShapeData(r);
			test.add(roi);
		}

		parent.addChildren(rois);
		setUpMethodCalls(false, 5, test);

		final long[] ids = service.uploadROIs(location, parent, 300);

		assertEquals(5, ids.length);

		// NB: run verification to capture ROIData
		checkROIData(ids.length, 1, 1, 1, 1, 1);
	}

	// -- Helper Methods --

	@SuppressWarnings({ "unchecked", "resource" })
	private void setUpMethodCalls(final boolean needInterval,
		final int numROIData, final List<ROIData> rois) throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{

		new Expectations() {

			{
				new DefaultOMEROSession(location, service);
				result = session;
				session.getGateway();
				result = gateway;

				gateway.getFacility(ROIFacility.class);
				result = roiFac;

				if (needInterval) {
					gateway.getFacility(BrowseFacility.class);
					result = browse;
					browse.getImage((SecurityContext) any, anyLong);
					result = image;

					image.getDefaultPixels();
					result = pixels;
					pixels.getSizeX();
					result = 120;

					image.getDefaultPixels();
					result = pixels;
					pixels.getSizeY();
					result = 190;
				}

				final List<ROIData> rd = new ArrayList<>(numROIData);
				if (rois != null) {
					for (int i = 0; i < numROIData; i++) {
						final ROIData roi = rois.get(i);
						final ROIData temp = new ROIData();
						temp.setId(i + 50);
						final Iterator<List<ShapeData>> itr = roi.getIterator();
						while (itr.hasNext()) {
							final List<ShapeData> shapes = itr.next();
							for (final ShapeData shape : shapes)
								temp.addShapeData(shape);
						}
						rd.add(temp);
					}
				}
				else {
					for (int i = 0; i < numROIData; i++) {
						final ROIData temp = new ROIData();
						temp.setId(50 + i);
						rd.add(temp);
					}
				}
				for (int i = 0; i < numROIData; i++) {
					roiFac.saveROIs((SecurityContext) any, anyLong,
						(Collection<ROIData>) any);
					result = rd;
				}
			}
		};
	}

	private void checkROIData(final int numROIData, final int... numShapes)
		throws DSOutOfServiceException, DSAccessException
	{
		new Verifications() {

			{
				Collection<ROIData> rois = new ArrayList<>(numROIData);
				for (int i = 0; i < numROIData; i++) {
					Collection<ROIData> rd;
					roiFac.saveROIs((SecurityContext) any, anyLong, rd = withCapture());
					rois.addAll(rd);
				}

				assertEquals(numROIData, rois.size());

				// HACK: Since this ROIData wasn't actually saved to the server, it has
				// shapes in a TreeMap with null keys
				final Iterator<ROIData> itrRD = rois.iterator();
				int countRD = 0;
				while (itrRD.hasNext()) {
					final Iterator<List<ShapeData>> itrShape = itrRD.next().getIterator();
					int countShape = 0;
					while (itrShape.hasNext())
						countShape += itrShape.next().size();
					assertEquals(numShapes[countRD], countShape);
					countRD++;
				}
			}
		};
	}
}
