/*
 * #%L
 * Server- and client-side communication between ImageJ and OMERO.
 * %%
 * Copyright (C) 2013 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
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

package imagej.omero;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import imagej.command.Command;
import imagej.data.table.Table;
import imagej.menu.MenuConstants;
import io.scif.omero.OMEROCredentials;
import omero.ServerError;

import org.scijava.ItemIO;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/** An ImageJ command for downloading a results table from an OMERO server. */
@Plugin(type = Command.class, label = "Import Table from OMERO", menu = {
	@Menu(label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
		mnemonic = MenuConstants.FILE_MNEMONIC),
	@Menu(label = "Import", weight = 6),
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
		final OMEROCredentials credentials = new OMEROCredentials();
		credentials.setServer(getServer());
		credentials.setPort(getPort());
		credentials.setUser(getUser());
		credentials.setPassword(getPassword());
		try {
			table = ((DefaultOMEROService) omeroService).downloadTable(credentials, tableID);
			log.info("GOT A TABLE: " + table);//TEMP
			System.out.println("GOT A TABLE: " + table);
		}
		catch (ServerError exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (PermissionDeniedException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (CannotCreateSessionException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
	}

}
