
package net.imagej.omero;

import io.scif.FormatException;
import io.scif.img.ImgIOException;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.table.Table;
import net.imglib2.type.numeric.RealType;

import org.scijava.service.Service;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;

/**
 * Service that provides methods to transfer data to and from an OMERO Server
 *
 * @author gabriel
 */
public interface OMEROTransferService extends Service {

	/**
	 * Downloads the image with the given image ID from OMERO, storing the result
	 * into a new ImageJ {@link Dataset}.
	 */
	Dataset downloadImage(omero.client client, long imageID)
		throws omero.ServerError, IOException;

	/**
	 * Uploads the given ImageJ {@link Dataset}'s image to OMERO, returning the
	 * new image ID on the OMERO server.
	 */
	long uploadImage(omero.client client, Dataset dataset)
		throws omero.ServerError, IOException;

	/**
	 * Uploads an ImageJ table to OMERO, returning the new table ID on the OMERO
	 * server.
	 */
	long uploadTable(OMEROCredentials credentials, String name,
		Table<?, ?> imageJTable, final long imageID) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException;

	/**
	 * Downloads the table with the given ID from OMERO, storing the result into a
	 * new ImageJ {@link Table}.
	 */
	Table<?, ?> downloadTable(OMEROCredentials credentials, long tableID)
		throws ServerError, PermissionDeniedException, CannotCreateSessionException;

	Collection<ImgPlus<?>> downloadImageSet(OMEROCredentials creds,
		Collection<Long> ids) throws IOException;

}
