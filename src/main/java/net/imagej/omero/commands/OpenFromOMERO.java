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

import java.io.IOException;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.omero.OMEROCommand;
import net.imagej.omero.OMEROService;
import net.imagej.roi.ROIService;
import net.imagej.roi.ROITree;
import net.imagej.table.Table;
import net.imagej.table.TableService;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;

/**
 * @author Alison Walter
 */
@Plugin(type = Command.class, label = "Import From OMERO", menu = { @Menu(
	label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
	mnemonic = MenuConstants.FILE_MNEMONIC), @Menu(label = "Import", weight = 6),
	@Menu(label = "From OMERO ...", weight = 100, mnemonic = 'o') })
public class OpenFromOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private long imageID;

	@Parameter
	private boolean downloadTables;

	@Parameter
	private boolean downloadRois;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset image;

	@Parameter(type = ItemIO.OUTPUT, required = false)
	private List<Table<?, ?>> tables;

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		try {
			final omero.client c = new omero.client(getServer(), getPort());
			c.createSession(getUser(), getPassword());
			image = omeroService.downloadImage(c, imageID);

			// Load ROIs by requesting the children
			// Do NOT set ROIs as an output, because they're already attached to the
			// image. Declaring them as output will cause duplicates to be attached.
			if (downloadRois) ((ROITree) image.getProperties().get(
				ROIService.ROI_PROPERTY)).children();

			if (downloadTables) tables = (List<Table<?, ?>>) image.getProperties()
				.get(TableService.TABLE_PROPERTY);
		}
		catch (final ServerError exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final PermissionDeniedException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final CannotCreateSessionException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final IOException exc) {
			log.error(exc);
		}
	}

}
