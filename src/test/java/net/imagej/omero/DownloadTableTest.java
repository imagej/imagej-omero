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

import net.imagej.table.BoolColumn;
import net.imagej.table.BoolTable;
import net.imagej.table.DefaultColumn;
import net.imagej.table.GenericTable;
import net.imagej.table.LongColumn;
import net.imagej.table.LongTable;
import net.imagej.table.ResultsTable;
import net.imagej.table.Table;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.util.DoubleArray;
import org.scijava.util.LongArray;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.VerificationsInOrder;
import omero.ServerError;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.grid.SharedResourcesPrx;
import omero.grid.TablePrx;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;

/**
 * Tests {@link DefaultOMEROService#downloadTable(OMEROCredentials, long)}.
 *
 * @author Alison Walter
 */
public class DownloadTableTest {

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
//	public void downloadBoolTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock)
//		throws PermissionDeniedException, CannotCreateSessionException, ServerError,
//		DSOutOfServiceException
//	{
//		// Setup OMERO data structures
//		final omero.grid.Column[] cols = new omero.grid.Column[3];
//		final omero.grid.BoolColumn bOne = new omero.grid.BoolColumn();
//		bOne.name = "Header 1";
//		bOne.values = new boolean[] { true, true, true, true, false, false, false,
//			false };
//		final omero.grid.BoolColumn bTwo = new omero.grid.BoolColumn();
//		bTwo.name = "Header 2";
//		bTwo.values = new boolean[] { true, true, false, false, true, true, false,
//			false };
//		final omero.grid.BoolColumn bThree = new omero.grid.BoolColumn();
//		bThree.name = "Header 3";
//		bThree.values = new boolean[] { true, false, true, false, true, false, true,
//			false };
//		cols[0] = bOne;
//		cols[1] = bTwo;
//		cols[2] = bThree;
//
//		final omero.grid.Data testData = new omero.grid.Data();
//		testData.columns = cols;
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock, cols,
//			testData, new long[] { 0, 1, 2 }, 8);
//
//		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
//			.downloadTable(credentials, 0);
//
//		// Verify method calls occurred in expected order
//		verify(sessionMock, gatewayMock, scMock, srMock, tableMock);
//
//		// Tests
//		assertTrue(BoolTable.class.isInstance(imageJTable));
//		assertEquals(imageJTable.getColumnCount(), 3);
//		assertEquals(imageJTable.getRowCount(), 8);
//
//		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
//		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
//		assertEquals(imageJTable.getColumnHeader(2), "Header 3");
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
//				assertEquals(((BoolTable) imageJTable).getValue(c, r),
//					((omero.grid.BoolColumn) cols[c]).values[r]);
//			}
//		}
//	}
//
//	@Test
//	public void downloadLongTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock)
//		throws PermissionDeniedException, CannotCreateSessionException, ServerError,
//		DSOutOfServiceException
//	{
//		// Setup OMERO data structures
//		final omero.grid.Column[] cols = new omero.grid.Column[4];
//		final omero.grid.LongColumn lOne = new omero.grid.LongColumn();
//		lOne.name = "Header 1";
//		lOne.values = new long[] { 0l, -9223372036854775808l,
//			9223372036854775807l };
//		final omero.grid.LongColumn lTwo = new omero.grid.LongColumn();
//		lTwo.name = "Header 2";
//		lTwo.values = new long[] { 134l, 5415145l, 4775807l };
//		final omero.grid.LongColumn lThree = new omero.grid.LongColumn();
//		lThree.name = "Header 3";
//		lThree.values = new long[] { -1898l, -97234l, 75807l };
//		final omero.grid.LongColumn lFour = new omero.grid.LongColumn();
//		lFour.name = "Header 4";
//		lFour.values = new long[] { 19048l, -123l, 4l };
//		cols[0] = lOne;
//		cols[1] = lTwo;
//		cols[2] = lThree;
//		cols[3] = lFour;
//
//		final omero.grid.Data testData = new omero.grid.Data();
//		testData.columns = cols;
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock, cols,
//			testData, new long[] { 0, 1, 2, 3 }, 3);
//
//		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
//			.downloadTable(credentials, 0);
//
//		// Verify method calls occurred in expected order
//		verify(sessionMock, gatewayMock, scMock, srMock, tableMock);
//
//		// Tests
//		assertTrue(LongTable.class.isInstance(imageJTable));
//		assertEquals(imageJTable.getColumnCount(), 4);
//		assertEquals(imageJTable.getRowCount(), 3);
//
//		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
//		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
//		assertEquals(imageJTable.getColumnHeader(2), "Header 3");
//		assertEquals(imageJTable.getColumnHeader(3), "Header 4");
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
//				assertEquals(((LongTable) imageJTable).getValue(c, r),
//					((omero.grid.LongColumn) cols[c]).values[r]);
//			}
//		}
//	}
//
//	@Test
//	public void downloadDoubleTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock)
//		throws PermissionDeniedException, CannotCreateSessionException, ServerError,
//		DSOutOfServiceException
//	{
//		// Setup OMERO data structures
//		final omero.grid.Column[] cols = new omero.grid.Column[2];
//		final omero.grid.DoubleColumn dOne = new omero.grid.DoubleColumn();
//		dOne.name = "Header 1";
//		dOne.values = new double[] { 0.125, -0.5, 923014712408917.25, -241.03125,
//			0.0 };
//		final omero.grid.DoubleColumn dTwo = new omero.grid.DoubleColumn();
//		dTwo.name = "Header 2";
//		dTwo.values = new double[] { 1002.125, 908082.5, 59871249.0625, -7.25,
//			4.5 };
//		cols[0] = dOne;
//		cols[1] = dTwo;
//
//		final omero.grid.Data testData = new omero.grid.Data();
//		testData.columns = cols;
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock, cols,
//			testData, new long[] { 0, 1 }, 5);
//
//		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
//			.downloadTable(credentials, 0);
//
//		// Verify method calls occurred in expected order
//		verify(sessionMock, gatewayMock, scMock, srMock, tableMock);
//
//		// Tests
//		assertTrue(ResultsTable.class.isInstance(imageJTable));
//		assertEquals(imageJTable.getColumnCount(), 2);
//		assertEquals(imageJTable.getRowCount(), 5);
//
//		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
//		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
//				assertEquals(((ResultsTable) imageJTable).getValue(c, r),
//					((omero.grid.DoubleColumn) cols[c]).values[r], 0);
//			}
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void downloadLongArrayTable(
//		@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock)
//		throws PermissionDeniedException, CannotCreateSessionException, ServerError,
//		DSOutOfServiceException
//	{
//		// Setup OMERO data structures
//		final omero.grid.Column[] cols = new omero.grid.Column[2];
//		final omero.grid.LongArrayColumn laOne = new omero.grid.LongArrayColumn();
//		laOne.name = "Header 1";
//		laOne.values = new long[][] { { 0l, -9223372036854775808l }, { 134l,
//			9223372036854775807l } };
//		final omero.grid.LongArrayColumn laTwo = new omero.grid.LongArrayColumn();
//		laTwo.name = "Header 2";
//		laTwo.values = new long[][] { { -2139847, 1023894 }, { 12, 23415 } };
//		cols[0] = laOne;
//		cols[1] = laTwo;
//
//		final omero.grid.Data testData = new omero.grid.Data();
//		testData.columns = cols;
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock, cols,
//			testData, new long[] { 0, 1 }, 2);
//
//		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
//			.downloadTable(credentials, 0);
//
//		// Verify method calls occurred in expected order
//		verify(sessionMock, gatewayMock, scMock, srMock, tableMock);
//
//		// Tests
//		assertTrue(GenericTable.class.isInstance(imageJTable));
//		assertEquals(imageJTable.getColumnCount(), 2);
//		assertEquals(imageJTable.getRowCount(), 2);
//		assertEquals(imageJTable.get(0).size(), 2);
//
//		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
//		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
//				assertTrue(DefaultColumn.class.isInstance(imageJTable.get(c)));
//				assertTrue(imageJTable.get(c).getType() == LongArray.class);
//				assertArrayEquals(((DefaultColumn<LongArray>) imageJTable.get(c)).get(r)
//					.getArray(), ((omero.grid.LongArrayColumn) cols[c]).values[r]);
//			}
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void downloadStringTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock)
//		throws PermissionDeniedException, CannotCreateSessionException, ServerError,
//		DSOutOfServiceException
//	{
//		// Setup OMERO data structures
//		final omero.grid.Column[] cols = new omero.grid.Column[3];
//		final omero.grid.StringColumn sOne = new omero.grid.StringColumn();
//		sOne.name = "Header 1";
//		sOne.values = new String[] { "abc", "123", "hi!" };
//		final omero.grid.StringColumn sTwo = new omero.grid.StringColumn();
//		sTwo.name = "Header 2";
//		sTwo.values = new String[] { "Good Morning", "Good evening", "good night" };
//		final omero.grid.StringColumn sThree = new omero.grid.StringColumn();
//		sThree.name = "Header 3";
//		sThree.values = new String[] { "good afternoon", "hey", "hello." };
//		cols[0] = sOne;
//		cols[1] = sTwo;
//		cols[2] = sThree;
//
//		final omero.grid.Data testData = new omero.grid.Data();
//		testData.columns = cols;
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock, cols,
//			testData, new long[] { 0, 1, 2 }, 3);
//
//		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
//			.downloadTable(credentials, 0);
//
//		// Verify method calls occurred in expected order
//		verify(sessionMock, gatewayMock, scMock, srMock, tableMock);
//
//		// Tests
//		assertTrue(GenericTable.class.isInstance(imageJTable));
//		assertEquals(imageJTable.getColumnCount(), 3);
//		assertEquals(imageJTable.getRowCount(), 3);
//
//		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
//		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
//		assertEquals(imageJTable.getColumnHeader(2), "Header 3");
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
//				assertTrue(DefaultColumn.class.isInstance(imageJTable.get(c)));
//				assertTrue(imageJTable.get(c).getType() == String.class);
//				assertEquals(((DefaultColumn<String>) imageJTable.get(c)).get(r),
//					((omero.grid.StringColumn) cols[c]).values[r]);
//			}
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void downloadMixedTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock)
//		throws PermissionDeniedException, CannotCreateSessionException, ServerError,
//		DSOutOfServiceException
//	{
//		// Setup OMERO data structures
//		final omero.grid.Column[] cols = new omero.grid.Column[4];
//		final omero.grid.StringColumn mOne = new omero.grid.StringColumn();
//		mOne.name = "Header 1";
//		mOne.values = new String[] { "abc", "123", "hi!" };
//		final omero.grid.BoolColumn mTwo = new omero.grid.BoolColumn();
//		mTwo.name = "Header 2";
//		mTwo.values = new boolean[] { false, true, false };
//		final omero.grid.DoubleArrayColumn mThree =
//			new omero.grid.DoubleArrayColumn();
//		mThree.name = "Header 3";
//		mThree.values = new double[][] { { 0.125, 3879123.5, -93.25 }, { 0,
//			-123353.03125, -5.5 }, { 100.25, 0.125, -9000.5 } };
//		final omero.grid.LongColumn mFour = new omero.grid.LongColumn();
//		mFour.name = "Header 4";
//		mFour.values = new long[] { -9028131908l, 0, 12 };
//		cols[0] = mOne;
//		cols[1] = mTwo;
//		cols[2] = mThree;
//		cols[3] = mFour;
//
//		final omero.grid.Data testData = new omero.grid.Data();
//		testData.columns = cols;
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock, cols,
//			testData, new long[] { 0, 1, 2, 3 }, 3);
//
//		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
//			.downloadTable(credentials, 0);
//
//		// Verify method calls occurred in expected order
//		verify(sessionMock, gatewayMock, scMock, srMock, tableMock);
//
//		// Tests
//		assertTrue(GenericTable.class.isInstance(imageJTable));
//		assertTrue(DefaultColumn.class.isInstance(imageJTable.get(0)));
//		assertTrue(imageJTable.get(0).getType() == String.class);
//		assertTrue(BoolColumn.class.isInstance(imageJTable.get(1)));
//		assertTrue(DefaultColumn.class.isInstance(imageJTable.get(2)));
//		assertTrue(imageJTable.get(2).getType() == DoubleArray.class);
//		assertTrue(LongColumn.class.isInstance(imageJTable.get(3)));
//
//		assertEquals(imageJTable.getColumnCount(), 4);
//		assertEquals(imageJTable.getRowCount(), 3);
//
//		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
//		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
//		assertEquals(imageJTable.getColumnHeader(2), "Header 3");
//		assertEquals(imageJTable.getColumnHeader(3), "Header 4");
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			assertEquals(((DefaultColumn<String>) imageJTable.get(0)).get(r),
//				((omero.grid.StringColumn) cols[0]).values[r]);
//		}
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			assertEquals(((BoolColumn) imageJTable.get(1)).getValue(r),
//				((omero.grid.BoolColumn) cols[1]).values[r]);
//		}
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			assertArrayEquals(((DefaultColumn<DoubleArray>) imageJTable.get(2)).get(r)
//				.getArray(), ((omero.grid.DoubleArrayColumn) cols[2]).values[r], 0);
//		}
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			assertEquals(((LongColumn) imageJTable.get(3)).getValue(r),
//				((omero.grid.LongColumn) cols[3]).values[r]);
//		}
//	}
//
//	@Test
//	public void downloadRefTable(@Mocked final DefaultOMEROSession sessionMock,
//		@Mocked final Gateway gatewayMock, @Mocked final SecurityContext scMock,
//		@Mocked final SharedResourcesPrx srMock, @Mocked final TablePrx tableMock)
//		throws PermissionDeniedException, CannotCreateSessionException, ServerError,
//		DSOutOfServiceException
//	{
//		// Setup OMERO data structures
//		final omero.grid.Column[] cols = new omero.grid.Column[3];
//		final omero.grid.ImageColumn rOne = new omero.grid.ImageColumn();
//		rOne.name = "Header 1";
//		rOne.values = new long[] { 101, 102, 103 };
//		final omero.grid.ImageColumn rTwo = new omero.grid.ImageColumn();
//		rTwo.name = "Header 2";
//		rTwo.values = new long[] { 201, 202, 203 };
//		final omero.grid.ImageColumn rThree = new omero.grid.ImageColumn();
//		rThree.name = "Header 3";
//		rThree.values = new long[] { 301, 302, 303 };
//		cols[0] = rOne;
//		cols[1] = rTwo;
//		cols[2] = rThree;
//
//		final omero.grid.Data testData = new omero.grid.Data();
//		testData.columns = cols;
//
//		// Create expectations
//		// NB: Cannot pass parameters to @Before methods
//		setUpMethodCalls(sessionMock, gatewayMock, scMock, srMock, tableMock, cols,
//			testData, new long[] { 0, 1, 2 }, 3);
//
//		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
//			.downloadTable(credentials, 0);
//
//		// Verify method calls occurred in expected order
//		verify(sessionMock, gatewayMock, scMock, srMock, tableMock);
//
//		// Tests
//		assertTrue(GenericTable.class.isInstance(imageJTable));
//		assertEquals(imageJTable.getColumnCount(), 3);
//		assertEquals(imageJTable.getRowCount(), 3);
//
//		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
//		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
//		assertEquals(imageJTable.getColumnHeader(2), "Header 3");
//
//		for (int r = 0; r < imageJTable.getRowCount(); r++) {
//			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
//				assertTrue(OMERORefColumn.class.isInstance(imageJTable.get(c)));
//				assertTrue(imageJTable.get(c).getType() == Long.class);
//				assertEquals(((OMERORefColumn) imageJTable.get(c)).getOMERORef(),
//					OMERORef.IMAGE);
//				assertEquals(((OMERORefColumn) imageJTable.get(c)).get(r).longValue(),
//					((omero.grid.ImageColumn) cols[c]).values[r]);
//			}
//		}
//	}
//
//	// -- Helper methods --
//
//	private void setUpMethodCalls(final DefaultOMEROSession sessionMock,
//		final Gateway gatewayMock, final SecurityContext scMock,
//		final SharedResourcesPrx srMock, final TablePrx tableMock,
//		final omero.grid.Column[] cols, final omero.grid.Data testData,
//		final long[] ind, final int rows) throws ServerError,
//		PermissionDeniedException, CannotCreateSessionException,
//		DSOutOfServiceException
//	{
//		new NonStrictExpectations() {
//
//			{
//				// Expect constructors
//				new DefaultOMEROSession(withNotNull());
//				new OriginalFileI(anyLong, false);
//
//				sessionMock.getGateway();
//				result = gatewayMock;
//				sessionMock.getSecurityContext();
//				result = scMock;
//				gatewayMock.getSharedResources(scMock);
//				result = srMock;
//				srMock.openTable((OriginalFile) any);
//				result = tableMock;
//
//				tableMock.getNumberOfRows();
//				result = rows;
//
//				tableMock.getHeaders();
//				result = cols;
//
//				tableMock.getHeaders();
//				result = cols;
//
//				tableMock.read(ind, 0, rows);
//				result = testData;
//
//				tableMock.close();
//			}
//		};
//	}
//
//	private void verify(final DefaultOMEROSession sessionMock,
//		final Gateway gatewayMock, final SecurityContext scMock,
//		final SharedResourcesPrx srMock, final TablePrx tableMock)
//		throws ServerError, DSOutOfServiceException
//	{
//		new VerificationsInOrder() {
//
//			{
//				sessionMock.getGateway();
//				sessionMock.getSecurityContext();
//				gatewayMock.getSharedResources(scMock);
//				srMock.openTable((OriginalFile) any);
//				tableMock.getNumberOfRows();
//				tableMock.getHeaders();
//				tableMock.getHeaders();
//				tableMock.read((long[]) any, anyLong, anyLong);
//				tableMock.close();
//			}
//		};
//	}
}
