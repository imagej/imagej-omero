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
import net.imagej.table.TableDisplay;

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

/** An ImageJ command for uploading a results table to an OMERO server. */
@Plugin(type = Command.class, label = "Export Table to OMERO", menu = { @Menu(
	label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
	mnemonic = MenuConstants.FILE_MNEMONIC), @Menu(label = "Export", weight = 6),
	@Menu(label = "OMERO Table... ", weight = 100, mnemonic = 'o') })
public class SaveTableToOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private String name;

	@Parameter
	private TableDisplay tableDisplay;

	@Parameter
	private long imageID;

	@Override
	public void run() {
		final Table<?, ?> table = tableDisplay.get(0);

		try {
			final OMEROLocation credentials = new OMEROLocation(getServer(),
				getPort(), getUser(), getPassword());
			omeroService.uploadTable(credentials, name, table, imageID);
		}
		catch (final ServerError exc) {
			log.error(exc);
			cancel("Error talking to OMERO: " + exc.message);
		}
		catch (final PermissionDeniedException exc) {
			log.error(exc);
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final CannotCreateSessionException exc) {
			log.error(exc);
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final ExecutionException exc) {
			log.error(exc);
			cancel("Error attaching table to OMERO image: " + exc.getMessage());
		}
		catch (final DSOutOfServiceException exc) {
			log.error(exc);
			cancel("Error attaching table to OMERO image: " + exc.getMessage());
		}
		catch (final DSAccessException exc) {
			log.error(exc);
			cancel("Error attaching table to OMERO image: " + exc.getMessage());
		}
		catch (final URISyntaxException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error creating URI for Session: " + exc.getMessage());
		}
	}

}
