/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2016 Open Microscopy Environment:
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

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import omero.RLong;
import omero.ServerError;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.log.Logger;
import omero.log.SimpleLogger;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.ImageAnnotationLink;
import omero.model.ImageAnnotationLinkI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.ItemVisibility;
import org.scijava.Versioned;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;

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
	 * @throws IOException
	 * @throws ServerError
	 * @throws ExecutionException
	 * @throws CannotCreateSessionException
	 * @throws PermissionDeniedException
	 * @throws DSAccessException
	 * @throws DSOutOfServiceException
	 */
	public void launch() throws ServerError, IOException, ExecutionException,
	PermissionDeniedException, CannotCreateSessionException,
	DSOutOfServiceException, DSAccessException
	{
		// populate inputs
		log.debug(info.getTitle() + ": populating inputs");
		final HashMap<String, Object> inputMap = new HashMap<String, Object>();
		for (final String name : client.getInputKeys()) {
			final ModuleItem<?> input = getInput(name);
			final Class<?> type = input.getType();
			final Object value =
					omeroService.toImageJ(client, client.getInput(name), type);
			inputMap.put(input.getName(), value);
		}

		// execute ImageJ module
		log.debug(info.getTitle() + ": executing module");
		final Future<Module> future = moduleService.run(info, true, inputMap);
		final Module module = moduleService.waitFor(future);

		// populate outputs
		log.debug(info.getTitle() + ": populating outputs");
		for (final ModuleItem<?> item : module.getInfo().outputs()) {
			final omero.RType value =
					omeroService.toOMERO(client, item.getValue(module));
			final String name = getOutputName(item);
			if (value == null) {
				log.warn(info.getTitle() + ": output '" + name + "' is null");
			}
			else client.setOutput(name, value);
		}

		createLinkages();

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

		// TODO: Instantiate and preprocess the module, excluding resolved inputs.

		// count module inputs and outputs
		final int inputCount = count(info.inputs());
		final int outputCount = count(info.outputs());
		final int inputDigits = String.valueOf(inputCount).length();
		final int outputDigits = String.valueOf(outputCount).length();

		// convert metadata for each module input
		params.inputs = new HashMap<String, omero.grid.Param>();
		int inputIndex = 0;
		for (final ModuleItem<?> item : info.inputs()) {
			if (item.getVisibility() == ItemVisibility.MESSAGE) continue;
			final omero.grid.Param param = omeroService.getJobParam(item);
			if (param != null) {
				param.grouping = pad(inputIndex++, inputDigits);
				params.inputs.put(getInputName(item), param);
			}
		}

		// convert metadata for each module output
		params.outputs = new HashMap<String, omero.grid.Param>();
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
	private String
	getName(final ModuleItem<?> item, final ModuleItem<?> imageItem)
	{
		if (imageItem != null && item.getName().equals(imageItem.getName())) {
			return IMAGE_NAME;
		}
		return item.getName();
	}

	/** Counts the number of elements iterated by the given {@link Iterable}. */
	private int count(final Iterable<?> iterable) {
		if (iterable == null) return -1;
		int count = 0;
		final Iterator<?> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return count;
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
			if (Dataset.class.isAssignableFrom(type) ||
					DatasetView.class.isAssignableFrom(type) ||
					ImageDisplay.class.isAssignableFrom(type))
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
	 * Attaches outputs to parent items of inputs or inputs themselves in OMERO.
	 *
	 * @throws ServerError
	 * @throws ExecutionException
	 */
	private void createLinkages() throws ServerError, ExecutionException {
		// Create user
		final ExperimenterData user = createUser();

		if(user != null) {
			final SecurityContext ctx = new SecurityContext(user.getGroupId());
			final BrowseFacility browse = gateway.getFacility(BrowseFacility.class);

			// Get all input images from OMERO
			final ArrayList<ImageData> images =
					getInputImages(browse, ctx, new ArrayList<ImageData>());

			if(!images.isEmpty()) {
				final ArrayList<DatasetData> datasets = new ArrayList<DatasetData>();
			// Get OMERO datasets associated with inputs only if output contains an
			// image
				if(containsImage()) getDatasets(browse, ctx, user, images, datasets);

				// attach specified outputs to inputs
				attachOutputs(datasets, ctx, browse, images);
			}
		}
	}

	/**
	 * Creates an ExperimenterData OMERO object which is connected to the gateway.
	 */
	private ExperimenterData createUser() {
		final LoginCredentials cred = new LoginCredentials();
		cred.getServer().setHostname(client.getProperty("omero.host"));
		cred.getServer()
		.setPort(Integer.parseInt(client.getProperty("omero.port")));
		cred.getUser().setUsername(client.getProperty("omero.user"));
		cred.getUser().setPassword(client.getProperty("omero.pass"));

		try {
			return gateway.connect(cred);
		}
		catch (final DSOutOfServiceException err) {
			log.error("Invalid user credentials");
		}

		return null;
	}

	/**
	 * Loops over input imageIDs and retrieves the OMERO image items associated
	 * with those IDs.
	 *
	 * @throws ServerError
	 */
	private ArrayList<ImageData> getInputImages(final BrowseFacility browse,
		final SecurityContext ctx, final ArrayList<ImageData> images)
				throws ServerError
				{
		for (final String name : client.getInputKeys()) {
			final Object value = client.getInput(name);
			if (!(value instanceof RLong)) continue;
			final long imageID = ((RLong) value).getValue();
			try {
				images.add(browse.getImage(ctx, imageID));
			}
			catch(NoSuchElementException err) {
				log.error("ImageID: " + imageID + " is not valid");
				return new ArrayList<ImageData>();
			}
		}
		return images;
				}

	/**
	 * Retrieves all the datasets associated with this user, and then loops over
	 * these datasets and their respective images looking for datasets which
	 * contain the input images. Then it returns a list of these OMERO datasets.
	 */
	private ArrayList<DatasetData> getDatasets(final BrowseFacility browse,
		final SecurityContext ctx, final ExperimenterData user,
		final ArrayList<ImageData> images, final ArrayList<DatasetData> finalIDs)
		{
		final Collection<DatasetData> dataSetIDs =
				browse.getDatasets(ctx, user.getId());

		for (final DatasetData dataset : dataSetIDs) {
			@SuppressWarnings("unchecked")
			final Set<ImageData> temp = dataset.getImages();
			for (final ImageData img : temp) {
				for (final ImageData img2 : images) {
					if (img.getId() == img2.getId()) finalIDs.add(dataset);
				}
			}
		}
		return finalIDs;
		}

	/**
	 * Loops over outputs and attaches them to appropriate OMERO objects when
	 * appropriate.
	 *
	 * @throws ServerError
	 */
	private void attachOutputs(final ArrayList<DatasetData> datasets,
		final SecurityContext ctx, final BrowseFacility browse, final
		ArrayList<ImageData> images) throws ServerError
	{
		for (final String key : client.getOutputKeys()) {
			omero.RType output = client.getOutput(key);
			if (!(output instanceof RLong)) continue;
			final long id = ((RLong) output).getValue();
			if(key.equals("table")) {
				attachTableToImages(id, images, ctx);
			}
			else {
				if(!datasets.isEmpty()) attachImageToDataset(id, datasets, ctx, browse);
			}
		}
	}

	/**
	 * Attaches an image to OMERO dataset(s).
	 */
	private void attachImageToDataset(final long imageID,
		final Collection<DatasetData> datasets, final SecurityContext ctx,
		final BrowseFacility browse)
	{
		try {
			final DataManagerFacility dm =
					gateway.getFacility(DataManagerFacility.class);
			final ImageData image = browse.getImage(ctx, imageID);
			for (final DatasetData dset : datasets)
				dm.addImageToDataset(ctx, image, dset);
		}
		catch (final ExecutionException err) {
			log.error("Cannot create DataManagerFacility");
		}
		catch (final DSOutOfServiceException err) {
			log.error("Cannot attach image to dataset. ImageID: " + imageID +
				" Dataset(s): " + datasets.toString());
		}
		catch (final DSAccessException err) {
			log.error("Cannot attach image to dataset. ImageID: " + imageID +
				" Dataset(s): " + datasets.toString());
		}
	}

	/**
	 * Attaches a table to OMERO images(s).
	 */
	private void attachTableToImages(final long tableID, final
		ArrayList<ImageData> images, final SecurityContext ctx) {

		final OriginalFile tableFile = new OriginalFileI(tableID, false);
		DataManagerFacility dm;
		try {
			dm = gateway.getFacility(DataManagerFacility.class);

			FileAnnotation annotation = new FileAnnotationI();
			// TODO assign annotation to a table namespace
			annotation.setNs(omero.rtypes
				.rstring(omero.constants.namespaces.NSBULKANNOTATIONS.value));
			annotation.setFile(tableFile);
			annotation = (FileAnnotation) dm.saveAndReturnObject(ctx, annotation);

			for(ImageData i : images) {
				ImageAnnotationLink link = new ImageAnnotationLinkI();
				link.setChild(annotation);
				link.setParent(i.asImage());
				// Save linkage to database
				link = (ImageAnnotationLink) dm.saveAndReturnObject(ctx, link);
			}
		}
		catch (ExecutionException exc) {
			log.error("Cannot create DataManagerFacility");
		}
		catch (DSOutOfServiceException exc) {
			log.error("Cannot attach table to image. TableID: " + tableID +
				" Image(s): " + images.toString());
		}
		catch (DSAccessException exc) {
			log.error("Cannot attach table to image. TableID: " + tableID +
				" Image(s): " + images.toString());
		}
	}

	/** Checks if the output contains an image */
	private boolean containsImage() throws ServerError {
		for (final String key : client.getOutputKeys()) {
			omero.RType output = client.getOutput(key);
			if (output instanceof RLong && !key.equals("table")) return true;
		}
		return false;
	}
}
