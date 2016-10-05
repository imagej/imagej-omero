
package net.imagej.omero;

import io.scif.FormatException;
import io.scif.Metadata;
import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.filters.ReaderFilter;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.ImgUtilityService;
import io.scif.img.SCIFIOImgPlus;
import io.scif.services.DatasetIOService;
import io.scif.services.FormatService;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.display.ImageDisplayService;
import net.imagej.table.Column;
import net.imagej.table.GenericTable;
import net.imagej.table.Table;

import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.grid.TablePrx;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.ImageAnnotationLink;
import omero.model.ImageAnnotationLinkI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import utils.NonClosingOMEROSession;

@Plugin(type = Service.class)
public class DefaultOMEROTransferService extends AbstractService implements
	OMEROTransferService
{
	// -- Parameters --

	@Parameter
	private LogService log;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private FormatService formatService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ObjectService objectService;

	@Parameter
	private ConvertService convertService;

	@Parameter
	private ImgUtilityService imgUtils;

	@Override
	public Dataset downloadImage(final omero.client client, final long imageID)
		throws omero.ServerError, IOException
	{
		// TODO: Reuse existing client instead of creating a new connection.
		// Will need to rethink how SCIFIO conveys source and destination metadata.
		// The RandomAccessInput/OutputStream design is probably too narrow.
		final String omeroSource = "omero:" + credentials(client) + "&imageID=" +
			imageID;

		return datasetIOService.open(omeroSource);
	}

	@Override
	public long uploadImage(final omero.client client, final Dataset dataset)
		throws omero.ServerError, IOException
	{
		// TODO: Reuse existing client instead of creating a new connection.
		// Will need to rethink how SCIFIO conveys source and destination metadata.
		// The RandomAccessInput/OutputStream design is probably too narrow.
		final String omeroDestination = "name=" + dataset.getName() + "&" +
			credentials(client) //
			+ ".omero"; // FIXME: Remove this after SCIFIO doesn't need it anymore.

		final Metadata metadata = datasetIOService.save(dataset, omeroDestination);

		if (metadata instanceof OMEROFormat.Metadata) {
			final OMEROFormat.Metadata omeroMeta = (OMEROFormat.Metadata) metadata;
			return omeroMeta.getImageID();
		}
		return -1;
	}

	@Override
	public long uploadTable(final OMEROCredentials credentials, final String name,
		final Table<?, ?> imageJTable, final long imageID) throws ServerError,
		PermissionDeniedException, CannotCreateSessionException, ExecutionException,
		DSOutOfServiceException, DSAccessException
	{
		final OMEROSession session = new DefaultOMEROSession(credentials);
		TablePrx tableService = null;
		long id = -1;
		try {
			tableService = session.getClient().getSession().sharedResources()
				.newTable(1, name);
			if (tableService == null) {
				throw new omero.ServerError(null, null, "Could not create table");
			}
			final omero.grid.Column[] columns = new omero.grid.Column[imageJTable
				.getColumnCount()];
			for (int c = 0; c < columns.length; c++) {
				columns[c] = TableUtils.createOMEROColumn(imageJTable.get(c), c);
			}
			tableService.initialize(columns);
			// TODO: Can reuse OMERO column structs (from 0 index every time)
			// to append rows in batches, in case there are too many.
			// If we do this, we can report progress better.
			for (int c = 0; c < columns.length; c++) {
				TableUtils.populateOMEROColumn(imageJTable.get(c), columns[c]);
			}
			tableService.addData(columns);
			if (imageID != 0) attachAnnotation(session, imageID, tableService);
		}
		finally {
			try {
				if (tableService != null) {
					id = tableService.getOriginalFile().getId().getValue();
					tableService.close();
				}
			}
			finally {
				((DefaultOMEROSession) session).close();
			}
		}
		return id;
	}

	@Override
	public Table<?, ?> downloadTable(final OMEROCredentials credentials,
		final long tableID) throws ServerError, PermissionDeniedException,
		CannotCreateSessionException
	{
		final OMEROSession session = new DefaultOMEROSession(credentials);
		TablePrx tableService = null;
		try {
			final OriginalFile tableFile = new OriginalFileI(tableID, false);
			tableService = session.getClient().getSession().sharedResources()
				.openTable(tableFile);
			if (tableService == null) {
				throw new omero.ServerError(null, null, "Could not open table");
			}
			final long rawRowCount = tableService.getNumberOfRows();
			if (rawRowCount > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Table too large: " + rawRowCount +
					" rows");
			}
			final int rowCount = (int) rawRowCount;
			final omero.grid.Column[] omeroColumns = tableService.getHeaders();

			final Table<?, ?> imageJTable = TableUtils.createImageJTable(
				omeroColumns);
			final int colCount = tableService.getHeaders().length;

			final int batchSize = 24 * 1024;
			int currentRow = 0;

			final Column<?>[] imageJColumns = new Column<?>[colCount];
			final long[] colIndices = new long[colCount];
			for (int c = 0; c < colIndices.length; c++)
				colIndices[c] = c;
			boolean colCreated = false;

			while (currentRow < rowCount) {
				final int rowsLeft = rowCount - currentRow;
				final int rowsToRead = Math.min(batchSize, rowsLeft);
				final omero.grid.Data data = tableService.read(colIndices, currentRow,
					currentRow + rowsToRead);
				assert (colCount == data.columns.length);
				// Create columns
				if (!colCreated) {
					if (GenericTable.class.isInstance(imageJTable)) addTypedColumns(
						(GenericTable) imageJTable, data, rowCount);
					// Append empty columns of table type
					else imageJTable.appendColumns(colCount);
					colCreated = true;
				}
				for (int c = 0; c < colCount; c++) {
					if (imageJColumns[c] == null) {
						imageJColumns[c] = imageJTable.get(c);
					}
					TableUtils.populateImageJColumn(data.columns[c], imageJColumns[c],
						currentRow);
				}
				currentRow += rowsToRead;
			}
			// Need to specify how many rows the table has for data to display
			imageJTable.setRowCount(rowCount);
			return imageJTable;
		}
		finally {
			try {
				if (tableService != null) {
					tableService.close();
				}
			}
			finally {
				((DefaultOMEROSession) session).close();
			}
		}
	}

	@Override
	public Collection<ImgPlus<?>> downloadImageSet(
		final OMEROCredentials credentials, Collection<Long> ids)
		throws IOException
	{
		final List<ImgPlus<?>> images = new LinkedList<>(); // to store the result

		NonClosingOMEROSession session = null;

		try {
			session = new NonClosingOMEROSession(credentials);

			// Reader filter stuff
			final OMEROFormat format = formatService.getFormatFromClass(
				OMEROFormat.class);
			OMEROFormat.Reader reader = (OMEROFormat.Reader) format.createReader();
			reader.setSession(session);

			ReaderFilter rf = new ReaderFilter(reader);
//			rf.enable(ChannelFiller.class);
//			rf.enable(PlaneSeparator.class).separate(SCIFIOUtilMethods.axesToSplit(rf));

			// Config
			SCIFIOConfig config = new SCIFIOConfig();
			config.imgOpenerSetComputeMinMax(false); // skip min max compute
			config.imgOpenerSetImgModes(ImgMode.PLANAR); // prefer planar

			// Parser
			OMEROFormat.Parser parser = (OMEROFormat.Parser) format.createParser();
			parser.setSession(session);

			ImgOpener opener = new ImgOpener(getContext());

			// read images
			for (Long id : ids) {
				final String omeroSource = "omero:" + "DUMMY" + "&imageID=" + id;
				OMEROFormat.Metadata metadata = parser.parse(omeroSource, config);
				rf.setMetadata(metadata);
				rf.setSource(omeroSource);
				reader.setPixelStore();

				List<SCIFIOImgPlus<?>> imgs = opener.openImgs(rf, config);
				images.addAll(imgs);
			}
			// close the connection
			session.terminate();

		}
		catch (IOException | ImgIOException | FormatException | ServerError
				| PermissionDeniedException | CannotCreateSessionException e)
		{
			if (session != null) {
				session.terminate();
			}
			throw new IOException("Failed to read from omero server:", e);
		}
		return images;
	}

	@Override
	public Stream<ImgPlus<?>> streamDownloadImageSet(OMEROCredentials creds,
		Collection<Long> ids)
	{

		Queue<Long> elements = new PriorityQueue<>(ids);
		final NonClosingOMEROSession session;
		final OMEROFormat format;
		final OMEROFormat.Reader reader;
		ReaderFilter rf;
		SCIFIOConfig config;
		OMEROFormat.Parser parser;
		ImgOpener opener;

		try {
			session = new NonClosingOMEROSession(creds);

			// Reader filter stuff
			format = formatService.getFormatFromClass(OMEROFormat.class);
			reader = (OMEROFormat.Reader) format.createReader();
			reader.setSession(session);

//					rf.enable(ChannelFiller.class);
//					rf.enable(PlaneSeparator.class).separate(SCIFIOUtilMethods.axesToSplit(rf));
			rf = new ReaderFilter(reader);

			// Config
			config = new SCIFIOConfig();
			config.imgOpenerSetComputeMinMax(false); // skip min max compute
			config.imgOpenerSetImgModes(ImgMode.PLANAR); // prefer planar

			// Parser
			parser = (OMEROFormat.Parser) format.createParser();
			parser.setSession(session);

			opener = new ImgOpener(getContext());
		}
		catch (CannotCreateSessionException | PermissionDeniedException
				| ServerError | FormatException e)
		{
			return null;
			// TODO Handle
		}

		Spliterator<ImgPlus<?>> omeroSplitterator = new Spliterator<ImgPlus<?>>() {

			@Override
			public Spliterator<ImgPlus<?>> trySplit() {
				// Currently no support for splitting
				return null;
			}

			@Override
			public long estimateSize() {
				return elements.size();
			}

			@Override
			public int characteristics() {
				return IMMUTABLE;
			}

			@Override
			public boolean tryAdvance(Consumer<? super ImgPlus<?>> action) {
				// there are elements to read
				if (!elements.isEmpty()) {
					try {
						long id = elements.poll();
						// read images
						final String omeroSource = "omero:" + "DUMMY" + "&imageID=" + id;
						OMEROFormat.Metadata metadata = parser.parse(omeroSource, config);
						rf.setMetadata(metadata);
						rf.setSource(omeroSource);
						reader.setPixelStore();

						List<SCIFIOImgPlus<?>> imgs = opener.openImgs(rf, config);
						action.accept(imgs.get(0)); // return more than one img
					}
					catch (IOException | ImgIOException | FormatException
							| ServerError e)
					{
						log.error("Failed to read from omero server", e);
						session.terminate();
						return false;

					}
					if (elements.isEmpty()) {
						session.terminate();
					}
					return true;
				}
				// No more images available.
				return false;
			}
		};

		return StreamSupport.stream(omeroSplitterator, false);

	}

	/**
	 * Generates an OMERO source string fragment with credentials matching the
	 * given client.
	 */
	private static String credentials(final omero.client client) {
		return "server=" + client.getProperty("omero.host") + //
			"&port=" + client.getProperty("omero.port") + //
			"&sessionID=" + client.getSessionId();
	}

	/**
	 * Attaches table file to OMERO image.
	 *
	 * @throws ExecutionException
	 * @throws DSAccessException
	 * @throws DSOutOfServiceException
	 */
	private static void attachAnnotation(final OMEROSession session,
		final long imageID, final TablePrx table) throws ServerError,
		ExecutionException, DSOutOfServiceException, DSAccessException
	{
		// Create necessary facilities
		final DataManagerFacility dm = session.getGateway().getFacility(
			DataManagerFacility.class);
		final BrowseFacility browse = session.getGateway().getFacility(
			BrowseFacility.class);

		// Get current sessions security context
		final SecurityContext ctx = new SecurityContext(session.getExperimenter()
			.getGroupId());

		// Get original file from the table
		final OriginalFile file = table.getOriginalFile();

		// Create file annotation for table file
		FileAnnotation annotation = new FileAnnotationI();
		// TODO assign annotation to a table namespace
		annotation.setNs(omero.rtypes.rstring(
			omero.constants.namespaces.NSBULKANNOTATIONS.value));
		annotation.setFile(file);
		// Save file annotation to database
		annotation = (FileAnnotation) dm.saveAndReturnObject(ctx, annotation);

		// Attach file to image with given ID
		ImageAnnotationLink link = new ImageAnnotationLinkI();
		link.setChild(annotation);
		link.setParent(browse.getImage(ctx, imageID).asImage());
		// Save linkage to database
		link = (ImageAnnotationLink) dm.saveAndReturnObject(ctx, link);
	}

	/**
	 * Adds columns of different types to a {@link GenericTable}.
	 */
	private void addTypedColumns(final GenericTable imageJTable,
		final omero.grid.Data data, final int rowCount)
	{
		for (int c = 0; c < data.columns.length; c++) {
			final Column<?> col = TableUtils.createImageJColumn(data.columns[c]);
			col.setSize(rowCount);
			imageJTable.add(col);
		}
	}
}
