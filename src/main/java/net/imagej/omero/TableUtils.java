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

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import net.imagej.table.BoolColumn;
import net.imagej.table.ByteColumn;
import net.imagej.table.CharColumn;
import net.imagej.table.Column;
import net.imagej.table.DefaultBoolTable;
import net.imagej.table.DefaultColumn;
import net.imagej.table.DefaultGenericTable;
import net.imagej.table.DefaultLongTable;
import net.imagej.table.DefaultResultsTable;
import net.imagej.table.DoubleColumn;
import net.imagej.table.FloatColumn;
import net.imagej.table.GenericColumn;
import net.imagej.table.IntColumn;
import net.imagej.table.LongColumn;
import net.imagej.table.ShortColumn;
import net.imagej.table.Table;

import org.scijava.util.BoolArray;
import org.scijava.util.ByteArray;
import org.scijava.util.ClassUtils;
import org.scijava.util.DoubleArray;
import org.scijava.util.FloatArray;
import org.scijava.util.IntArray;
import org.scijava.util.LongArray;
import org.scijava.util.ShortArray;

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
		final Class<?> type = imageJColumn.getType();
		final omero.grid.Column omeroColumn;
		if (type == Double.class || type == Float.class) {
			omeroColumn = new omero.grid.DoubleColumn();
		}
		else if (type == Boolean.class) {
			omeroColumn = new omero.grid.BoolColumn();
		}
		else if (type == Long.class || type == Integer.class ||
			type == Short.class || type == Byte.class)
		{
			omeroColumn = new omero.grid.LongColumn();
		}
		else if (type == DoubleArray.class) {
			omeroColumn = new omero.grid.DoubleArrayColumn();
		}
		else if (type == FloatArray.class) {
			omeroColumn = new omero.grid.FloatArrayColumn();
		}
		else if (type == LongArray.class || type == IntArray.class ||
			type == ShortArray.class || type == ByteArray.class ||
			type == BoolArray.class)
		{
			omeroColumn = new omero.grid.LongArrayColumn();
		}
		else if (type == File.class) {
			omeroColumn = new omero.grid.FileColumn();
		}
		else if (type == String.class || type == Character.class) {
			omeroColumn = new omero.grid.StringColumn();
		}
		else {
			throw new UnsupportedOperationException("Not yet implemented: " +
				type.getName());
			/* TODO:
			ImageColumn
			MaskColumn
			PlateColumn
			RoiColumn
			WellColumn
			*/
		}
		omeroColumn.name = imageJColumn.getHeader();
		if (omeroColumn.name == null) omeroColumn.name = "" + index;
		return omeroColumn;
	}

	@SuppressWarnings("unchecked")
	public static void populateOMEROColumn(final Column<?> imageJColumn,
		final omero.grid.Column omeroColumn)
	{
		final Class<?> type = imageJColumn.getType();
		if (type == Double.class) {
			final DoubleColumn doubleColumn = (DoubleColumn) imageJColumn;
			final omero.grid.DoubleColumn omeroDColumn =
				(omero.grid.DoubleColumn) omeroColumn;
			omeroDColumn.values = doubleColumn.getArray();
		}
		else if (type == Float.class) {
			final omero.grid.DoubleColumn omeroDColumn =
				(omero.grid.DoubleColumn) omeroColumn;
			omeroDColumn.values =
					toDoubleArray(((FloatColumn) imageJColumn).getArray());
		}
		else if (type == Boolean.class) {
			final BoolColumn boolColumn = (BoolColumn) imageJColumn;
			final omero.grid.BoolColumn omeroBColumn =
				(omero.grid.BoolColumn) omeroColumn;
			omeroBColumn.values = boolColumn.getArray();
		}
		else if (type == Long.class) {
			final LongColumn longColumn = (LongColumn) imageJColumn;
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values = longColumn.getArray();
		}
		else if (type == Byte.class) {
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values = toLongArray(((ByteColumn) imageJColumn).getArray());
		}
		else if (type == Short.class) {
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values =
				toLongArray(((ShortColumn) imageJColumn).getArray());
		}
		else if (type == Integer.class) {
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values = toLongArray(((IntColumn) imageJColumn).getArray());
		}
		else if (type == DoubleArray.class) {
			final DefaultColumn<DoubleArray> defaultColumn =
				(DefaultColumn<DoubleArray>) imageJColumn;
			final omero.grid.DoubleArrayColumn omeroDAColumn =
				(omero.grid.DoubleArrayColumn) omeroColumn;
			final double[][] values = new double[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = defaultColumn.get(i).getArray();
			}
			omeroDAColumn.values = values;
		}
		else if (type == FloatArray.class) {
			final DefaultColumn<FloatArray> defaultColumn =
				(DefaultColumn<FloatArray>) imageJColumn;
			final omero.grid.FloatArrayColumn omeroFAColumn =
				(omero.grid.FloatArrayColumn) omeroColumn;
			final float[][] values = new float[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = defaultColumn.get(i).getArray();
			}
			omeroFAColumn.values = values;
		}
		else if (type == LongArray.class) {
			final DefaultColumn<LongArray> defaultColumn =
				(DefaultColumn<LongArray>) imageJColumn;
			final omero.grid.LongArrayColumn omeroLAColumn =
				(omero.grid.LongArrayColumn) omeroColumn;
			final long[][] values = new long[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = defaultColumn.get(i).getArray();
			}
			omeroLAColumn.values = values;
		}
		else if (type == IntArray.class) {
			final DefaultColumn<IntArray> defaultColumn =
				(DefaultColumn<IntArray>) imageJColumn;
			final omero.grid.LongArrayColumn omeroLAColumn =
				(omero.grid.LongArrayColumn) omeroColumn;
			final long[][] values = new long[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = toLongArray(defaultColumn.get(i).getArray());
			}
			omeroLAColumn.values = values;
		}
		else if (type == ShortArray.class) {
			final DefaultColumn<ShortArray> defaultColumn =
				(DefaultColumn<ShortArray>) imageJColumn;
			final omero.grid.LongArrayColumn omeroLAColumn =
				(omero.grid.LongArrayColumn) omeroColumn;
			final long[][] values = new long[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = toLongArray(defaultColumn.get(i).getArray());
			}
			omeroLAColumn.values = values;
		}
		else if (type == ByteArray.class) {
			final DefaultColumn<ByteArray> defaultColumn =
				(DefaultColumn<ByteArray>) imageJColumn;
			final omero.grid.LongArrayColumn omeroLAColumn =
				(omero.grid.LongArrayColumn) omeroColumn;
			final long[][] values = new long[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = toLongArray(defaultColumn.get(i).getArray());
			}
			omeroLAColumn.values = values;
		}
		else if (type == BoolArray.class) {
			final DefaultColumn<BoolArray> defaultColumn =
				(DefaultColumn<BoolArray>) imageJColumn;
			final omero.grid.LongArrayColumn omeroLAColumn =
				(omero.grid.LongArrayColumn) omeroColumn;
			final long[][] values = new long[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = toLongArray(defaultColumn.get(i).getArray());
			}
			omeroLAColumn.values = values;
		}
		else if (type == String.class) {
			final DefaultColumn<String> defaultColumn =
				(DefaultColumn<String>) imageJColumn;
			final omero.grid.StringColumn omeroSColumn =
				(omero.grid.StringColumn) omeroColumn;
			omeroSColumn.values = defaultColumn.getArray();
		}
		else if (type == Character.class) {
			final CharColumn charColumn = (CharColumn) imageJColumn;
			final omero.grid.StringColumn omeroSColumn =
				(omero.grid.StringColumn) omeroColumn;
			final char[] temp = charColumn.getArray();
			final String[] values = new String[temp.length];
			for (int i = 0; i < values.length; i++) {
				values[i] = String.valueOf(temp[i]);
			}
			omeroSColumn.values = values;
		}
		else {
			throw new UnsupportedOperationException("Unsupported column type: " +
				imageJColumn.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	public static void populateImageJColumn(final omero.grid.Column omeroColumn,
		final Column<?> imageJColumn, final int offset)
	{
		if (omeroColumn instanceof omero.grid.DoubleColumn) {
			final double[] data = ((omero.grid.DoubleColumn) omeroColumn).values;
			((DoubleColumn) imageJColumn).fill(data, offset);
		}
		else if (omeroColumn instanceof omero.grid.BoolColumn) {
			final boolean[] data = ((omero.grid.BoolColumn) omeroColumn).values;
			((BoolColumn) imageJColumn).fill(data, offset);
		}
		else if (omeroColumn instanceof omero.grid.LongColumn) {
			final long[] data = ((omero.grid.LongColumn) omeroColumn).values;
			((LongColumn) imageJColumn).fill(data, offset);
		}
		else if (omeroColumn instanceof omero.grid.DoubleArrayColumn) {
			final double[][] data =
				((omero.grid.DoubleArrayColumn) omeroColumn).values;
			final DefaultColumn<DoubleArray> imageJDoubleArrayColumn =
				(DefaultColumn<DoubleArray>) imageJColumn;
			for (int i = 0; i < data.length; i++) {
				populateArrayColumn(offset, data, imageJDoubleArrayColumn, i);
			}
		}
		else if (omeroColumn instanceof omero.grid.FloatArrayColumn) {
			final float[][] data = ((omero.grid.FloatArrayColumn) omeroColumn).values;
			final DefaultColumn<FloatArray> imageJFloatArrayColumn =
				(DefaultColumn<FloatArray>) imageJColumn;
			for (int i = 0; i < data.length; i++) {
				populateArrayColumn(offset, data, imageJFloatArrayColumn, i);
			}
		}
		else if (omeroColumn instanceof omero.grid.LongArrayColumn) {
			final long[][] data = ((omero.grid.LongArrayColumn) omeroColumn).values;
			final DefaultColumn<LongArray> imageJLongArrayColumn =
				(DefaultColumn<LongArray>) imageJColumn;
			for (int i = 0; i < data.length; i++) {
				populateArrayColumn(offset, data, imageJLongArrayColumn, i);
			}
		}
		else if (omeroColumn instanceof omero.grid.StringColumn) {
			final String[] data = ((omero.grid.StringColumn) omeroColumn).values;
			final DefaultColumn<String> imageJStringColumn =
				(DefaultColumn<String>) imageJColumn;
			if (imageJStringColumn.getArray() == null) {
				imageJStringColumn.setArray(data.clone());
			}
			else {
				System.arraycopy(data, 0, imageJStringColumn.getArray(), offset,
					data.length);
			}
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
		if (imageJColumn.getHeader() == null) imageJColumn
			.setHeader(omeroColumn.name);
	}

	public static Table<?, ?> createImageJTable(
		final omero.grid.Column[] omeroColumns)
	{
		omero.grid.Column prev = omeroColumns[0];
		for (int c = 1; c < omeroColumns.length; c++) {
			// if table contains a mixture of column types, return a GenericTable
			if (!prev.getClass().equals(omeroColumns[c].getClass())) {
				return new DefaultGenericTable();
			}
			prev = omeroColumns[c];
		}

		// Uniform column types
		prev = omeroColumns[0];
		if (prev instanceof omero.grid.DoubleColumn) {
			return new DefaultResultsTable();
		}
		else if (prev instanceof omero.grid.LongColumn) {
			return new DefaultLongTable();
		}
		else if (prev instanceof omero.grid.BoolColumn) {
			return new DefaultBoolTable();
		}
		else {
			return new DefaultGenericTable();
		}
	}

	public static Column<?> createImageJColumn(final omero.grid.Column column) {
		if (column instanceof omero.grid.BoolColumn) {
			return new BoolColumn(column.name);
		}
		if (column instanceof omero.grid.DoubleArrayColumn) {
			return new DefaultColumn<DoubleArray>(DoubleArray.class, column.name);
		}
		if (column instanceof omero.grid.DoubleColumn) {
			return new DoubleColumn(column.name);
		}
		if (column instanceof omero.grid.FileColumn) {
			return new DefaultColumn<File>(File.class, column.name);
		}
		if (column instanceof omero.grid.FloatArrayColumn) {
			return new DefaultColumn<FloatArray>(FloatArray.class, column.name);
		}
		if (column instanceof omero.grid.ImageColumn) {
			// TODO: Implement ImageColumn for efficiency.
//		  return new GenericColumn(column.name);
		}
		if (column instanceof omero.grid.LongArrayColumn) {
			return new DefaultColumn<LongArray>(LongArray.class, column.name);
		}
		if (column instanceof omero.grid.LongColumn) {
			return new LongColumn(column.name);
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
			return new DefaultColumn<String>(String.class, column.name);
		}
		if (column instanceof omero.grid.WellColumn) {
			// TODO: Implement WellColumn for efficiency.
//		  return new GenericColumn(column.name);
		}
		throw new IllegalArgumentException("Unsupported column type: " +
			column.getClass().getName());
	}

	// -- Helper methods --

	private static long[] toLongArray(boolean[] b) {
		final long[] l = new long[b.length];
		for (int q = 0; q < l.length; q++) {
			l[q] = b[q] ? 1 : 0;
		}
		return l;
	}

	private static long[] toLongArray(byte[] b) {
		final long[] l = new long[b.length];
		for (int q = 0; q < l.length; q++) {
			l[q] = b[q];
		}
		return l;
	}

	private static long[] toLongArray(short[] s) {
		final long[] l = new long[s.length];
		for (int q = 0; q < l.length; q++) {
			l[q] = s[q];
		}
		return l;
	}

	private static long[] toLongArray(int[] i) {
		final long[] l = new long[i.length];
		for (int q = 0; q < l.length; q++) {
			l[q] = i[q];
		}
		return l;
	}

	private static double[] toDoubleArray(float[] i) {
		final double[] d = new double[i.length];
		for (int q = 0; q < d.length; q++) {
			d[q] = i[q];
		}
		return d;
	}

	private static void populateArrayColumn(final int offset,
		final long[][] data, final DefaultColumn<LongArray> imageJLongArrayColumn,
		final int col)
	{
		if (imageJLongArrayColumn.get(col) == null) {
			imageJLongArrayColumn.set(col, new LongArray(data[col].clone()));
		}
		else if (imageJLongArrayColumn.get(col) != null &&
			imageJLongArrayColumn.get(col).getArray() == null)
		{
			imageJLongArrayColumn.get(col).setArray(data[col].clone());
		}
		else {
			System.arraycopy(data[col], 0, imageJLongArrayColumn.get(col).getArray(),
				offset, data.length);
		}
	}

	private static void populateArrayColumn(final int offset,
		final float[][] data,
		final DefaultColumn<FloatArray> imageJFloatArrayColumn, final int col)
	{
		if (imageJFloatArrayColumn.get(col) == null) {
			imageJFloatArrayColumn.set(col, new FloatArray(data[col].clone()));
		}
		else if (imageJFloatArrayColumn.get(col) != null &&
			imageJFloatArrayColumn.get(col).getArray() == null)
		{
			imageJFloatArrayColumn.get(col).setArray(data[col].clone());
		}
		else {
			System.arraycopy(data[col], 0,
				imageJFloatArrayColumn.get(col).getArray(), offset, data.length);
		}
	}

	private static void populateArrayColumn(final int offset,
		final double[][] data,
		final DefaultColumn<DoubleArray> imageJDoubleArrayColumn, final int col)
	{
		if (imageJDoubleArrayColumn.get(col) == null) {
			imageJDoubleArrayColumn.set(col, new DoubleArray(data[col].clone()));
		}
		else if (imageJDoubleArrayColumn.get(col) != null &&
			imageJDoubleArrayColumn.get(col).getArray() == null)
		{
			imageJDoubleArrayColumn.get(col).setArray(data[col].clone());
		}
		else {
			System.arraycopy(data[col], 0, imageJDoubleArrayColumn.get(col)
				.getArray(), offset, data.length);
		}
	}
}
