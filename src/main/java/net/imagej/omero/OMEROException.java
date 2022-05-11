package net.imagej.omero;

/**
 * A checked exception encompassing the various things that can go wrong when
 * communicating with an OMERO server. Check the exception cause for more
 * details on the specific issue.
 * 
 * @author Curtis Rueden
 */
public class OMEROException extends Exception {

	public OMEROException(final String message) {
		super(message);
	}

	public OMEROException(final Throwable cause) {
		super(cause);
	}

	public OMEROException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
