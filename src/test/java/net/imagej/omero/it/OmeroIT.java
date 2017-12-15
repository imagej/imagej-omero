
package net.imagej.omero.it;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;

@Category(IntegrationTest.class)
public class OmeroIT {

	private omero.client client;

	@Test
	public void testConnectingToServer() throws CannotCreateSessionException,
		PermissionDeniedException, ServerError
	{
		client = new omero.client("omero", 4064);
		client.createSession("root", "omero");
	}
}
