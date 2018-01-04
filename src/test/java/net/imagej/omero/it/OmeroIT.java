
package net.imagej.omero.it;

import static org.junit.Assert.assertNotNull;

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
}
