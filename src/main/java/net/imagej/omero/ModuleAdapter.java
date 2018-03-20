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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import omero.gateway.model.TableData;
import omero.log.Logger;
import omero.log.SimpleLogger;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.Roi;

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
	 */
	@SuppressWarnings("unchecked")
	public void launch() throws ServerError, IOException, ExecutionException,
		PermissionDeniedException, CannotCreateSessionException,
		DSOutOfServiceException, DSAccessException
	{
		// populate inputs
		log.debug(info.getTitle() + ": populating inputs");
		final HashMap<String, Object> inputMap = new HashMap<>();
		for (final String name : client.getInputKeys()) {
			final ModuleItem<?> input = getInput(name);
			final Class<?> type = input.getType();
			final Object value = omeroService.toImageJ(client, client.getInput(name),
				type);
			inputMap.put(input.getName(), value);
		}

		// execute ImageJ module
		log.debug(info.getTitle() + ": executing module");
		final Future<Module> future = moduleService.run(info, true, inputMap);
		final Module module = moduleService.waitFor(future);

		final HashMap<String, TableData> tables = new HashMap<>();
		final HashMap<String, List<ROIData>> rois = new HashMap<>();

		// populate outputs, except tables and ROIs
		log.debug(info.getTitle() + ": populating outputs");
		for (final ModuleItem<?> item : module.getInfo().outputs()) {
			final Object value = omeroService.toOMERO(client, item.getValue(module));
			final String name = getOutputName(item);
			if (value == null) {
				log.warn(info.getTitle() + ": output '" + name + "' is null");
			}
			if (value instanceof omero.RType) client.setOutput(name,
				(omero.RType) value);
			if (value instanceof TableData) tables.put(name, (TableData) value);
			if (value instanceof ROIData) rois.put(name, Collections.singletonList(
				(ROIData) value));
			if (value instanceof List && ((List<?>) value).iterator()
				.next() instanceof ROIData) rois.put(name, (List<ROIData>) value);
		}

		createOutputLinks(inputMap, tables, rois);
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
			if (Dataset.class.isAssignableFrom(type) || DatasetView.class
				.isAssignableFrom(type) || ImageDisplay.class.isAssignableFrom(type))
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
	 * Loops over all the orphaned images, and grabs the ones which have IDs that
	 * match the omero client outputs.
	 */
	private List<ImageData> getOutputImages(final long userId,
		final BrowseFacility browse, final SecurityContext ctx)
		throws ServerError
	{
		final Collection<ImageData> orphans = browse.getOrphanedImages(ctx, userId);

		final HashMap<Long, ImageData> mappedOrphans = new HashMap<>();
		for (ImageData i : orphans)
			mappedOrphans.put(i.getId(), i);

		final List<ImageData> images = new ArrayList<>();

		for (String key : client.getOutputKeys()) {
			final omero.RType type = client.getOutput(key);
			if (type instanceof RLong) {
				if (mappedOrphans.containsKey(((RLong) type).getValue())) images.add(
					mappedOrphans.get(((RLong) type).getValue()));
			}
		}
		return images;
	}

	/**
	 * Loops over the input items, and checks for Datasets, and if so gets the
	 * corresponding omero ImageData.
	 */
	private List<ImageData> getInputImages(final HashMap<String, Object> inputMap,
		final BrowseFacility browse, final SecurityContext ctx) throws ServerError,
		DSOutOfServiceException, DSAccessException
	{
		if (imageInput != null) {
			final RLong id = (RLong) client.getInput(IMAGE_NAME);
			return Collections.singletonList(browse.getImage(ctx, id.getValue()));
		}
		final List<ImageData> images = new ArrayList<>();
		for (String key : inputMap.keySet()) {
			if (inputMap.get(key) instanceof Dataset) {
				final RLong id = (RLong) client.getInput(key);
				images.add(browse.getImage(ctx, id.getValue()));
			}
		}

		return images;
	}

	/**
	 * Attaches the given output images to the datasets of the given input images.
	 */
	private void attachImagesToDatasets(final List<ImageData> inputImages,
		final List<ImageData> outputImages, final DataManagerFacility dm, final BrowseFacility browse,
		SecurityContext ctx) throws DSOutOfServiceException, DSAccessException
	{
		final HashMap<Long, DatasetData> datasets = new HashMap<>();

		// Get all datasets related to the input images
		// FIXME: ImageData has a getDatasets() method, but it is null since the
		// underlying ImageI is unloaded.
		final Collection<DatasetData> allDatasets = browse.getDatasets(ctx);
		for (DatasetData d : allDatasets) {
			Collection<ImageData> allImages = browse.getImagesForDatasets(ctx,
				Collections.singleton(d.getId()));
			for (ImageData image : allImages) {
				for (ImageData input : inputImages) {
					if (input.getId() == image.getId()) datasets.put(d.getId(), d);
				}
			}
		}

		// attach all output images to these datasets
		if (!datasets.isEmpty()) {
			for (Long id : datasets.keySet())
				dm.addImagesToDataset(ctx, outputImages, datasets.get(id));
		}
	}

	/**
	 * Attaches the tables to the input images. This also adds the Table ids to
	 * the omero client.
	 */
	private void attachTablesToImages(final List<ImageData> images,
		final HashMap<String, TableData> tables, final SecurityContext ctx,
		final DataManagerFacility dm) throws DSOutOfServiceException,
		DSAccessException, ExecutionException, ServerError
	{
		final TablesFacility tablesFacility = gateway.getFacility(
			TablesFacility.class);

		for (final String name : tables.keySet()) {
			final TableData t = tablesFacility.addTable(ctx, images.get(0), name,
				tables.get(name));
			client.setOutput(name, omero.rtypes.rlong(t.getOriginalFileId()));
		}

		// Adding tables again would create new tables on the server
		for (int i = 1; i < images.size(); i++) {
			for (final String name : tables.keySet()) {
				final OriginalFile file = new OriginalFileI(tables.get(name)
					.getOriginalFileId(), false);
				final FileAnnotation anno = new FileAnnotationI();
				anno.setFile(file);
				FileAnnotationData annotation = new FileAnnotationData(anno);
				annotation.setDescription(name);

				annotation = (FileAnnotationData) dm.saveAndReturnObject(ctx,
					annotation);
				dm.attachAnnotation(ctx, annotation, images.get(i));
			}
		}
	}

	/**
	 * Saves and attaches {@link ROIData} to images. It also sets output IDs in
	 * the omero client.
	 */
	private void attachROIsToImages(final List<ImageData> images,
		final HashMap<String, List<ROIData>> rois, final SecurityContext ctx)
		throws ExecutionException, DSOutOfServiceException, DSAccessException,
		ServerError
	{
		// Assign names to ROIData
		for (final String name : rois.keySet()) {
			for (final ROIData roi : rois.get(name)) {
				String roiName = "";
				final Roi roiI = (Roi) roi.asIObject();
				if (roiI.getName() == null) roiName = name;
				else roiName = roiI.getName().getValue() + " " + name;
				roiI.setName(omero.rtypes.rstring(roiName));
			}
		}

		final ROIFacility roiFacility = gateway.getFacility(ROIFacility.class);

		// Upload ROIs without image
		if (images.isEmpty()) {
			for (final String key : rois.keySet()) {
				final Collection<ROIData> savedRois = roiFacility.saveROIs(ctx, -1, rois
					.get(key));
				rois.put(key, new ArrayList<>(savedRois));
			}
		}

		// Upload ROIs and attach to images
		else {
			for (final ImageData image : images) {
				for (final String key : rois.keySet()) {
					final Collection<ROIData> savedRois = roiFacility.saveROIs(ctx, image
						.getId(), rois.get(key));
					rois.put(key, new ArrayList<>(savedRois));
				}
			}
		}

		// Add output IDs to client
		for (final String name : rois.keySet()) {
			final List<ROIData> savedRois = rois.get(name);
			if (rois.get(name).size() > 1) {
				final omero.RLong[] ids = new omero.RLong[savedRois.size()];
				for (int i = 0; i < ids.length; i++)
					ids[i] = omero.rtypes.rlong(savedRois.get(i).getId());
				client.setOutput(name, omero.rtypes.rlist(ids));
			}
			else {
				client.setOutput(name, omero.rtypes.rlong(savedRois.get(0).getId()));
			}
		}
	}

	/**
	 * Attempts to attach the outputs to the appropriate items.
	 */
	private void createOutputLinks(final HashMap<String, Object> inputMap,
		final HashMap<String, TableData> tables,
		final HashMap<String, List<ROIData>> rois) throws ExecutionException,
		ServerError, DSOutOfServiceException, DSAccessException
	{
		final ExperimenterData user = createUser();
		final BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
		final DataManagerFacility dm = gateway.getFacility(
			DataManagerFacility.class);
		final SecurityContext ctx = new SecurityContext(user.getGroupId());
		final List<ImageData> outImages = getOutputImages(user.getId(), browse,
			ctx);
		final List<ImageData> inputImages = getInputImages(inputMap, browse, ctx);

		if (!outImages.isEmpty()) {
			attachImagesToDatasets(inputImages, outImages, dm, browse, ctx);
		}
		if (!tables.isEmpty()) {
			if (inputImages.isEmpty()) throw new IllegalArgumentException(
				"Input image(s) required to upload table to OMERO");
			attachTablesToImages(inputImages, tables, ctx, dm);
		}
		if (!rois.isEmpty()) {
			if (inputImages.isEmpty()) log.warn(
				"Uploaded ROIs not attached to any image");
			attachROIsToImages(inputImages, rois, ctx);
		}
	}

}
