/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2023 Open Microscopy Environment:
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

import java.util.List;
import java.util.Map;

import net.imagej.Dataset;
import net.imagej.omero.OMEROCommand;
import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMEROException;
import net.imagej.omero.OMEROServer;
import net.imagej.omero.OMEROService;
import net.imagej.omero.OMEROSession;
import net.imagej.roi.ROIService;
import net.imagej.roi.ROITree;
import net.imagej.table.TableService;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.Table;

/**
 * A command to import objects from an OMERO database.
 *
 * @author Alison Walter
 */
@Plugin(type = Command.class, label = "Import from OMERO", menu = { //
	@Menu(label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
		mnemonic = MenuConstants.FILE_MNEMONIC), //
	@Menu(label = "Import", weight = 6), //
	@Menu(label = "OMERO...", weight = 100, mnemonic = 'o') //
})
public class OpenFromOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter(label = "OMERO Image ID")
	private long imageID;

	@Parameter(label = "Download tables?")
	private boolean downloadTables;

	@Parameter(label = "Download ROIs?")
	private boolean downloadRois;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset image;

	@Parameter(type = ItemIO.OUTPUT, required = false)
	private ROITree rois;

	@Parameter(type = ItemIO.OUTPUT, required = false)
	private List<Table<?, ?>> tables;

	@Override
	public void run() {
		try {
			// Connect to OMERO.
			final OMEROServer server = new OMEROServer(getServer(), getPort());
			final OMEROCredentials credentials = //
				new OMEROCredentials(getUser(), getPassword());
			final OMEROSession session = omeroService.session(server, credentials);

			// Download the image.
			image = session.downloadImage(imageID);

			final Map<String, Object> props = image.getProperties();

			// Load ROIs by requesting the children.
			// Do NOT set ROIs as an output, because they're already attached to the
			// image. Declaring them as output will cause duplicates to be attached.
			if (downloadRois) {
				rois = (ROITree) image.getProperties().get(ROIService.ROI_PROPERTY);
				rois.children();
			}

			// Load tables.
			if (downloadTables) {
				@SuppressWarnings("unchecked")
				final List<Table<?, ?>> tableProperty = //
					(List<Table<?, ?>>) props.get(TableService.TABLE_PROPERTY);
				tables = tableProperty;
			}
		}
		catch (final OMEROException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
	}
}
