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

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import net.imagej.omero.OMEROCommand;
import net.imagej.omero.OMEROLocation;
import net.imagej.omero.OMEROService;
import net.imagej.table.Table;

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

/** An ImageJ command for downloading a results table from an OMERO server. */
@Plugin(type = Command.class, label = "Import Table from OMERO", menu = { @Menu(
	label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
	mnemonic = MenuConstants.FILE_MNEMONIC), @Menu(label = "Import", weight = 6),
	@Menu(label = "OMERO Table...", weight = 100, mnemonic = 'o') })
public class OpenTableFromOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private long tableID;

	@Parameter(type = ItemIO.OUTPUT)
	private Table<?, ?> table;

	@Override
	public void run() {
		try {
			final OMEROLocation credentials = new OMEROLocation(getServer(),
				getPort(), getUser(), getPassword());
			table = omeroService.downloadTable(credentials, tableID);
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
	}

}
