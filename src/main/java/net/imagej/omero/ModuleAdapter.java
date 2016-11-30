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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
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
	 * A name used when there is a single image input or output.
	 * Parameters with this name are handled specially by OMERO clients such as
	 * OMERO.web and OMERO.insight.
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

	// -- Constructor --

	public ModuleAdapter(final Context context, final ModuleInfo info,
		final omero.client client)
	{
		setContext(context);
		this.info = info;
		this.client = client;
		imageInput = getSingleImage(info.inputs());
		imageOutput = getSingleImage(info.outputs());
	}

	// -- ModuleAdapter methods --

	/** Parses script parameters for the associated ImageJ module. */
	public void params() throws omero.ServerError {
		// Parsing. See OmeroPy/src/omero/scripts.py
		// for the Python implementation.
		// =========================================
		client.setOutput("omero.scripts.parse", getJobInfo());
	}

	/** Executes the associated ImageJ module as an OMERO script. */
	public void launch() throws omero.ServerError, IOException {
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

}
