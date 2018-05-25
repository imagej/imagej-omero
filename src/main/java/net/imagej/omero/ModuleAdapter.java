/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2018 Open Microscopy Environment:
 * 	- Board of Regents of the University of Wisconsin-Madison
 * 	- Glencoe Software, Inc.
 * 	- University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package net.imagej.omero;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.ItemVisibility;
import org.scijava.Versioned;
import org.scijava.command.Command;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleRunner;
import org.scijava.module.ModuleService;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.RLong;
import omero.ServerError;
import omero.api.IQueryPrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;
import omero.gateway.model.TableData;
import omero.log.Logger;
import omero.log.SimpleLogger;
import omero.model.DatasetImageLink;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.Image;
import omero.model.ImageI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.Roi;
import omero.model.Shape;

/**
 * Adapts an ImageJ {@link Module} (such as a {@link Command}) to be usable as
 * an OMERO script, converting information between ImageJ- and OMERO-compatible
 * formats as appropriate.
 *
 * @author Curtis Rueden
 */
public class ModuleAdapter extends AbstractContextual {

	/**
	 * A name used when there is a single image input or output. Parameters with
	 * this name are handled specially by OMERO clients such as OMERO.web and
	 * OMERO.insight.
	 */
	private static final String IMAGE_NAME = "Image_ID";

	private static final String ATTACH_DATATSET = "attachToOMERODatasetIDs";

	private static final String ATTACH_IMAGE = "attachToImages";

	// -- Parameters --

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private PluginService pluginService;

	// -- Fields --

	/** The {@link ModuleInfo} associated with this adapter. */
	private final ModuleInfo info;

	/** The OMERO client to use when communicating about a job. */
	private final omero.client client;

	/**
	 * The single image input parameter for the module, or null if there are no
	 * input image parameters or more than one.
	 */
	private final ModuleItem<?> imageInput;

	/**
	 * The single image output parameter for the module, or null if there are no
	 * output image parameters or more than one.
	 */
	private final ModuleItem<?> imageOutput;

	/**
	 * The OMERO gateway which can be used to connect users to OMERO, or create
	 * various facilities for searching OMERO Datasets, Projects, etc.
	 */
	private final Gateway gateway;

	// -- Constructor --

	public ModuleAdapter(final Context context, final ModuleInfo info,
		final omero.client client)
	{
		setContext(context);
		this.info = info;
		this.client = client;
		imageInput = getSingleImage(info.inputs());
		imageOutput = getSingleImage(info.outputs());

		// Create gateway
		final Logger simpleLogger = new SimpleLogger();
		this.gateway = new Gateway(simpleLogger);
	}

	// -- ModuleAdapter methods --

	/** Parses script parameters for the associated ImageJ module. */
	public void params() throws omero.ServerError {
		// Parsing. See OmeroPy/src/omero/scripts.py
		// for the Python implementation.
		// =========================================
		client.setOutput("omero.scripts.parse", getJobInfo());
	}

	/**
	 * Executes the associated ImageJ module as an OMERO script.
	 * 
	 * @throws IOException
	 * @throws ServerError
	 * @throws ExecutionException
	 * @throws CannotCreateSessionException
	 * @throws PermissionDeniedException
	 * @throws DSAccessException
	 * @throws DSOutOfServiceException
	 * @throws URISyntaxException 
	 * @throws NumberFormatException 
	 */
	public void launch() throws ServerError, IOException, ExecutionException,
		PermissionDeniedException, CannotCreateSessionException,
		DSOutOfServiceException, DSAccessException, NumberFormatException,
		URISyntaxException
	{
		// populate inputs
		log.debug(info.getTitle() + ": populating inputs");
		final HashMap<String, Object> inputMap = new HashMap<>();
		final HashMap<String, Long> inputImages = new HashMap<>();
		for (final String name : client.getInputKeys()) {
			final ModuleItem<?> input = getInput(name);
			final Class<?> type = input.getType();
			final Object value = omeroService.toImageJ(client, client.getInput(name),
				type);
			inputMap.put(input.getName(), value);
			if (isImageType(value.getClass())) inputImages.put(input.getName(),
					((RLong) client.getInput(name)).getValue());
		}

		// execute ImageJ module
		log.debug(info.getTitle() + ": executing module");
		final Future<Module> future = moduleService.run(info, true, inputMap);
		final Module module = moduleService.waitFor(future);

		final Map<ModuleItem<?>, Object> outputAttach = new HashMap<>();
		final Map<String, Long> outputImages = new HashMap<>();

		// populate outputs, except tables and ROIs
		log.debug(info.getTitle() + ": populating outputs");
		for (final ModuleItem<?> item : module.getInfo().outputs()) {
			final Object value = omeroService.toOMERO(client, item.getValue(module));
			final String name = getOutputName(item);
			if (value == null) {
				log.warn(info.getTitle() + ": output '" + name + "' is null");
				continue;
			}
			if (value instanceof omero.RType) client.setOutput(name,
				(omero.RType) value);

			// store output images
			if (isImageType(item.getValue(module).getClass())) {
				outputAttach.put(item, value);
				outputImages.put(item.getName(), ((RLong) value).getValue());
			}

			// store output tables
			if (value instanceof TableData) outputAttach.put(item, value);

			// store output rois
			if (value instanceof ROIData) outputAttach.put(item, value);
			if (value instanceof Collection && ((Collection<?>) value).iterator()
				.next() instanceof ROIData) outputAttach.put(item, value);

			// Resolve outputs
			// NB: ModuleAdpater is only run when running script from OMERO, resolving
			// the outputs prevents the ROI post processors from trying to display
			// anything
			module.resolveOutput(item.getName());
		}

		// Create output links, and upload ROIs/Tables
		createOutputLinks(inputImages, outputImages, outputAttach);
		gateway.disconnect();

		log.debug(info.getTitle() + ": completed execution");
	}

	/** Converts ImageJ module metadata to OMERO job metadata. */
	public omero.RType getJobInfo() {
		// populate module metadata
		final omero.grid.JobParams params = new omero.grid.JobParams();
		params.name = "[ImageJ] " + info.getTitle(); // info.getName();
		params.version = getVersion();
		params.description = info.getDescription();
		params.stdoutFormat = "text/plain";
		params.stderrFormat = "text/plain";

		// Instantiate and preprocess the module
		final Module m = moduleService.createModule(info);
		final List<PreprocessorPlugin> pre = pluginService.createInstancesOfType(
			PreprocessorPlugin.class);
		final ModuleRunner mr = new ModuleRunner(getContext(), m, pre, null);
		mr.preProcess();

		// count module inputs and outputs
		final int inputCount = m.getInputs().size();
		final int outputCount = m.getOutputs().size();
		final int inputDigits = String.valueOf(inputCount).length();
		final int outputDigits = String.valueOf(outputCount).length();

		// convert metadata for each module input
		params.inputs = new HashMap<>();
		int inputIndex = 0;
		for (final ModuleItem<?> item : info.inputs()) {
			if (item.getVisibility() == ItemVisibility.MESSAGE) continue;
			if (m.isInputResolved(item.getName())) continue;
			final omero.grid.Param param = omeroService.getJobParam(item);
			if (param != null) {
				param.grouping = pad(inputIndex++, inputDigits);
				params.inputs.put(getInputName(item), param);
			}
		}

		// convert metadata for each module output
		params.outputs = new HashMap<>();
		int outputIndex = 0;
		for (final ModuleItem<?> item : info.outputs()) {
			final omero.grid.Param param = omeroService.getJobParam(item);
			if (param != null) {
				param.grouping = pad(outputIndex++, outputDigits);
				params.outputs.put(getOutputName(item), param);
			}
		}

		return omero.rtypes.rinternal(params);
	}

	/**
	 * Gets the version of the associated ImageJ module.
	 *
	 * @return {@link Versioned#getVersion()}; or null if the module does not
	 *         implement the {@link Versioned} interface. Extracts the version of
	 *         the associated ImageJ module, by scanning the relevant JAR manifest
	 *         and/or POM.
	 */
	public String getVersion() {
		return info instanceof Versioned ? ((Versioned) info).getVersion() : null;
	}

	// -- Helper methods --

	/**
	 * Gets the module input with the given name.
	 * <p>
	 * Includes special case handling for renaming single image inputs to
	 * {@link #IMAGE_NAME} to make OMERO clients happy.
	 * </p>
	 */
	private ModuleItem<?> getInput(final String name) {
		if (name.equals(IMAGE_NAME) && imageInput != null) return imageInput;
		return info.getInput(name);
	}

	/**
	 * Gets the name of the given module input.
	 * <p>
	 * Includes special case handling for renaming single image inputs to
	 * {@link #IMAGE_NAME} to make OMERO clients happy.
	 * </p>
	 */
	private String getInputName(final ModuleItem<?> item) {
		return getName(item, imageInput);
	}

	/**
	 * Gets the name of the given module output.
	 * <p>
	 * Includes special case handling for renaming single image outputs to
	 * {@link #IMAGE_NAME} to make OMERO clients happy.
	 * </p>
	 */
	private String getOutputName(final ModuleItem<?> item) {
		return getName(item, imageOutput);
	}

	/** Helper method for {@link #getInputName} and {@link #getOutputName}. */
	private String getName(final ModuleItem<?> item,
		final ModuleItem<?> imageItem)
	{
		if (imageItem != null && item.getName().equals(imageItem.getName())) {
			return IMAGE_NAME;
		}
		return item.getName();
	}

	/**
	 * Scans the given list of parameters for a single item of image type (i.e., a
	 * {@link Dataset}, {@link DatasetView} or {@link ImageDisplay}). This is
	 * useful so that such items can be assigned a special name for the benefit of
	 * OMERO clients.
	 */
	private ModuleItem<?> getSingleImage(final Iterable<ModuleItem<?>> items) {
		ModuleItem<?> imageItem = null;
		for (final ModuleItem<?> item : items) {
			final Class<?> type = item.getType();
			if (isImageType(type))
			{
				if (imageItem == null) imageItem = item;
				else return null; // multiple image parameters
			}
		}
		return imageItem;
	}

	/**
	 * Gets a zero-padded string of the given number, with the specified number of
	 * digits.
	 */
	private String pad(final int num, final int outputDigits) {
		return String.format("%0" + outputDigits + "d", num);
	}

	/**
	 * Creates an ExperimenterData OMERO object which is connected to the gateway.
	 */
	private ExperimenterData createUser() {
		String host = client.getProperty("omero.host");
		if (host.equals("")) {
			String router = client.getProperty("omero.ClientCallback.Router");
			String[] comp = router.split("\\s+");
			for (int i = 0; i < comp.length; i++) {
				if (comp[i].equals("-h")) {
					host = comp[i + 1];
					break;
				}
			}
		}
		final LoginCredentials cred = new LoginCredentials(client.getSessionId(),
			null, host, Integer.parseInt(client.getProperty("omero.port")));

		try {
			return gateway.connect(cred);
		}
		catch (final DSOutOfServiceException err) {
			log.error(err.getMessage());
		}

		return null;
	}

	/**
	 * Attempts to attach the outputs to the appropriate items.
	 */
	@SuppressWarnings("unchecked")
	private void createOutputLinks(final Map<String, Long> inputImages,
		final Map<String, Long> outputImages,
		final Map<ModuleItem<?>, Object> outputs) throws ExecutionException,
		ServerError, DSOutOfServiceException, DSAccessException
	{
		final ExperimenterData user = createUser();
		final BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
		final DataManagerFacility dm = gateway.getFacility(
			DataManagerFacility.class);
		final SecurityContext ctx = new SecurityContext(user.getGroupId());
		final IQueryPrx query = gateway.getQueryService(ctx);

		for (ModuleItem<?> item : outputs.keySet()) {
			final Object o = outputs.get(item);
			if (o instanceof RLong) attachImageToDataset((RLong) o, item, inputImages,
				browse, query, dm, ctx);
			else if (o instanceof TableData) attachTableToImages(item, (TableData) o,
				inputImages, outputImages, dm, ctx);
			else if (o instanceof ROIData) attachRoiToImages(item, (ROIData) o,
				inputImages, outputImages, ctx);
			else if (o instanceof Collection) attachRoisToImages(item,
				(Collection<ROIData>) o, inputImages, outputImages, ctx);
			else throw new IllegalArgumentException("Unsupported type: " + o
				.getClass());
		}
	}

	/**
	 * Attaches the image represented by the given ID to the appropriate OMERO
	 * Datasets. If the given image had a specified "attach" field in the script,
	 * the image will be attached to the datasets specified there. Otherwise, it
	 * is attached to the datasets of the input images. If there are no input
	 * images the image will be orphaned.
	 */
	private void attachImageToDataset(final RLong imageID,
		final ModuleItem<?> item, final Map<String, Long> inputImages,
		final BrowseFacility browse, final IQueryPrx query,
		final DataManagerFacility dm, final SecurityContext ctx)
		throws DSOutOfServiceException, DSAccessException, ServerError
	{
		Collection<DatasetData> datasets = null;

		// Check if any dataset IDs were specified in the attach modifier
		if (item.get(ATTACH_DATATSET) != null && !item.get(ATTACH_DATATSET)
			.isEmpty())
		{
			final String[] idStrings = item.get(ATTACH_DATATSET).split(",[ ]*");
			final List<Long> ids = new ArrayList<>();
			for (int i = 0; i < idStrings.length; i++) {
				try {
					final long id = Long.parseLong(idStrings[i]);
					ids.add(id);
				}
				catch (NumberFormatException exc) {
					log.error(idStrings[i] + " is not a valid number", exc);
				}
			}
			datasets = browse.getDatasets(ctx, ids);
		}

		// Load all Datasets for all input images
		else {
			final Map<Long, DatasetData> datasetMap = new HashMap<>();
			for (final Long id : inputImages.values()) {
				// Get the image from OMERO with loaded datasets
				final Image img = (Image) query.findByQuery(
					"select i from Image as i " +
						"left outer join fetch i.datasetLinks link " +
						"join fetch link.parent " + "where i.id = " + id, null);
				final Collection<DatasetImageLink> links = img.copyDatasetLinks();
				for (final DatasetImageLink link : links)
					datasetMap.put(link.getParent().getId().getValue(), new DatasetData(
						link.getParent()));
			}
			datasets = datasetMap.values();
		}

		if (datasets == null || datasets.isEmpty()) {
			log.warn("No datasets found to attach image to, image will be orphaned");
			return;
		}

		// Attach image to each dataset in collection
		for (final DatasetData dataset : datasets)
			dm.addImageToDataset(ctx, new ImageData(new ImageI(imageID, false)),
				dataset);
	}

	/**
	 * Attaches an output table to the appropriate OMERO image(s). If no image can
	 * be found to attach the table to, the table is not uploaded and an error is
	 * logged.
	 */
	private void attachTableToImages(final ModuleItem<?> item,
		final TableData table, final Map<String, Long> inputImages,
		final Map<String, Long> outputImages, final DataManagerFacility dm,
		final SecurityContext ctx) throws DSOutOfServiceException,
		DSAccessException, ExecutionException, ServerError
	{
		final List<ImageData> attachToImages = getImagesToAttachTo(item,
			inputImages, outputImages);

		if (attachToImages.isEmpty()) {
			log.error("No images to attach table to. Tables must be attached " +
				"to something! " + item.getName() + " will not be uploaded!");
			return;
		}

		final TablesFacility tablesFacility = gateway.getFacility(
			TablesFacility.class);

		final TableData uploaded = tablesFacility.addTable(ctx, attachToImages.get(
			0), item.getName(), table);

		// Adding tables again would create new tables on the server
		for (int i = 1; i < attachToImages.size(); i++) {
			final OriginalFile file = new OriginalFileI(uploaded.getOriginalFileId(),
				false);
			final FileAnnotation anno = new FileAnnotationI();
			anno.setFile(file);
			FileAnnotationData annotation = new FileAnnotationData(anno);
			annotation.setDescription(item.getName());

			annotation = (FileAnnotationData) dm.saveAndReturnObject(ctx, annotation);
			dm.attachAnnotation(ctx, annotation, attachToImages.get(i));
		}

		// Set output in OMERO client
		client.setOutput(getOutputName(item), omero.rtypes.rlong(uploaded
			.getOriginalFileId()));
	}

	/**
	 * Attaches the given ROI to an OMERO image. If no images are specified the
	 * ROI is simply uploaded with no image.
	 * <p>
	 * Additionally, if the ROI needs to be attached to multiple images then
	 * multiple ROIs are created and uploaded to OMERO. In this case, only the ID
	 * of the last uploaded ROI is set in the output
	 * </p>
	 */
	private void attachRoiToImages(final ModuleItem<?> item, final ROIData roi,
		final Map<String, Long> inputImages, final Map<String, Long> outputImages,
		final SecurityContext ctx) throws ExecutionException,
		DSOutOfServiceException, DSAccessException, ServerError
	{
		final ROIData uploaded = uploadROIs(item, Collections.singletonList(roi),
			inputImages, outputImages, ctx).iterator().next();

		// Add output ID to client
		// If multiple ROIs needed to be saved only the last ones ID is set
		client.setOutput(getOutputName(item), omero.rtypes.rlong(uploaded.getId()));
	}

	/**
	 * Attaches the given ROIs to an OMERO image. If no images are specified the
	 * ROIsare simply uploaded with no image.
	 * <p>
	 * Additionally, if the ROIs need to be attached to multiple images then
	 * multiple collections of ROIs are created and uploaded to OMERO. In this
	 * case, only the IDs of the last uploaded ROI collection is set in the output
	 * </p>
	 */
	private void attachRoisToImages(final ModuleItem<?> item,
		final Collection<ROIData> rois, final Map<String, Long> inputImages,
		final Map<String, Long> outputImages, final SecurityContext ctx)
		throws ExecutionException, DSOutOfServiceException, DSAccessException,
		ServerError
	{
		final Collection<ROIData> uploaded = uploadROIs(item, rois, inputImages,
			outputImages, ctx);

		final RLong[] ids = uploaded.stream().map(x -> omero.rtypes.rlong(x
			.getId())).toArray(RLong[]::new);
		// Add output ID to client
		// If multiple ROI collections needed to be saved only the ROI IDs of the
		// last uploaded collection are set
		client.setOutput(getOutputName(item), omero.rtypes.rlist(ids));
	}

	/**
	 * Uploads and returns the given ROIs to OMERO.
	 */
	private Collection<ROIData> uploadROIs(final ModuleItem<?> item,
		final Collection<ROIData> rois, final Map<String, Long> inputImages,
		final Map<String, Long> outputImages, final SecurityContext ctx)
		throws ExecutionException, DSOutOfServiceException, DSAccessException
	{
		final List<ImageData> attachToImages = getImagesToAttachTo(item,
			inputImages, outputImages);
		final ROIFacility roiFacility = gateway.getFacility(ROIFacility.class);
		Collection<ROIData> uploaded = null;

		// Set name
		for (final ROIData roi : rois) {
			if (((Roi) roi.asIObject()).getName() == null) ((Roi) roi.asIObject())
				.setName(omero.rtypes.rstring(item.getName()));
		}

		// ROIs without images are allowed
		if (attachToImages.isEmpty()) {
			log.warn("No images to attach ROIs to. " +
				"ROIs will be uploaded WITHOUT being attached to anything!");
			uploaded = roiFacility.saveROIs(ctx, -1, rois);
		}

		// Upload ROIs and attach to image
		// NB: In OMERO an Image can have multiple ROIs but a single ROI cannot
		// reference multiple Images.
		else {
			if (attachToImages.size() > 1) log.warn("ROIs can only have one image, " +
				"multiple ROIs will be uploaded!");
			// Attach the first ROI, do this outside the loop in case we're updating
			// rois
			uploaded = roiFacility.saveROIs(ctx, attachToImages.get(0).getId(), rois);
			for (int i = 1; i < attachToImages.size(); i++) {
				// Clear before uploading to ensure IDs preserved for setting outputs
				// in omero client object
				clearROIs(rois);
				uploaded = roiFacility.saveROIs(ctx, attachToImages.get(i).getId(),
					rois);
			}
		}

		return uploaded;
	}

	/**
	 * Clears out the ROIs and associated shapes IDs, such that they will be
	 * uploaded to OMERO as new objects.
	 */
	private void clearROIs(final Collection<ROIData> rois) {
		for (final ROIData roi : rois) {
			final Roi roiI = (Roi) roi.asIObject();
			roiI.setId(null);
			roiI.setImage(null);

			final Iterator<List<ShapeData>> itr = roi.getIterator();
			while (itr.hasNext()) {
				final List<ShapeData> shapes = itr.next();
				for (final ShapeData shape : shapes)
					((Shape) shape.asIObject()).setId(null);
			}
		}
	}

	/**
	 * Collects and returns the {@link ImageData} objects the given
	 * {@link ModuleItem} should be attached to. If the given output
	 * {@link ModuleItem} has an "attach" property, the images in that property
	 * are returned. If not, the input images are returned.
	 */
	private List<ImageData> getImagesToAttachTo(final ModuleItem<?> item,
		final Map<String, Long> inputImages, final Map<String, Long> outputImages)
	{
		final List<ImageData> images = new ArrayList<>();

		// Were the images to attach to specified?
		if (item.get(ATTACH_IMAGE) != null && !item.get(ATTACH_IMAGE).isEmpty()) {
			final String[] attachImages = item.get(ATTACH_IMAGE).split(",[ ]*");
			for (int i = 0; i < attachImages.length; i++) {

				// Does the specified attachment correspond to an input image?
				if (inputImages.containsKey(attachImages[i])) images.add(new ImageData(
					new ImageI(inputImages.get(attachImages[i]), false)));

				// an output image?
				else if (outputImages.containsKey(attachImages[i])) images.add(
					new ImageData(new ImageI(outputImages.get(attachImages[i]), false)));

				else log.warn(attachImages[i] +
					" does not correspond to a valid input image");
			}
			return images;
		}

		for (final Long imageID : inputImages.values())
			images.add(new ImageData(new ImageI(imageID, false)));
		return images;
	}

	@SuppressWarnings("deprecation")
	private boolean isImageType(final Class<?> type) {
		final ConvertService convert = omeroService.getContext().getService(
			ConvertService.class);
		return Dataset.class.isAssignableFrom(type) || DatasetView.class
			.isAssignableFrom(type) || ImageDisplay.class.isAssignableFrom(type) ||
			convert.supports(type, Dataset.class);
	}

}
