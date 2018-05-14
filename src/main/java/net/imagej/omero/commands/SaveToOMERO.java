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

import io.scif.MetadataService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import net.imagej.Dataset;
import net.imagej.omero.OMEROCommand;
import net.imagej.omero.OMEROLocation;
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
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;

/**
 * Exports Objects to OMERO.
 *
 * @author Alison Walter
 */
@Plugin(type = Command.class, label = "Export to OMERO", menu = { @Menu(
	label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
	mnemonic = MenuConstants.FILE_MNEMONIC), @Menu(label = "Export", weight = 6),
	@Menu(label = "To OMERO ... ", weight = 100, mnemonic = 'o') })
public class SaveToOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private ROIService roiService;

	@Parameter
	private TableService tableService;

	@Parameter
	private MetadataService metadataService;

	@Parameter(label = "OMERO Dataset ID")
	private long datasetID = -1;

	@Parameter(type = ItemIO.BOTH)
	private Dataset dataset;

	@Parameter(label = "Upload new image?")
	private boolean uploadImage;

	@Parameter(label = "Upload tables?")
	private boolean uploadTables;

	@Parameter(label = "Upload ROIs?")
	private boolean uploadROIs;

	@Parameter(label = "Update ROIs?", description = "If the ROIs already exist" +
		" in OMERO, they'll be updated. If not, new ones will be created")
	private boolean updateROIs;

	@Parameter(label = "Table names")
	private String tableNames = "";

	@Override
	public void run() {
		try {
			final long imageID = getImageID();
			if (!uploadImage && getImageID() < 0) {
				log.error("Given image did not originate from OMERO! Cannot update " +
					"image which doesn't exist in OMERO");
				return;
			}
			if (uploadImage && updateROIs) {
				log.error("Cannot update ROIs on a newly uploaded image!");
				return;
			}

			final OMEROLocation credentials = new OMEROLocation(getServer(),
				getPort(), getUser(), getPassword());

			final ROITree ROIsToUpload = uploadROIs || updateROIs ? roiService
				.getROIs(dataset) : null;
			final List<Table<?, ?>> tablesToUpload = uploadTables ? tableService
				.getTables(dataset) : null;
			final String[] names = uploadTables && tablesToUpload != null
				? getTableNames(tablesToUpload.size()) : null;

			if (uploadImage) omeroService.uploadImage(credentials, dataset,
				uploadROIs, ROIsToUpload, updateROIs, uploadTables, tablesToUpload,
				names, datasetID);
			else omeroService.uploadImageAttachments(credentials, imageID, uploadROIs,
				updateROIs, uploadTables, ROIsToUpload, tablesToUpload, names);
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
		catch (final DSOutOfServiceException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final ExecutionException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final DSAccessException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final URISyntaxException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error creating URI for Session: " + exc.getMessage());
		}
		catch (final IOException exc) {
			log.error(exc);
		}
	}

	// -- Helper methods --

	private String[] getTableNames(final int numTables) {
		final String[] names = new String[numTables];
		final String[] parsedNames = tableNames.split(",[ ]*");
		for (int i = 0; i < names.length; i++) {
			if (tableNames == null || parsedNames.length == 0) names[i] = "table";
			else if (parsedNames.length < i) names[i] =
				parsedNames[parsedNames.length - 1];
			else names[i] = parsedNames[i];
		}
		return names;
	}

	private long getImageID() {
		final String source = dataset.getSource();
		if (source == null || source.isEmpty() || !source.contains("omero"))
			return -1;

		// Parse source String
		final String clean = source.replaceFirst("^omero:", "").replaceFirst(
			"\\.omero$", "");
		final Map<String, Object> map = metadataService.parse(clean, "&");
		final Object id = map.get("imageID");

		try {
			return Long.valueOf(id.toString());
		}
		catch (final NumberFormatException exc) {
			return -1;
		}
	}

}
