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

package net.imagej.omero;

import net.imagej.table.Column;
import net.imagej.table.DoubleColumn;

/**
 * Utility class for working with converting between ImageJ and OMERO tables.
 * 
 * @author Curtis Rueden
 */
public final class TableUtils {

	private TableUtils() {
		// NB: Prevent instantiation of utility class.
	}

	public static omero.grid.Column createOMEROColumn(
		final Column<?> imageJColumn, final int index)
	{
		// FIXME: need ImageJ to remember type of column via a getType() method
		// For now, we hardcode.
		final omero.grid.Column omeroColumn;
		if (imageJColumn instanceof DoubleColumn) {
			omeroColumn = new omero.grid.DoubleColumn();
		}
		else {
			final Class<?> type = imageJColumn.get(0).getClass();
			throw new UnsupportedOperationException("Not yet implemented: " +
				type.getName());
			/* TODO:
			BoolColumn
			DoubleArrayColumn
			DoubleColumn
			FileColumn
			FloatArrayColumn
			ImageColumn
			LongArrayColumn
			LongColumn
			MaskColumn
			PlateColumn
			RoiColumn
			StringColumn
			WellColumn
			*/
		}
		omeroColumn.name = imageJColumn.getHeader();
		if (omeroColumn.name == null) omeroColumn.name = "" + index;
		return omeroColumn;
	}

	public static void populateOMEROColumn(final Column<?> imageJColumn,
		final omero.grid.Column omeroColumn)
	{
		if (imageJColumn instanceof DoubleColumn) {
			final DoubleColumn doubleColumn = (DoubleColumn) imageJColumn;
			final omero.grid.DoubleColumn omeroDColumn =
				(omero.grid.DoubleColumn) omeroColumn;
			omeroDColumn.values = doubleColumn.getArray();
		}
		throw new UnsupportedOperationException("Unsupported column type: " +
			imageJColumn.getClass().getName());
	}

}
