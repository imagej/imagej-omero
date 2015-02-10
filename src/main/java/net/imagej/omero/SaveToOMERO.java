/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2015 Board of Regents of the University of
 * Wisconsin-Madison and University of Dundee.
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

import io.scif.services.DatasetIOService;

import java.io.IOException;

import net.imagej.Dataset;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/** An ImageJ command for uploading images to an OMERO server. */
@Plugin(type = Command.class, label = "Export to OMERO", menu = {
	@Menu(label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
		mnemonic = MenuConstants.FILE_MNEMONIC),
	@Menu(label = "Export", weight = 6),
	@Menu(label = "OMERO... ", weight = 100, mnemonic = 'o') })
public class SaveToOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private Dataset dataset;

	@Override
	public void run() {
		final String omeroDestination =
			"name=" + dataset.getName() + //
			"&server=" + getServer() + //
			"&port=" + getPort() + //
			"&user=" + getUser() + //
			"&password=" + getPassword() + //
			".omero";

		try {
			datasetIOService.save(dataset, omeroDestination);
		}
		catch (IOException exc) {
			log.error(exc);
		}
	}

}
