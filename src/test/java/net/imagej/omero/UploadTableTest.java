/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2015 Open Microscopy Environment:
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

package net.imagej.omero;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import net.imagej.table.BoolColumn;
import net.imagej.table.BoolTable;
import net.imagej.table.CharTable;
import net.imagej.table.DefaultBoolTable;
import net.imagej.table.DefaultCharTable;
import net.imagej.table.DefaultColumn;
import net.imagej.table.DefaultFloatTable;
import net.imagej.table.DefaultGenericTable;
import net.imagej.table.DefaultLongTable;
import net.imagej.table.DefaultShortTable;
import net.imagej.table.DoubleColumn;
import net.imagej.table.FloatTable;
import net.imagej.table.GenericTable;
import net.imagej.table.LongTable;
import net.imagej.table.ShortTable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.util.BoolArray;
import org.scijava.util.DoubleArray;
import org.scijava.util.IntArray;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.VerificationsInOrder;
import omero.ServerError;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.grid.SharedResourcesPrx;
import omero.grid.TablePrx;
import omero.model.OriginalFile;

/**
 * Tests {@link DefaultOMEROService#uploadTable}.
 *
 * @author Alison Walter
 */
public class UploadTableTest {

	private OMEROService service;
	private OMEROCredentials credentials;

	@Before
	public void setUp() throws Exception {
		service = new Context(OMEROService.class).service(OMEROService.class);
	}

	@After
	public void tearDown() {
		if (service != null) {
			service.getContext().dispose();
			service = null;
		}
	}

//	@Test
//	public void testBoolTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final BoolTable table = new DefaultBoolTable(2, 4);
//		table.get(0).fill(new boolean[] { true, true, false, false });
//		table.get(1).fill(new boolean[] { true, false, true, false });
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 3l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 3l);
//				assertEquals(cols.length, table.getColumnCount());
//
//				for (int c = 0; c < table.getColumnCount(); c++) {
//					assertTrue(omero.grid.BoolColumn.class.isInstance(cols[c]));
//					assertEquals(((omero.grid.BoolColumn) cols[c]).values.length, table
//						.getRowCount());
//					assertEquals(cols[c].name, String.valueOf(c));
//					for (int r = 0; r < table.getRowCount(); r++) {
//						assertEquals(((omero.grid.BoolColumn) cols[c]).values[r], table
//							.getValue(c, r));
//					}
//				}
//			}
//		};
//	}
//
//	@Test
//	public void testShortTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final ShortTable table = new DefaultShortTable(6, 3);
//		table.get(0).fill(new short[] { -32768, 0, 32767 });
//		table.get(1).fill(new short[] { 12, -12, 5 });
//		table.get(2).fill(new short[] { 10000, 20000, -30000 });
//		table.get(3).fill(new short[] { 1111, 2222, 8888 });
//		table.get(4).fill(new short[] { -4, -17, -31194 });
//		table.get(5).fill(new short[] { 0, 17239, -20 });
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 22l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 22l);
//				assertEquals(cols.length, table.getColumnCount());
//
//				for (int c = 0; c < table.getColumnCount(); c++) {
//					assertTrue(omero.grid.LongColumn.class.isInstance(cols[c]));
//					assertEquals(((omero.grid.LongColumn) cols[c]).values.length, table
//						.getRowCount());
//					assertEquals(cols[c].name, String.valueOf(c));
//					for (int r = 0; r < table.getRowCount(); r++) {
//						assertEquals(((omero.grid.LongColumn) cols[c]).values[r], table
//							.getValue(c, r));
//					}
//				}
//			}
//		};
//	}
//
//	@Test
//	public void testLongTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final LongTable table = new DefaultLongTable(2, 2);
//		table.get(0).fill(new long[] { 9223372036854775807l, 0 });
//		table.get(1).fill(new long[] { -9223372036854775808l, 4 });
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 11l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 11l);
//				assertEquals(cols.length, table.getColumnCount());
//
//				for (int c = 0; c < table.getColumnCount(); c++) {
//					assertTrue(omero.grid.LongColumn.class.isInstance(cols[c]));
//					assertEquals(((omero.grid.LongColumn) cols[c]).values.length, table
//						.getRowCount());
//					assertEquals(cols[c].name, String.valueOf(c));
//					for (int r = 0; r < table.getRowCount(); r++) {
//						assertEquals(((omero.grid.LongColumn) cols[c]).values[r], table
//							.getValue(c, r));
//					}
//				}
//			}
//		};
//	}
//
//	@Test
//	public void testFloatTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final FloatTable table = new DefaultFloatTable(4, 2);
//		table.get(0).fill(new float[] { -380129.125f, 0.25f });
//		table.get(1).fill(new float[] { 9871234.0f, -12.5f });
//		table.get(2).fill(new float[] { 0.0625f, 13208.03125f });
//		table.get(3).fill(new float[] { -0.0625f, 1908471790.5f });
//
//		final String[] headers = new String[] { "H1", "H2", "H3", "H4" };
//
//		for (int i = 0; i < table.getColumnCount(); i++) {
//			table.get(i).setHeader(headers[i]);
//		}
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 21l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 21l);
//				assertEquals(cols.length, table.getColumnCount());
//
//				for (int c = 0; c < table.getColumnCount(); c++) {
//					assertTrue(omero.grid.DoubleColumn.class.isInstance(cols[c]));
//					assertEquals(((omero.grid.DoubleColumn) cols[c]).values.length, table
//						.getRowCount());
//					assertEquals(cols[c].name, table.get(c).getHeader());
//					for (int r = 0; r < table.getRowCount(); r++) {
//						assertEquals(((omero.grid.DoubleColumn) cols[c]).values[r], table
//							.getValue(c, r), 0);
//					}
//				}
//			}
//		};
//	}
//
//	@Test
//	public void testDoubleArrayTable(
//		@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final GenericTable table = new DefaultGenericTable();
//		final DefaultColumn<DoubleArray> ij0 = new DefaultColumn<>(
//			DoubleArray.class, "H1");
//		final DefaultColumn<DoubleArray> ij1 = new DefaultColumn<>(
//			DoubleArray.class, "H2");
//		ij0.add(new DoubleArray(new double[] { 0.5, 0.25, 0.125 }));
//		ij1.add(new DoubleArray(new double[] { -0.125, -0.0625, 0.03125 }));
//		table.add(ij0);
//		table.add(ij1);
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 4l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 4l);
//				assertEquals(cols.length, table.getColumnCount());
//				assertTrue(omero.grid.DoubleArrayColumn.class.isInstance(cols[0]));
//				assertTrue(omero.grid.DoubleArrayColumn.class.isInstance(cols[1]));
//
//				final omero.grid.DoubleArrayColumn c0 =
//					(omero.grid.DoubleArrayColumn) cols[0];
//				final omero.grid.DoubleArrayColumn c1 =
//					(omero.grid.DoubleArrayColumn) cols[1];
//				assertEquals(c0.values.length, table.getRowCount());
//				assertEquals(c1.values.length, table.getRowCount());
//				assertEquals(c0.name, table.get(0).getHeader());
//				assertEquals(c1.name, table.get(1).getHeader());
//				assertEquals(c0.size, 3);
//				assertEquals(c1.size, 3);
//
//				assertArrayEquals(c0.values[0], ij0.get(0).getArray(), 0);
//				assertArrayEquals(c1.values[0], ij1.get(0).getArray(), 0);
//			}
//		};
//	}
//
//	@Test
//	public void testBoolArrayTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final GenericTable table = new DefaultGenericTable();
//		final DefaultColumn<BoolArray> ij0 = new DefaultColumn<>(
//			BoolArray.class);
//		final DefaultColumn<BoolArray> ij1 = new DefaultColumn<>(
//			BoolArray.class);
//
//		ij0.add(new BoolArray(new boolean[] { true, false }));
//		ij0.add(new BoolArray(new boolean[] { false, false }));
//		ij1.add(new BoolArray(new boolean[] { true, true }));
//		ij1.add(new BoolArray(new boolean[] { false, true }));
//
//		table.add(ij0);
//		table.add(ij1);
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 22l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 22l);
//				assertEquals(cols.length, table.getColumnCount());
//				assertTrue(omero.grid.LongArrayColumn.class.isInstance(cols[0]));
//				assertTrue(omero.grid.LongArrayColumn.class.isInstance(cols[1]));
//
//				final omero.grid.LongArrayColumn c0 =
//					(omero.grid.LongArrayColumn) cols[0];
//				final omero.grid.LongArrayColumn c1 =
//					(omero.grid.LongArrayColumn) cols[1];
//				assertEquals(c0.name, "0");
//				assertEquals(c1.name, "1");
//				assertEquals(c0.values.length, table.getRowCount());
//				assertEquals(c1.values.length, table.getRowCount());
//				assertEquals(c0.size, 2);
//				assertEquals(c1.size, 2);
//
//				for (int r = 0; r < c0.values.length; r++) {
//					for (int z = 0; z < c0.values[r].length; z++) {
//						final int i = ij0.get(r).get(z) ? 1 : 0;
//						assertEquals(c0.values[r][z], i);
//					}
//				}
//
//				for (int r = 0; r < c1.values.length; r++) {
//					for (int z = 0; z < c1.values[r].length; z++) {
//						final int i = ij1.get(r).get(z) ? 1 : 0;
//						assertEquals(c1.values[r][z], i);
//					}
//				}
//			}
//		};
//	}
//
//	@Test
//	public void testCharTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final CharTable table = new DefaultCharTable(5, 2);
//		table.get(0).fill(new char[] { 'q', 'V' });
//		table.get(1).fill(new char[] { '2', '$' });
//		table.get(2).fill(new char[] { 'b', 'a' });
//		table.get(3).fill(new char[] { '\t', '\n' });
//		table.get(4).fill(new char[] { '.', ' ' });
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 103l);
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 103l);
//				assertEquals(cols.length, table.getColumnCount());
//
//				for (int c = 0; c < table.getColumnCount(); c++) {
//					assertTrue(omero.grid.StringColumn.class.isInstance(cols[c]));
//					assertEquals(((omero.grid.StringColumn) cols[c]).values.length, table
//						.getRowCount());
//					assertEquals(((omero.grid.StringColumn) cols[c]).size, 1);
//					assertEquals(cols[c].name, String.valueOf(c));
//					for (int r = 0; r < table.getRowCount(); r++) {
//						assertEquals(((omero.grid.StringColumn) cols[c]).values[r], String
//							.valueOf(table.getValue(c, r)));
//					}
//				}
//			}
//		};
//	}
//
//	@Test
//	public void testReferenceTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final GenericTable table = new DefaultGenericTable();
//		final OMERORefColumn rc0 = new OMERORefColumn(OMERORef.WELL);
//		final OMERORefColumn rc1 = new OMERORefColumn(OMERORef.WELL);
//		final OMERORefColumn rc2 = new OMERORefColumn(OMERORef.WELL);
//		rc0.fill(new long[] { 2314, 3141324, 1235 });
//		rc1.fill(new long[] { 1234, 18367, 82156 });
//		rc2.fill(new long[] { 3198, 968431, 5489 });
//		table.add(rc0);
//		table.add(rc1);
//		table.add(rc2);
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 9l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 9l);
//				assertEquals(cols.length, table.getColumnCount());
//
//				for (int c = 0; c < table.getColumnCount(); c++) {
//					assertTrue(omero.grid.WellColumn.class.isInstance(cols[c]));
//					assertEquals(((omero.grid.WellColumn) cols[c]).values.length, table
//						.getRowCount());
//					assertEquals(cols[c].name, String.valueOf(c));
//					for (int r = 0; r < table.getRowCount(); r++) {
//						assertEquals(((omero.grid.WellColumn) cols[c]).values[r],
//							((OMERORefColumn) table.get(c)).getValue(r));
//					}
//				}
//			}
//		};
//	}
//
//	@Test
//	public void testMixedTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock,
//		@Mocked final OriginalFile fileMock) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
//		DSOutOfServiceException, DSAccessException
//	{
//		final GenericTable table = new DefaultGenericTable();
//		final BoolColumn ijc0 = new BoolColumn("h0");
//		final DoubleColumn ijc1 = new DoubleColumn("h1");
//		final DefaultColumn<String> ijc2 = new DefaultColumn<>(String.class,
//			"h2");
//		final DefaultColumn<IntArray> ijc3 = new DefaultColumn<>(
//			IntArray.class, "h3");
//		final OMERORefColumn ijc4 = new OMERORefColumn("h4", OMERORef.ROI);
//		ijc0.fill(new boolean[] { true, false });
//		ijc1.fill(new double[] { 0.03125, -2134.5 });
//		ijc2.add("abc");
//		ijc2.add("123");
//		ijc3.add(new IntArray(new int[] { 9012, 1294 }));
//		ijc3.add(new IntArray(new int[] { -4123, -9 }));
//		ijc4.fill(new long[] { 2314, 1234 });
//		table.add(ijc0);
//		table.add(ijc1);
//		table.add(ijc2);
//		table.add(ijc3);
//		table.add(ijc4);
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock,
//			fileMock, 12l);
//
//		final long id = service.uploadTable(credentials, "table", table, 0);
//
//		// Confirm method calls made in this order
//		new VerificationsInOrder() {
//
//			{
//				final omero.grid.Column[] cols;
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.newTable(anyInt, anyString);
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData(cols = withCapture()); // Capture OMERO columns
//				tableMock.getOriginalFile();
//				fileMock.getId();
//				tableMock.close();
//
//				// Tests
//				// NB: Tests must occur in verification
//				assertEquals(id, 12l);
//				assertEquals(cols.length, table.getColumnCount());
//
//				assertTrue(omero.grid.BoolColumn.class.isInstance(cols[0]));
//				assertTrue(omero.grid.DoubleColumn.class.isInstance(cols[1]));
//				assertTrue(omero.grid.StringColumn.class.isInstance(cols[2]));
//				assertTrue(omero.grid.LongArrayColumn.class.isInstance(cols[3]));
//				assertTrue(omero.grid.RoiColumn.class.isInstance(cols[4]));
//
//				final omero.grid.BoolColumn oc0 = (omero.grid.BoolColumn) cols[0];
//				final omero.grid.DoubleColumn oc1 = (omero.grid.DoubleColumn) cols[1];
//				final omero.grid.StringColumn oc2 = (omero.grid.StringColumn) cols[2];
//				final omero.grid.LongArrayColumn oc3 =
//					(omero.grid.LongArrayColumn) cols[3];
//				final omero.grid.RoiColumn oc4 = (omero.grid.RoiColumn) cols[4];
//
//				assertEquals(oc0.name, "h0");
//				assertEquals(oc1.name, "h1");
//				assertEquals(oc2.name, "h2");
//				assertEquals(oc3.name, "h3");
//				assertEquals(oc4.name, "h4");
//
//				assertEquals(oc0.values.length, ijc0.size());
//				assertEquals(oc1.values.length, ijc1.size());
//				assertEquals(oc2.values.length, ijc2.size());
//				assertEquals(oc3.values.length, ijc3.size());
//				assertEquals(oc4.values.length, ijc4.size());
//
//				assertEquals(oc2.size, 3);
//				assertEquals(oc3.size, 2);
//
//				for (int i = 0; i < oc0.values.length; i++) {
//					assertEquals(oc0.values[i], ijc0.getValue(i));
//				}
//				for (int i = 0; i < oc1.values.length; i++) {
//					assertEquals(oc1.values[i], ijc1.getValue(i), 0);
//				}
//				for (int i = 0; i < oc2.values.length; i++) {
//					assertEquals(oc2.values[i], ijc2.getValue(i));
//				}
//				for (int i = 0; i < oc3.values.length; i++) {
//					assertEquals(oc3.values[i].length, ijc3.get(i).getArray().length);
//					for (int j = 0; j < oc3.values[0].length; j++) {
//						assertEquals(oc3.values[i][j], ijc3.get(i).getArray()[j]);
//					}
//				}
//				for (int i = 0; i < oc4.values.length; i++) {
//					assertEquals(oc4.values[i], ijc4.getValue(i));
//				}
//			}
//		};
//	}
//
//	// -- Helper methods --
//
//	private void setUpMethodCalls(final DefaultOMEROSession sessionMock,
//		final Gateway gatewayMock, final SecurityContext scMock,
//		final omero.grid.SharedResourcesPrx srMock, final TablePrx tableMock,
//		final OriginalFile fileMock, final long tableID) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException,
//		DSOutOfServiceException
//	{
//		new NonStrictExpectations() {
//
//			{
//				new DefaultOMEROSession(withNotNull());
//				sessionMock.getGateway();
//				result = gatewayMock;
//				sessionMock.getSecurityContext();
//				result = scMock;
//				gatewayMock.getSharedResources(scMock);
//				result = srMock;
//				srMock.newTable(1, "table");
//				result = tableMock;
//
//				tableMock.initialize((omero.grid.Column[]) any);
//				tableMock.addData((omero.grid.Column[]) any);
//				tableMock.getOriginalFile();
//				result = fileMock;
//				fileMock.getId();
//				result = omero.rtypes.rlong(tableID);
//
//				tableMock.close();
//			}
//		};
//	}

}
