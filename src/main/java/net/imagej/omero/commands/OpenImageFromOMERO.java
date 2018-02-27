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

package net.imagej.omero.commands;

import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.services.DatasetIOService;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.omero.OMEROCommand;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/** An ImageJ command for retrieving images from an OMERO server on demand. */
@Plugin(type = Command.class, label = "Import from OMERO", menu = {
	@Menu(label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
		mnemonic = MenuConstants.FILE_MNEMONIC),
	@Menu(label = "Import", weight = 5),
	@Menu(label = "OMERO...", weight = 100, mnemonic = 'o') })
public class OpenImageFromOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private int imageID;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset dataset;

	@Override
	public void run() {
		final String omeroSource =
			"server=" + getServer() + //
			"&port=" + getPort() + //
			"&user=" + getUser() + //
			"&password=" + getPassword() + //
			"&imageID=" + imageID + //
			".omero";

		// Force on-demand loading of planes, instead of loading them all up front.
		final SCIFIOConfig config = new SCIFIOConfig();
		config.imgOpenerSetImgModes(ImgMode.CELL);

		try {
			dataset = datasetIOService.open(omeroSource, config);
		}
		catch (IOException exc) {
			log.error(exc);
		}
	}

}
