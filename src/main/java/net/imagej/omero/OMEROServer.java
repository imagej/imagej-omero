
package net.imagej.omero;

import java.util.Objects;

/**
 * Helper class for storing an OMERO server address (host + port).
 *
 * @author Curtis Rueden
 */
public class OMEROServer {

	public final String host;
	public final int port;

	public OMEROServer(final String host, final boolean encrypted) {
		this(host, encrypted ? 4064 : 4063);
	}

	public OMEROServer(final String host, final int port) {
		this.host = host.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","");
		this.port = port;
	}

	// -- Object methods --

	@Override
	public int hashCode() {
		return host.hashCode() ^ port;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof OMEROServer)) return false;
		final OMEROServer that = (OMEROServer) o;
		return Objects.equals(this.host, that.host) && this.port == that.port;
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}
}
