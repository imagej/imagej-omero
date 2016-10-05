
package net.imagej.omero;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.table.Table;

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

	/**
	 * Downloads all images given by id.
	 * 
	 * @param credentials the credentials for the server
	 * @param ids the ids of the images to download
	 * @return A collection containing the images
	 * @throws IOException
	 */
	Collection<ImgPlus<?>> downloadImageSet(OMEROCredentials credentials,
		Collection<Long> ids) throws IOException;

	/**
	 * Returns a stream that downloads images on demand from an omero server.
	 * 
	 * @param creds the credentials for the server
	 * @param ids the ids of the images to download
	 * @return A stream containing the downloaded images
	 */
	Stream<ImgPlus<?>> streamDownloadImageSet(OMEROCredentials creds,
		Collection<Long> ids);

}
