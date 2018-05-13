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

package net.imagej.omero.roi.point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.omero.OMEROLocation;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.point.TreeNodeRPCToROIData;
import net.imagej.table.Table;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritableRealPointCollection;
import net.imglib2.roi.geom.real.KDTreeRealPointCollection;
import net.imglib2.roi.geom.real.RealPointCollection;
import net.imglib2.roi.geom.real.RealPointSampleListWritableRealPointCollection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.Optional;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.util.DefaultTreeNode;
import org.scijava.util.TreeNode;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.RType;
import omero.ServerError;
import omero.client;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.PointData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TableData;
import omero.grid.Param;
import omero.model.Roi;
import omero.model.RoiAnnotationLink;
import omero.model.TagAnnotation;
import omero.model.TagAnnotationI;

/**
 * Tests for {@link TreeNodeRPCToROIData}.
 *
 * @author Alison Walter
 */
public class TreeNodeRPCToROIDataConversionTest {

	private List<RealLocalizable> pts;
	private ConvertService convert;

	@Before
	public void setUp() {
		final Context c = new Context(ConvertService.class, MockOMEROService.class,
			LogService.class);
		convert = c.getService(ConvertService.class);
		pts = new ArrayList<>(5);
		pts.add(new RealPoint(new double[] { 1.5, 2.25 }));
		pts.add(new RealPoint(new double[] { 6, 392 }));
		pts.add(new RealPoint(new double[] { 99, 78.5 }));
		pts.add(new RealPoint(new double[] { 90, 30 }));
		pts.add(new RealPoint(new double[] { 400, 8 }));
	}

	@After
	public void tearDown() {
		convert.getContext().dispose();
	}

	@Test
	public void testConverterMatching() {
		final RealPointCollection<?> defaultRPC =
			new DefaultWritableRealPointCollection<>(pts);
		final RealPointCollection<?> rpslRPC =
			new RealPointSampleListWritableRealPointCollection<>(pts);
		final RealPointCollection<?> kdTreeRPC = new KDTreeRealPointCollection<>(
			pts);

		final TreeNode<?> defaultDN = new DefaultTreeNode<>(defaultRPC, null);
		final TreeNode<?> rpslDN = new DefaultTreeNode<>(rpslRPC, null);
		final TreeNode<?> kdTreeDN = new DefaultTreeNode<>(kdTreeRPC, null);

		final Converter<?, ?> defaultC = convert.getHandler(defaultDN,
			ROIData.class);
		final Converter<?, ?> rpslC = convert.getHandler(rpslDN, ROIData.class);
		final Converter<?, ?> kdTreeC = convert.getHandler(kdTreeDN, ROIData.class);

		assertTrue(defaultC instanceof TreeNodeRPCToROIData);
		assertTrue(rpslC instanceof TreeNodeRPCToROIData);
		assertTrue(kdTreeC instanceof TreeNodeRPCToROIData);
	}

	@Test
	public void testDefaultConversion() {
		final RealPointCollection<?> rpc = new DefaultWritableRealPointCollection<>(
			pts);

		final TreeNode<?> dn = new DefaultTreeNode<>(rpc, null);
		final ROIData rd = convert.convert(dn, ROIData.class);

		checkROIData(rd);
		checkShapeData(rd);
	}

	@Test
	public void testRealPointSampleListConversion() {
		final RealPointCollection<?> rpc =
			new RealPointSampleListWritableRealPointCollection<>(pts);

		final TreeNode<?> dn = new DefaultTreeNode<>(rpc, null);
		final ROIData rd = convert.convert(dn, ROIData.class);

		checkROIData(rd);
		checkShapeData(rd);
	}

	@Test
	public void testKDTreeConversion() {
		final RealPointCollection<?> rpc = new KDTreeRealPointCollection<>(pts);

		final TreeNode<?> dn = new DefaultTreeNode<>(rpc, null);
		final ROIData rd = convert.convert(dn, ROIData.class);

		checkROIData(rd);
		checkShapeData(rd);
	}

	// -- Helper methods --

	private void checkROIData(final ROIData rd) {
		// ensure ID set to -1, so not over-written on server
		assertEquals(-1, rd.getId());

		// check that the annotation is there and correct
		final List<RoiAnnotationLink> anno = ((Roi) rd.asIObject())
			.copyAnnotationLinks();
		assertEquals(1, anno.size());
		assertTrue(anno.get(0).getChild() instanceof TagAnnotation);
		final TagAnnotation tag = (TagAnnotation) anno.get(0).getChild();
		assertEquals(ROIConverters.IJO_VERSION_DESC, tag.getDescription()
			.getValue());
		assertEquals(convert.getContext().getService(OMEROService.class)
			.getVersion(), tag.getTextValue().getValue());
	}

	private void checkShapeData(final ROIData rd) {
		final Iterator<List<ShapeData>> itr = rd.getIterator();
		int count = 0;
		final List<RealLocalizable> copy = new ArrayList<>(pts);

		while (itr.hasNext()) {
			final List<ShapeData> shapes = itr.next();
			for (final ShapeData shape : shapes) {
				assertTrue(shape instanceof PointData);
				final PointData pd = (PointData) shape;
				assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, pd.getText());
				findMatch(pd, copy);
				count++;
			}
		}

		assertEquals(pts.size(), count);
	}

	private void findMatch(final PointData pd, final List<RealLocalizable> copy) {
		for (int i = 0; i < copy.size(); i++) {
			if (copy.get(i).getDoublePosition(0) == pd.getX() && copy.get(i)
				.getDoublePosition(1) == pd.getY())
			{
				copy.remove(i);
				return;
			}
		}
		// No matching point found
		assertTrue(false);
	}

//-- Mock classes --

	@Plugin(type = Service.class)
	public static final class MockOMEROService extends AbstractService implements
		OMEROService, Optional
	{

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
			return null;
		}

		@Override
		public OMEROSession createSession(final OMEROLocation location) {
			return null;
		}

		@Override
		public void removeSession(final OMEROSession session) {}

		@Override
		public TagAnnotationI getAnnotation(final String description,
			final String value, final OMEROLocation location)
			throws ExecutionException, ServerError, DSOutOfServiceException,
			DSAccessException
		{
			return null;
		}

		@Override
		public TagAnnotationI getAnnotation(final String description,
			final String value) throws ExecutionException, ServerError,
			DSOutOfServiceException, DSAccessException
		{
			if (description == ROIConverters.IJO_VERSION_DESC) {
				final TagAnnotationI tag = new TagAnnotationI();
				tag.setDescription(omero.rtypes.rstring(description));
				tag.setTextValue(omero.rtypes.rstring(value));
				return tag;
			}
			throw new IllegalArgumentException("Invalid description: " + description);
		}

		@Override
		public List<TreeNode<?>> downloadROIs(final OMEROLocation credentials,
			final long imageID) throws ServerError, PermissionDeniedException,
			CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
			DSAccessException
		{
			return null;
		}

		@Override
		public TreeNode<?> downloadROI(OMEROLocation credentials, long roiID)
			throws DSOutOfServiceException, DSAccessException, ExecutionException
		{
			return null;
		}

		@Override
		public <D extends TreeNode<?>> long[] uploadROIs(
			final OMEROLocation credentials, final List<D> ijROIs, final long imageID)
			throws ServerError, PermissionDeniedException,
			CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
			DSAccessException
		{
			return null;
		}

	}
}
