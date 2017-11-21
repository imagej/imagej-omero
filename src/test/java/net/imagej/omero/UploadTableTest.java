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

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
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
import net.imagej.table.Table;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.util.ByteArray;
import org.scijava.util.DoubleArray;
import org.scijava.util.IntArray;
import org.scijava.util.PrimitiveArray;

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
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.DataObject;
import omero.gateway.model.ImageData;
import omero.gateway.model.ROIData;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.gateway.model.WellSampleData;
import omero.model.RoiI;
import omero.model.WellSampleI;

/**
 * Tests {@link DefaultOMEROService#uploadTable}.
 *
 * @author Alison Walter
 */
public class UploadTableTest {

	private OMEROLocation credentials;
	private OMEROService service;

	@Mocked
	private DefaultOMEROSession session;

	@Mocked
	private Gateway gateway;

	@Mocked
	private TablesFacility tablesFacility;

	@Mocked
	private BrowseFacility browseFacility;

	@Before
	public void setUp() {
		try {
			credentials = new OMEROLocation("localhost", 4064, "omero", "omero");
		}
		catch (URISyntaxException exc) {
			exc.printStackTrace();
		}
		service = new Context(OMEROService.class).getService(OMEROService.class);
	}

	@After
	public void tearDown() {
		service.dispose();
	}

	@Test
	public void testBoolTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final BoolTable table = new DefaultBoolTable(2, 4);
		table.get(0).fill(new boolean[] { true, true, false, false });
		table.get(1).fill(new boolean[] { true, false, true, false });

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, Boolean.class);
			}
		};
	}

	@Test
	public void testShortTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final ShortTable table = new DefaultShortTable(6, 3);
		table.get(0).fill(new short[] { -32768, 0, 32767 });
		table.get(1).fill(new short[] { 12, -12, 5 });
		table.get(2).fill(new short[] { 10000, 20000, -30000 });
		table.get(3).fill(new short[] { 1111, 2222, 8888 });
		table.get(4).fill(new short[] { -4, -17, -31194 });
		table.get(5).fill(new short[] { 0, 17239, -20 });

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, Long.class);
			}
		};
	}

	@Test
	public void testLongTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final LongTable table = new DefaultLongTable(2, 2);
		table.get(0).fill(new long[] { 9223372036854775807l, 0 });
		table.get(1).fill(new long[] { -9223372036854775808l, 4 });

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, Long.class);
			}
		};
	}

	@Test
	public void testFloatTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final FloatTable table = new DefaultFloatTable(4, 2);
		table.get(0).fill(new float[] { -380129.125f, 0.25f });
		table.get(1).fill(new float[] { 9871234.0f, -12.5f });
		table.get(2).fill(new float[] { 0.0625f, 13208.03125f });
		table.get(3).fill(new float[] { -0.0625f, 1908471790.5f });

		final String[] headers = new String[] { "H1", "H2", "H3", "H4" };

		for (int i = 0; i < table.getColumnCount(); i++) {
			table.get(i).setHeader(headers[i]);
		}

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, Double.class);
			}
		};
	}

	@Test
	public void testDoubleArrayTable() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final GenericTable table = new DefaultGenericTable();
		final DefaultColumn<DoubleArray> ij0 = new DefaultColumn<>(
			DoubleArray.class, "H1");
		final DefaultColumn<DoubleArray> ij1 = new DefaultColumn<>(
			DoubleArray.class, "H2");
		ij0.add(new DoubleArray(new double[] { 0.5, 0.25, 0.125 }));
		ij1.add(new DoubleArray(new double[] { -0.125, -0.0625, 0.03125 }));
		table.add(ij0);
		table.add(ij1);

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, Double[].class);
			}
		};
	}

	@Test
	public void testByteArrayTable() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final GenericTable table = new DefaultGenericTable();
		final DefaultColumn<ByteArray> ij0 = new DefaultColumn<>(ByteArray.class);
		final DefaultColumn<ByteArray> ij1 = new DefaultColumn<>(ByteArray.class);

		ij0.add(new ByteArray(new byte[] { -128, 127 }));
		ij0.add(new ByteArray(new byte[] { 0, 10 }));
		ij1.add(new ByteArray(new byte[] { 112, 42 }));
		ij1.add(new ByteArray(new byte[] { -13, -84 }));

		table.add(ij0);
		table.add(ij1);

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, Long[].class);
			}
		};
	}

	@Test
	public void testCharTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final CharTable table = new DefaultCharTable(5, 2);
		table.get(0).fill(new char[] { 'q', 'V' });
		table.get(1).fill(new char[] { '2', '$' });
		table.get(2).fill(new char[] { 'b', 'a' });
		table.get(3).fill(new char[] { '\t', '\n' });
		table.get(4).fill(new char[] { '.', ' ' });

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, String.class);
			}
		};
	}

	@Test
	public void testReferenceTable() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final GenericTable table = new DefaultGenericTable();
		final OMERORefColumn rc0 = new OMERORefColumn(OMERORef.WELL);
		final OMERORefColumn rc1 = new OMERORefColumn(OMERORef.WELL);
		final OMERORefColumn rc2 = new OMERORefColumn(OMERORef.WELL);
		rc0.fill(new long[] { 2314, 3141324, 1235 });
		rc1.fill(new long[] { 1234, 18367, 82156 });
		rc2.fill(new long[] { 3198, 968431, 5489 });
		table.add(rc0);
		table.add(rc1);
		table.add(rc2);

		final Object[][] wells = new Object[3][3];
		for (int c = 0; c < table.getColumnCount(); c++) {
			for (int r = 0; r < table.getRowCount(); r++) {
				wells[c][r] = new WellSampleData(new WellSampleI((long) table.get(c, r),
					false));
			}
			((OMERORefColumn) table.get(c)).setOriginalData(wells[c]);
		}

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, WellSampleData.class);
			}
		};
	}

	@Test
	public void testMixedTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final GenericTable table = new DefaultGenericTable();
		final BoolColumn ijc0 = new BoolColumn("h0");
		final DoubleColumn ijc1 = new DoubleColumn("h1");
		final DefaultColumn<String> ijc2 = new DefaultColumn<>(String.class, "h2");
		final DefaultColumn<IntArray> ijc3 = new DefaultColumn<>(IntArray.class,
			"h3");
		final OMERORefColumn ijc4 = new OMERORefColumn("h4", OMERORef.ROI);
		ijc0.fill(new boolean[] { true, false });
		ijc1.fill(new double[] { 0.03125, -2134.5 });
		ijc2.add("abc");
		ijc2.add("123");
		ijc3.add(new IntArray(new int[] { 9012, 1294 }));
		ijc3.add(new IntArray(new int[] { -4123, -9 }));
		ijc4.fill(new long[] { 2314, 1234 });
		ijc4.setOriginalData(new Object[] { new ROIData(new RoiI(2314, true)),
			new ROIData(new RoiI(1234, true)) });
		table.add(ijc0);
		table.add(ijc1);
		table.add(ijc2);
		table.add(ijc3);
		table.add(ijc4);

		// Create expectations
		setUpMethodCalls();

		final long id = service.uploadTable(credentials, "table", table, 0);
		assertEquals(id, -1);

		// NB: Can only capture in a Verifications block
		new Verifications() {

			{
				TableData td;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, td = withCapture());

				tableEquals(table, td, Boolean.class, Double.class, String.class,
					Long[].class, ROIData.class);
			}
		};
	}

	// -- Helper methods --

	private void setUpMethodCalls() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, DSOutOfServiceException, ExecutionException,
		DSAccessException
	{
		new Expectations() {

			{
				new DefaultOMEROSession(credentials, service);

				gateway.getFacility(BrowseFacility.class);
				result = browseFacility;
				browseFacility.getImage((SecurityContext) any, anyLong);
				result = new ImageData();

				gateway.getFacility(TablesFacility.class);
				result = tablesFacility;
				tablesFacility.addTable((SecurityContext) any, (ImageData) any,
					anyString, (TableData) any);
				result = new TableData((TableDataColumn[]) any, (Object[][]) any);
			}
		};
	}

	private void tableEquals(final Table<?, ?> imageJTable,
		final TableData omeroTable, final Class<?>... type)
	{

		final TableDataColumn[] tdc = omeroTable.getColumns();
		final Object[][] data = omeroTable.getData();
		assertEquals(imageJTable.getColumnCount(), tdc.length);
		assertEquals(imageJTable.getRowCount(), omeroTable.getNumberOfRows());

		for (int r = 0; r < omeroTable.getNumberOfRows(); r++) {
			for (int c = 0; c < tdc.length; c++) {
				final Class<?> imageJType = type.length == 1 ? type[0] : type[c];
				assertColDataEquals(imageJTable.get(c, r), data[c][r]);
				assertEquals(imageJType, tdc[c].getType());
				assertHeadersEqual(imageJTable.getColumnHeader(c), tdc[c].getName(), c);
			}
		}
	}

	private void assertHeadersEqual(final String imageJ, final String omero,
		final int index)
	{
		if (imageJ == null) assertEquals("" + index, omero);
		else assertEquals(imageJ, omero);
	}

	private void assertColDataEquals(final Object imageJ, final Object omero) {
		if (omero instanceof Long && imageJ instanceof Number) assertEquals(
			((Number) imageJ).longValue(), ((Long) omero).longValue());
		else if (omero instanceof Double && imageJ instanceof Number) assertEquals(
			((Number) imageJ).doubleValue(), ((Double) omero).doubleValue(), 0);
		else if (omero instanceof Long[]) {
			final Long[] omeroArray = (Long[]) omero;
			final Object[] imageJArray = ((PrimitiveArray<?, ?>) imageJ).toArray();
			for (int i = 0; i < imageJArray.length; i++) {
				assertEquals(((Number) imageJArray[i]).longValue(), omeroArray[i]
					.longValue());
			}
		}
		else if (omero instanceof Double[]) {
			final Double[] omeroArray = (Double[]) omero;
			final Object[] imageJArray = ((PrimitiveArray<?, ?>) imageJ).toArray();
			for (int i = 0; i < imageJArray.length; i++) {
				assertEquals(((Number) imageJArray[i]).doubleValue(), omeroArray[i]
					.doubleValue(), 0);
			}
		}
		else if (imageJ instanceof Character) assertEquals(((Character) imageJ)
			.toString(), omero);
		else if (omero instanceof DataObject) {
			assertEquals(imageJ, ((DataObject) omero).getId());
		}
		else assertEquals(imageJ, omero);
	}

}
