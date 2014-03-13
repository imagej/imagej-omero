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

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import net.imagej.table.Column;
import net.imagej.table.DefaultGenericTable;
import net.imagej.table.DefaultResultsTable;
import net.imagej.table.DoubleColumn;
import net.imagej.table.GenericColumn;
import net.imagej.table.Table;

import org.scijava.util.ClassUtils;

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

	public static void populateImageJColumn(final omero.grid.Column omeroColumn,
		final Column<?> imageJColumn, final int offset)
	{
		if (omeroColumn instanceof omero.grid.DoubleColumn) {
			final double[] data = ((omero.grid.DoubleColumn) omeroColumn).values;
			final DoubleColumn imageJDoubleColumn = (DoubleColumn) imageJColumn;
			System.arraycopy(data, 0, imageJDoubleColumn.getArray(), offset,
				data.length);
		}
		else {
			final GenericColumn imageJGenericColumn = (GenericColumn) imageJColumn;
			final Field field = ClassUtils.getField(omeroColumn.getClass(), "values");
			final Object data = ClassUtils.getValue(field, omeroColumn);
			final int length = Array.getLength(data);
			for (int r = 0; r < length; r++) {
				imageJGenericColumn.set(r + offset, Array.get(data, r));
			}
		}
	}

	public static Table<?, ?> createImageJTable(
		final omero.grid.Column[] omeroColumns)
	{
		// TODO Decide if we really need this case logic.
		for (int c = 0; c < omeroColumns.length; c++) {
			if (!(omeroColumns[c] instanceof omero.grid.DoubleColumn)) {
				// not all doubles
				return new DefaultGenericTable();
			}
		}
		// all double columns, yay
		return new DefaultResultsTable();
	}

	public static Column<?> createImageJColumn(final omero.grid.Column column) {
		if (column instanceof omero.grid.BoolColumn) {
			// TODO: Implement BoolColumn for efficiency.
			return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.DoubleArrayColumn) {
			// TODO: Implement DoubleArrayColumn for efficiency.
			return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.DoubleColumn) {
			// TODO: Implement DoubleColumn for efficiency.
			return new DoubleColumn(column.name);
		}
		if (column instanceof omero.grid.FileColumn) {
			// TODO: Implement FileColumn for efficiency.
			return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.FloatArrayColumn) {
			// TODO: Implement FloatArrayColumn for efficiency.
			return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.ImageColumn) {
			// TODO: Implement ImageColumn for efficiency.
//		  return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.LongArrayColumn) {
			// TODO: Implement LongArrayColumn for efficiency.
			return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.LongColumn) {
			// TODO: Implement LongColumn for efficiency.
			return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.MaskColumn) {
			// TODO: Implement MaskColumn for efficiency.
//		  return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.PlateColumn) {
			// TODO: Implement PlateColumn for efficiency.
//		  return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.RoiColumn) {
			// TODO: Implement RoiColumn for efficiency.
//		  return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.StringColumn) {
			// TODO: Implement StringColumn for efficiency.
			return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.WellColumn) {
			// TODO: Implement WellColumn for efficiency.
//		  return new GenericColumn(column.name);
		}
		throw new IllegalArgumentException("Unsupported column type: " +
			column.getClass().getName());
	}

}
