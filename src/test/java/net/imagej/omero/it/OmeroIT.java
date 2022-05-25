/*-
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

package net.imagej.omero.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import io.scif.services.DatasetIOService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.omero.OMEROException;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.omero.roi.OMEROROICollection;
import net.imagej.roi.DefaultROITree;
import net.imagej.roi.ROITree;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.PointMask;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.WritablePointMask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.scijava.Context;
import org.scijava.table.ByteTable;
import org.scijava.table.DefaultByteTable;
import org.scijava.table.Table;
import org.scijava.util.DefaultTreeNode;
import org.scijava.util.TreeNode;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.TableData;

/**
 * Minimal integration tests for upload/download functionalities. The intention
 * of these tests is to test connecting to the OMERO server, not to check the
 * actual conversions.
 *
 * @author Alison Walter
 */
@Category(IntegrationTest.class)
public class OmeroIT {

	private omero.client client;
	private Context context;
	private OMEROService omero;

	private static final String OMERO_SERVER = "omero";
	private static final int OMERO_PORT = 4064;
	private static final String OMERO_USER = "root";
	private static final String OMERO_PASSWORD = "omero";

	@Before
	public void setup() {
		client = new omero.client(OMERO_SERVER, OMERO_PORT);

		context = new Context();
		omero = context.getService(OMEROService.class);
	}

	@After
	public void teardown() {
		context.dispose();
	}

	@Test
	public void testConnectingToServer() throws CannotCreateSessionException,
		PermissionDeniedException, ServerError
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);

		assertNotNull(client.getSessionId());
		client.closeSession();
	}

	@Test
	public void testDownloadImage() throws ServerError,
		CannotCreateSessionException, PermissionDeniedException, OMEROException
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (OMEROSession session = new OMEROSession(omero, client)) {
			final Dataset d = session.downloadImage(1);
			assertNotNull(d.getImgPlus());
		}
		finally {
			client.closeSession();
		}

	}

	@Test
	public void testUploadImage() throws IOException, ServerError,
		CannotCreateSessionException, PermissionDeniedException, OMEROException
	{
		final DatasetIOService io = context.getService(DatasetIOService.class);
		final Dataset d = io.open("http://imagej.net/images/blobs.gif");

		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final long id = session.uploadImage(d);
			assertTrue(id > 0);
		}
		finally {
			client.closeSession();
		}

	}

	@Test
	public void testDownloadThenUploadImage() throws CannotCreateSessionException,
		PermissionDeniedException, ServerError, OMEROException
	{
		final long originalId = 1;
		client.createSession(OMERO_USER, OMERO_PASSWORD);

		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final Dataset d = session.downloadImage(originalId);
			final long newId = session.uploadImage(d);
			assertTrue(originalId != newId);
		}
		finally {
			client.closeSession();
		}
	}

	@Test
	public void testDownloadTable() throws OMEROException,
		CannotCreateSessionException, PermissionDeniedException, ServerError
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			// now download the table
			final Table<?, ?> ijTable = session.downloadTable(83);

			assertNotNull(ijTable);
			assertEquals(3, ijTable.getColumnCount());
			assertEquals(3, ijTable.getRowCount());
		}
		finally {
			client.closeSession();
		}
	}

	@Test
	public void testUploadTable() throws ExecutionException,
		DSOutOfServiceException, DSAccessException, OMEROException,
		CannotCreateSessionException, PermissionDeniedException, ServerError
	{
		final byte[][] d = new byte[][] { { 127, 0, -128 }, { -1, -6, -23 }, { 100,
			87, 4 } };
		final ByteTable table = new DefaultByteTable(3, 3);
		for (int c = 0; c < table.getColumnCount(); c++) {
			table.setColumnHeader(c, "Heading " + (c + 1));
			for (int r = 0; r < table.getRowCount(); r++)
				table.set(c, r, d[c][r]);
		}

		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final long tableId = session.uploadTable("test-table-upload", table, 1);
			final TablesFacility tablesFacility = session.getGateway().getFacility(
				TablesFacility.class);
			final TableData td = tablesFacility.getTableInfo(session
				.getSecurityContext(), tableId);

			assertEquals(td.getColumns().length, 3);
			assertEquals(td.getColumns()[0].getName(), "Heading 1");
			assertEquals(td.getColumns()[1].getName(), "Heading 2");
			assertEquals(td.getColumns()[2].getName(), "Heading 3");
			assertEquals(td.getNumberOfRows(), 3);
		}
		finally {
			client.closeSession();
		}
	}

	@Test
	public void testDownloadThenUploadTable() throws OMEROException,
		CannotCreateSessionException, PermissionDeniedException, ServerError
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final long originalId = 83;
			final Table<?, ?> ijTable = session.downloadTable(originalId);

			final long newId = session.uploadTable("table-version2", ijTable, 1);

			assertTrue(originalId != newId);
		}
		finally {
			client.closeSession();
		}
	}

	@Test
	public void testDownloadROIs() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, OMEROException
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final TreeNode<?> dns = session.downloadROIs(1);

			assertNotNull(dns);
			assertFalse(dns.children().isEmpty());
			assertEquals(10, dns.children().size());

			for (final TreeNode<?> dn : dns.children()) {
				assertTrue(dn instanceof OMEROROICollection);
				final List<TreeNode<?>> children = dn.children();
				assertEquals(1, children.size());
				assertTrue(children.get(0).data() instanceof PointMask);
			}
		}
		finally {
			client.closeSession();
		}
	}

	@Test
	public void testDownloadROI() throws OMEROException,
		CannotCreateSessionException, PermissionDeniedException, ServerError
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final TreeNode<?> dn = session.downloadROI(1);

			assertTrue(dn instanceof ROITree);
			assertEquals(1, dn.children().size());

			assertTrue(dn.children().get(0) instanceof OMEROROICollection);
			final List<TreeNode<?>> children = dn.children().get(0).children();
			assertEquals(1, children.size());

			assertTrue(children.get(0).data() instanceof PointMask);
		}
		finally {
			client.closeSession();
		}
	}

	@Test
	public void testUploadROIs() throws OMEROException,
		CannotCreateSessionException, PermissionDeniedException, ServerError
	{
		final Box b = GeomMasks.closedBox(new double[] { 10, 10 }, new double[] {
			22, 46.5 });
		final TreeNode<Box> dnb = new DefaultTreeNode<>(b, null);
		final Ellipsoid e = GeomMasks.openEllipsoid(new double[] { 120, 121.25 },
			new double[] { 4, 9 });
		final TreeNode<Ellipsoid> dne = new DefaultTreeNode<>(e, null);
		final Polygon2D p = GeomMasks.polygon2D(new double[] { 30, 40, 50 },
			new double[] { 50, 80, 50 });
		final TreeNode<Polygon2D> dnp = new DefaultTreeNode<>(p, null);

		final List<TreeNode<?>> dns = Lists.newArrayList(dnb, dne, dnp);
		final ROITree parent = new DefaultROITree();
		parent.addChildren(dns);

		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final long[] ids = session.uploadROIs(parent, 2);

			assertEquals(3, ids.length);
			assertTrue(ids[0] > 0);
			assertTrue(ids[1] > 0);
			assertTrue(ids[2] > 0);
		}
		finally {
			client.closeSession();
		}
	}

	@Test
	public void testDownloadThenUploadROI() throws OMEROException,
		CannotCreateSessionException, PermissionDeniedException, ServerError
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		try (final OMEROSession session = new OMEROSession(omero, client)) {
			final long originalRoiId = 3;
			final TreeNode<?> dn = session.downloadROI(originalRoiId);

			final List<TreeNode<?>> children = dn.children().get(0).children();
			assertTrue(children.get(0).data() instanceof WritablePointMask);
			final WritablePointMask pm = (WritablePointMask) children.get(0).data();

			pm.setPosition(new double[] { 0, 0 });

			final long[] ids = session.uploadROIs(dn, 2);

			assertEquals(1, ids.length);
			assertTrue(originalRoiId != ids[0]);
		}
		finally {
			client.closeSession();
		}
	}
}
