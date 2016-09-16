package utils;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import net.imagej.omero.DefaultOMEROSession;
import net.imagej.omero.OMEROCredentials;
import omero.ServerError;

public class NonClosingOMEROSession extends DefaultOMEROSession {

	public NonClosingOMEROSession(OMEROCredentials credentials)
			throws ServerError, PermissionDeniedException, CannotCreateSessionException {
		super(credentials);
	}

	public NonClosingOMEROSession(OMEROCredentials credentials, omero.client client)
			throws ServerError, PermissionDeniedException, CannotCreateSessionException {
		super(credentials, client);
	}

	/**
	 * NO OP implementation, use {@link #terminate()} to close the connection
	 */
	@Override
	public void close() {
		// NO OP
	}

	/**
	 * Closes the connection.
	 */
	public void terminate(){
		super.close();
	}


}
