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

package net.imagej.omero;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

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
import mockit.Expectations;
import mockit.Mocked;
import omero.ServerError;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.model.ImageI;

/**
 * Tests {@link DefaultOMEROService#downloadTable(OMEROLocation, long)}.
 *
 * @author Alison Walter
 */
public class DownloadTableTest {

	private OMEROLocation credentials;
	private OMEROService service;

	@Mocked
	private DefaultOMEROSession session;

	@Mocked
	private Gateway gateway;

	@Mocked
	private TablesFacility tablesFacility;

	@Before
	public void setUp() {
		try {
			credentials = new OMEROLocation("localhost", 4064, "omero", "omero");
		}
		catch (final URISyntaxException exc) {
			exc.printStackTrace();
		}
		service = new Context(OMEROService.class).getService(OMEROService.class);
	}

	@After
	public void tearDown() {
		service.dispose();
	}

	@Test
	public void downloadBoolTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		// Setup OMERO data structures
		final TableDataColumn[] tdc = new TableDataColumn[] { new TableDataColumn(
			"Header 1", 0, Boolean.class), new TableDataColumn("Header 2", 1,
				Boolean.class), new TableDataColumn("Header 3", 2, Boolean.class) };
		final Object[][] data = new Object[3][];
		data[0] = new Boolean[] { true, true, true, true, false, false, false,
			false };
		data[1] = new Boolean[] { true, true, false, false, true, true, false,
			false };
		data[2] = new Boolean[] { true, false, true, false, true, false, true,
			false };
		final TableData table = new TableData(tdc, data);
		table.setNumberOfRows(8);

		setUpMethodCalls(table);

		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
			.downloadTable(credentials, 0);

		// Tests
		assertTrue(BoolTable.class.isInstance(imageJTable));
		assertEquals(imageJTable.getColumnCount(), 3);
		assertEquals(imageJTable.getRowCount(), 8);

		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
		assertEquals(imageJTable.getColumnHeader(2), "Header 3");

		for (int r = 0; r < imageJTable.getRowCount(); r++) {
			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
				assertEquals(data[c][r], imageJTable.get(c, r));
			}
		}
	}

	@Test
	public void downloadLongTable() throws PermissionDeniedException,
		CannotCreateSessionException, ServerError, DSOutOfServiceException,
		ExecutionException, DSAccessException
	{
		// Setup OMERO data structures
		final TableDataColumn[] tdc = new TableDataColumn[] { new TableDataColumn(
			"Header 1", 0, Long.class), new TableDataColumn("Header 2", 1,
				Long.class), new TableDataColumn("Header 3", 2, Long.class),
			new TableDataColumn("Header 4", 3, Long.class) };
		final Object[][] data = new Object[4][];
		data[0] = new Long[] { 0l, -9223372036854775808l, 9223372036854775807l };
		data[1] = new Long[] { 134l, 5415145l, 4775807l };
		data[2] = new Long[] { -1898l, -97234l, 75807l };
		data[3] = new Long[] { 19048l, -123l, 4l };
		final TableData table = new TableData(tdc, data);
		table.setNumberOfRows(3);

		// Create expectations
		setUpMethodCalls(table);

		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
			.downloadTable(credentials, 0);

		// Tests
		assertTrue(LongTable.class.isInstance(imageJTable));
		assertEquals(imageJTable.getColumnCount(), 4);
		assertEquals(imageJTable.getRowCount(), 3);

		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
		assertEquals(imageJTable.getColumnHeader(2), "Header 3");
		assertEquals(imageJTable.getColumnHeader(3), "Header 4");

		for (int r = 0; r < imageJTable.getRowCount(); r++) {
			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
				assertEquals(data[c][r], imageJTable.get(c, r));
			}
		}
	}

	@Test
	public void downloadDoubleTable() throws PermissionDeniedException,
		CannotCreateSessionException, ServerError, DSOutOfServiceException,
		ExecutionException, DSAccessException
	{
		// Setup OMERO data structures
		final TableDataColumn[] tdc = new TableDataColumn[] { new TableDataColumn(
			"Header 1", 0, Double.class), new TableDataColumn("Header 2", 1,
				Double.class) };
		final Object[][] data = new Object[2][];
		data[0] = new Double[] { 0.125, -0.5, 923014712408917.25, -241.03125, 0.0 };
		data[1] = new Double[] { 1002.125, 908082.5, 59871249.0625, -7.25, 4.5 };
		final TableData table = new TableData(tdc, data);
		table.setNumberOfRows(5);

		// Create expectations
		setUpMethodCalls(table);

		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
			.downloadTable(credentials, 0);

		// Tests
		assertTrue(ResultsTable.class.isInstance(imageJTable));
		assertEquals(imageJTable.getColumnCount(), 2);
		assertEquals(imageJTable.getRowCount(), 5);

		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
		assertEquals(imageJTable.getColumnHeader(1), "Header 2");

		for (int r = 0; r < imageJTable.getRowCount(); r++) {
			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
				assertEquals((double) data[c][r], ((ResultsTable) imageJTable).get(c,
					r), 0);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void downloadLongArrayTable() throws PermissionDeniedException,
		CannotCreateSessionException, ServerError, DSOutOfServiceException,
		ExecutionException, DSAccessException
	{
		// Setup OMERO data structures
		final TableDataColumn[] tdc = new TableDataColumn[] { new TableDataColumn(
			"Header 1", 0, Long[].class), new TableDataColumn("Header 2", 1,
				Long[].class) };
		final Object[][] data = new Object[2][];
		data[0] = new Long[][] { { 0l, -9223372036854775808l }, { 134l,
			9223372036854775807l } };
		data[1] = new Long[][] { { -2139847l, 1023894l }, { 12l, 23415l } };
		final TableData table = new TableData(tdc, data);
		table.setNumberOfRows(2);

		// Create expectations
		setUpMethodCalls(table);

		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
			.downloadTable(credentials, 0);

		// Tests
		assertTrue(GenericTable.class.isInstance(imageJTable));
		assertEquals(imageJTable.getColumnCount(), 2);
		assertEquals(imageJTable.getRowCount(), 2);
		assertEquals(imageJTable.get(0).size(), 2);

		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
		assertEquals(imageJTable.getColumnHeader(1), "Header 2");

		for (int r = 0; r < imageJTable.getRowCount(); r++) {
			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
				assertTrue(DefaultColumn.class.isInstance(imageJTable.get(c)));
				assertTrue(imageJTable.get(c).getType() == LongArray.class);
				final Long[] l = (Long[]) data[c][r];
				final Object[] ijl = ((DefaultColumn<LongArray>) imageJTable.get(c))
					.get(r).toArray();
				assertArrayEquals(l, ijl);
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void downloadStringTable() throws PermissionDeniedException,
		CannotCreateSessionException, ServerError, DSOutOfServiceException,
		ExecutionException, DSAccessException
	{
		/// Setup OMERO data structures
		final TableDataColumn[] tdc = new TableDataColumn[] { new TableDataColumn(
			"Header 1", 0, String.class), new TableDataColumn("Header 2", 1,
				String.class), new TableDataColumn("Header 3", 2, String.class) };
		final Object[][] data = new Object[3][];
		data[0] = new String[] { "abc", "123", "hi!" };
		data[1] = new String[] { "Good Morning", "Good evening", "good night" };
		data[2] = new String[] { "good afternoon", "hey", "hello." };

		final TableData table = new TableData(tdc, data);
		table.setNumberOfRows(3);

		// Create expectations
		setUpMethodCalls(table);

		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
			.downloadTable(credentials, 0);

		// Tests
		assertTrue(GenericTable.class.isInstance(imageJTable));
		assertEquals(imageJTable.getColumnCount(), 3);
		assertEquals(imageJTable.getRowCount(), 3);

		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
		assertEquals(imageJTable.getColumnHeader(2), "Header 3");

		for (int r = 0; r < imageJTable.getRowCount(); r++) {
			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
				assertTrue(DefaultColumn.class.isInstance(imageJTable.get(c)));
				assertTrue(imageJTable.get(c).getType() == String.class);
				assertEquals(data[c][r], ((DefaultColumn<String>) imageJTable.get(c))
					.get(r));
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void downloadMixedTable() throws PermissionDeniedException,
		CannotCreateSessionException, ServerError, DSOutOfServiceException,
		ExecutionException, DSAccessException
	{
		/// Setup OMERO data structures
		final TableDataColumn[] tdc = new TableDataColumn[] { new TableDataColumn(
			"Header 1", 0, String.class), new TableDataColumn("Header 2", 1,
				Boolean.class), new TableDataColumn("Header 3", 2, Double[].class),
			new TableDataColumn("Header 4", 3, Long.class) };
		final Object[][] data = new Object[4][];
		data[0] = new String[] { "abc", "123", "hi!" };
		data[1] = new Boolean[] { false, true, false };
		data[2] = new Double[][] { { 0.125, 3879123.5, -93.25 }, { 0d,
			-123353.03125, -5.5 }, { 100.25, 0.125, -9000.5 } };
		data[3] = new Long[] { -9028131908l, 0l, 12l };

		final TableData table = new TableData(tdc, data);
		table.setNumberOfRows(3);

		// Create expectations
		setUpMethodCalls(table);

		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
			.downloadTable(credentials, 0);

		// Tests
		assertTrue(GenericTable.class.isInstance(imageJTable));
		assertTrue(DefaultColumn.class.isInstance(imageJTable.get(0)));
		assertTrue(imageJTable.get(0).getType() == String.class);
		assertTrue(BoolColumn.class.isInstance(imageJTable.get(1)));
		assertTrue(DefaultColumn.class.isInstance(imageJTable.get(2)));
		assertTrue(imageJTable.get(2).getType() == DoubleArray.class);
		assertTrue(LongColumn.class.isInstance(imageJTable.get(3)));

		assertEquals(imageJTable.getColumnCount(), 4);
		assertEquals(imageJTable.getRowCount(), 3);

		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
		assertEquals(imageJTable.getColumnHeader(2), "Header 3");
		assertEquals(imageJTable.getColumnHeader(3), "Header 4");

		for (int r = 0; r < imageJTable.getRowCount(); r++)
			assertEquals(data[0][r], ((DefaultColumn<String>) imageJTable.get(0)).get(
				r));

		for (int r = 0; r < imageJTable.getRowCount(); r++)
			assertEquals(data[1][r], ((BoolColumn) imageJTable.get(1)).getValue(r));

		for (int r = 0; r < imageJTable.getRowCount(); r++)
			assertArrayEquals((Double[]) data[2][r],
				((DefaultColumn<DoubleArray>) imageJTable.get(2)).get(r).toArray());

		for (int r = 0; r < imageJTable.getRowCount(); r++)
			assertEquals(data[3][r], ((LongColumn) imageJTable.get(3)).getValue(r));
	}

	@Test
	public void downloadRefTable() throws PermissionDeniedException,
		CannotCreateSessionException, ServerError, DSOutOfServiceException,
		ExecutionException, DSAccessException
	{
		final TableDataColumn[] tdc = new TableDataColumn[] { new TableDataColumn(
			"Header 1", 0, ImageData.class), new TableDataColumn("Header 2", 1,
				ImageData.class), new TableDataColumn("Header 3", 2, ImageData.class) };
		final Object[][] data = new Object[3][3];
		final long[][] ids = new long[][] { { 101, 102, 103 }, { 201, 202, 203 }, {
			301, 302, 303 } };
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				data[i][j] = new ImageData(new ImageI(ids[i][j], false));
			}
		}
		final TableData table = new TableData(tdc, data);
		table.setNumberOfRows(3);

		// Create expectations
		setUpMethodCalls(table);

		final Table<?, ?> imageJTable = ((DefaultOMEROService) service)
			.downloadTable(credentials, 0);

		// Tests
		assertTrue(GenericTable.class.isInstance(imageJTable));
		assertEquals(imageJTable.getColumnCount(), 3);
		assertEquals(imageJTable.getRowCount(), 3);

		assertEquals(imageJTable.getColumnHeader(0), "Header 1");
		assertEquals(imageJTable.getColumnHeader(1), "Header 2");
		assertEquals(imageJTable.getColumnHeader(2), "Header 3");

		for (int r = 0; r < imageJTable.getRowCount(); r++) {
			for (int c = 0; c < imageJTable.getColumnCount(); c++) {
				assertTrue(OMERORefColumn.class.isInstance(imageJTable.get(c)));
				assertTrue(imageJTable.get(c).getType() == Long.class);
				assertEquals(((OMERORefColumn) imageJTable.get(c)).getOMERORef(),
					OMERORef.IMAGE);
				assertEquals(ids[c][r], ((OMERORefColumn) imageJTable.get(c)).get(r)
					.longValue());
			}
		}
	}

	// -- Helper methods --

	private void setUpMethodCalls(final TableData table) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		new Expectations() {

			{
				new DefaultOMEROSession(credentials, service);

				gateway.getFacility(TablesFacility.class);
				result = tablesFacility;
				tablesFacility.getTable((SecurityContext) any, anyLong, anyLong,
					anyLong);
				result = table;
			}
		};
	}
}
