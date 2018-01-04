
package net.imagej.omero.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.scif.services.DatasetIOService;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMEROService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.scijava.Context;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;

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
}
