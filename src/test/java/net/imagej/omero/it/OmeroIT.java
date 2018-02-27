
package net.imagej.omero.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.scif.services.DatasetIOService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.omero.DefaultOMEROSession;
import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.table.ByteTable;
import net.imagej.table.DefaultByteTable;
import net.imagej.table.Table;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.scijava.Context;

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
	private OMEROCredentials cred;
	private Context context;
	private OMEROService omero;

	private static final String OMERO_SERVER = "omero";
	private static final int OMERO_PORT = 4064;
	private static final String OMERO_USER = "root";
	private static final String OMERO_PASSWORD = "omero";

	@Before
	public void setup() {
		client = new omero.client(OMERO_SERVER, OMERO_PORT);
		cred = new OMEROCredentials();
		cred.setServer(OMERO_SERVER);
		cred.setPort(OMERO_PORT);
		cred.setUser(OMERO_USER);
		cred.setPassword(OMERO_PASSWORD);

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
	public void testDownloadImage() throws ServerError, IOException,
		CannotCreateSessionException, PermissionDeniedException
	{
		client.createSession(OMERO_USER, OMERO_PASSWORD);
		final Dataset d = omero.downloadImage(client, 1);
		client.closeSession();

		assertNotNull(d.getImgPlus());
	}

	@Test
	public void testUploadImage() throws IOException, ServerError,
		CannotCreateSessionException, PermissionDeniedException
	{
		final DatasetIOService io = context.getService(DatasetIOService.class);
		final Dataset d = io.open("http://imagej.net/images/blobs.gif");

		client.createSession(OMERO_USER, OMERO_PASSWORD);
		final long id = omero.uploadImage(client, d);
		client.closeSession();

		assertTrue(id > 0);
	}

	@Test
	public void testDownloadThenUploadImage() throws CannotCreateSessionException,
		PermissionDeniedException, ServerError, IOException
	{
		final long originalId = 1;
		client.createSession(OMERO_USER, OMERO_PASSWORD);

		final Dataset d = omero.downloadImage(client, originalId);
		final long newId = omero.uploadImage(client, d);

		client.closeSession();

		assertTrue(originalId != newId);
	}

	@Test
	public void testDownloadTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		// now download the table
		final Table<?, ?> ijTable = omero.downloadTable(cred, 83);

		assertNotNull(ijTable);
		assertEquals(3, ijTable.getColumnCount());
		assertEquals(3, ijTable.getRowCount());
	}

	@Test
	public void testUploadTable() throws ServerError, PermissionDeniedException,
		CannotCreateSessionException, ExecutionException, DSOutOfServiceException,
		DSAccessException
	{
		final byte[][] d = new byte[][] {
			{ 127, 0, -128 },
			{ -1, -6, -23 },
			{ 100, 87, 4 }
		};
		final ByteTable table = new DefaultByteTable(3, 3);
		for (int c = 0; c < table.getColumnCount(); c++) {
			table.setColumnHeader(c, "Heading " + (c + 1));
			for (int r = 0; r < table.getRowCount(); r++)
				table.set(c, r, d[c][r]);
		}

		final long tableId = omero.uploadTable(cred, "test-table-upload", table, 1);

		// When upload table was called it created a session, which cleared out
		// the username and password from the credentials. The credentials must
		// have a username and password to create security contexts.
		final OMEROCredentials tc = new OMEROCredentials();
		tc.setServer(OMERO_SERVER);
		tc.setPort(OMERO_PORT);
		tc.setUser(OMERO_USER);
		tc.setPassword(OMERO_PASSWORD);

		try (final OMEROSession session = new DefaultOMEROSession(tc)) {
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
	}

	@Test
	public void testDownloadThenUploadTable() throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final long originalId = 83;
		final Table<?, ?> ijTable = omero.downloadTable(cred, originalId);

		// When download table was called it created a session, which cleared out
		// the username and password from the credentials. The credentials must
		// have a username and password to create security contexts.
		final OMEROCredentials tc = new OMEROCredentials();
		tc.setServer(OMERO_SERVER);
		tc.setPort(OMERO_PORT);
		tc.setUser(OMERO_USER);
		tc.setPassword(OMERO_PASSWORD);

		final long newId = omero.uploadTable(tc, "table-version2", ijTable, 1);

		assertTrue(originalId != newId);
	}
}
