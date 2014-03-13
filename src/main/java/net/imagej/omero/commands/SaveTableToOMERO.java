/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2015 Open Microscopy Environment:
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

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import net.imagej.omero.DefaultOMEROService;
import net.imagej.omero.OMEROCommand;
import net.imagej.omero.OMEROCredentials;
import net.imagej.omero.OMEROService;
import net.imagej.table.DefaultResultsTable;
import net.imagej.table.ResultsTable;
import net.imagej.table.Table;
import omero.ServerError;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/** An ImageJ command for uploading a results table to an OMERO server. */
@Plugin(type = Command.class, label = "Export Table to OMERO", menu = {
	@Menu(label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
		mnemonic = MenuConstants.FILE_MNEMONIC),
	@Menu(label = "Export", weight = 6),
	@Menu(label = "OMERO Table... ", weight = 100, mnemonic = 'o') })
public class SaveTableToOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private String name;

	@Parameter(required = false)
	private Table<?, ?> table;

	@Override
	public void run() {
		final OMEROCredentials credentials = new OMEROCredentials();
		credentials.setServer(getServer());
		credentials.setPort(getPort());
		credentials.setUser(getUser());
		credentials.setPassword(getPassword());

		if (table == null) table = createBaseballTable();

		try {
			((DefaultOMEROService) omeroService).uploadTable(credentials, name, table);
		}
		catch (ServerError exc) {
			log.error(exc);
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (PermissionDeniedException exc) {
			log.error(exc);
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (CannotCreateSessionException exc) {
			log.error(exc);
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
	}

	private ResultsTable createBaseballTable() {
		final double[][] data = {
			{1978, 21, .273},
			{1979, 22, .322},
			{1980, 23, .304},
			{1981, 24, .267},
			{1982, 25, .302},
			{1983, 26, .270},
			{1984, 27, .217},
			{1985, 28, .297},
			{1986, 29, .281},
			{1987, 30, .353},
			{1988, 31, .312},
			{1989, 32, .315},
			{1990, 33, .285},
			{1991, 34, .325},
			{1992, 35, .320},
			{1993, 36, .332},
			{1994, 37, .341},
			{1995, 38, .270},
			{1996, 39, .341},
			{1997, 40, .305},
			{1998, 41, .281},
		};
		DefaultResultsTable baseball = new DefaultResultsTable(data[0].length, data.length);
		baseball.setColumnHeader(0, "Year");
		baseball.setColumnHeader(1, "Age");
		baseball.setColumnHeader(2, "BA");
		baseball.setRowHeader(9, "Best");
		for (int row = 0; row < data.length; row++) {
			for (int col = 0; col < data[row].length; col++) {
				baseball.setValue(col, row, data[row][col]);
			}
		}
		return baseball;
	}

}
