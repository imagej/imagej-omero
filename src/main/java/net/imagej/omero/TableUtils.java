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
import org.scijava.util.PrimitiveArray;
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

	@SuppressWarnings("unchecked")
	public static omero.grid.Column createOMEROColumn(
		final Column<?> imageJColumn, final int index)
	{
		// FIXME: need ImageJ to remember type of column via a getType() method
		// For now, we hardcode.
		final Class<?> type = imageJColumn.getType();
		final omero.grid.Column omeroColumn;
		if (OMERORefColumn.class.isInstance(imageJColumn)) {
			omeroColumn =
				createOMERORefColumn(((OMERORefColumn) imageJColumn).getOMERORef());
		}
		else if (type == Double.class  || type == Float.class) {
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
		else if (type == Character.class) {
			omeroColumn = new omero.grid.StringColumn();
			// NB: Must set the maximum length of Strings contained in this column
			((omero.grid.StringColumn) omeroColumn).size = 1l;
		}
		else if (type == String.class) {
			omeroColumn = new omero.grid.StringColumn();
			// NB: Must set the maximum length of Strings contained in this column
			((omero.grid.StringColumn) omeroColumn).size =
				longestString(((DefaultColumn<String>) imageJColumn).getArray());
		}
		else {
			throw new UnsupportedOperationException("Not yet implemented: " +
				type.getName());
			/* TODO:
			 * MaskColumn
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
		final int s = imageJColumn.size();
		if (OMERORefColumn.class.isInstance(imageJColumn)) {
			populateOMERORefColumn(((OMERORefColumn) imageJColumn).getOMERORef(),
				((OMERORefColumn) imageJColumn).getArray(), omeroColumn, s);
		}
		else if (type == Double.class) {
			final DoubleColumn doubleColumn = (DoubleColumn) imageJColumn;
			final omero.grid.DoubleColumn omeroDColumn =
				(omero.grid.DoubleColumn) omeroColumn;
			omeroDColumn.values = new double[s];
			System.arraycopy(doubleColumn.getArray(), 0, omeroDColumn.values, 0, s);
		}
		else if (type == Float.class) {
			final omero.grid.DoubleColumn omeroDColumn =
				(omero.grid.DoubleColumn) omeroColumn;
			omeroDColumn.values =
				toDoubleArray(((FloatColumn) imageJColumn).getArray(), s);
		}
		else if (type == Boolean.class) {
			final BoolColumn boolColumn = (BoolColumn) imageJColumn;
			final omero.grid.BoolColumn omeroBColumn =
				(omero.grid.BoolColumn) omeroColumn;
			omeroBColumn.values = new boolean[s];
			System.arraycopy(boolColumn.getArray(), 0, omeroBColumn.values, 0, s);
		}
		else if (type == Long.class) {
			final LongColumn longColumn = (LongColumn) imageJColumn;
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values = new long[s];
			System.arraycopy(longColumn.getArray(), 0, omeroLColumn.values, 0, s);
		}
		else if (type == Byte.class) {
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values =
				toLongArray(((ByteColumn) imageJColumn).getArray(), s);
		}
		else if (type == Short.class) {
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values =
				toLongArray(((ShortColumn) imageJColumn).getArray(), s);
		}
		else if (type == Integer.class) {
			final omero.grid.LongColumn omeroLColumn =
				(omero.grid.LongColumn) omeroColumn;
			omeroLColumn.values =
				toLongArray(((IntColumn) imageJColumn).getArray(), s);
		}
		else if (type == DoubleArray.class) {
			final DefaultColumn<DoubleArray> defaultColumn =
				(DefaultColumn<DoubleArray>) imageJColumn;
			final omero.grid.DoubleArrayColumn omeroDAColumn =
				(omero.grid.DoubleArrayColumn) omeroColumn;
			final double[][] values = new double[defaultColumn.size()][];
			for (int i = 0; i < values.length; i++) {
				values[i] = new double[defaultColumn.get(i).size()];
				System.arraycopy(defaultColumn.get(i).getArray(), 0, values[i], 0,
					defaultColumn.get(i).size());
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
				values[i] = new float[defaultColumn.get(i).size()];
				System.arraycopy(defaultColumn.get(i).getArray(), 0, values[i], 0,
					defaultColumn.get(i).size());
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
				values[i] = new long[defaultColumn.get(i).size()];
				System.arraycopy(defaultColumn.get(i).getArray(), 0, values[i], 0,
					defaultColumn.get(i).size());
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
				values[i] = toLongArray(defaultColumn.get(i).getArray(), s);
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
				values[i] = toLongArray(defaultColumn.get(i).getArray(), s);
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
				values[i] = toLongArray(defaultColumn.get(i).getArray(), s);
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
				values[i] = toLongArray(defaultColumn.get(i).getArray(), s);
			}
			omeroLAColumn.values = values;
		}
		else if (type == String.class) {
			final DefaultColumn<String> defaultColumn =
				(DefaultColumn<String>) imageJColumn;
			final omero.grid.StringColumn omeroSColumn =
				(omero.grid.StringColumn) omeroColumn;
			omeroSColumn.values = new String[s];
			System.arraycopy(defaultColumn.getArray(), 0, omeroSColumn.values, 0, s);
		}
		else if (type == Character.class) {
			final CharColumn charColumn = (CharColumn) imageJColumn;
			final omero.grid.StringColumn omeroSColumn =
				(omero.grid.StringColumn) omeroColumn;
			final char[] temp = charColumn.getArray();
			final String[] values = new String[s];
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
		else if (omeroColumn instanceof omero.grid.FileColumn) {
			final long[] data = ((omero.grid.FileColumn) omeroColumn).values;
			((OMERORefColumn) imageJColumn).fill(data, offset);
		}
		else if (omeroColumn instanceof omero.grid.ImageColumn) {
			final long[] data = ((omero.grid.ImageColumn) omeroColumn).values;
			((OMERORefColumn) imageJColumn).fill(data, offset);
		}
		else if (omeroColumn instanceof omero.grid.PlateColumn) {
			final long[] data = ((omero.grid.PlateColumn) omeroColumn).values;
			((OMERORefColumn) imageJColumn).fill(data, offset);
		}
		else if (omeroColumn instanceof omero.grid.RoiColumn) {
			final long[] data = ((omero.grid.RoiColumn) omeroColumn).values;
			((OMERORefColumn) imageJColumn).fill(data, offset);
		}
		else if (omeroColumn instanceof omero.grid.WellColumn) {
			final long[] data = ((omero.grid.WellColumn) omeroColumn).values;
			((OMERORefColumn) imageJColumn).fill(data, offset);
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
			return new OMERORefColumn(column.name, OMERORef.FILE);
		}
		if (column instanceof omero.grid.FloatArrayColumn) {
			return new DefaultColumn<FloatArray>(FloatArray.class, column.name);
		}
		if (column instanceof omero.grid.ImageColumn) {
			return new OMERORefColumn(column.name, OMERORef.IMAGE);
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
			return new OMERORefColumn(column.name, OMERORef.PLATE);
		}
		if (column instanceof omero.grid.RoiColumn) {
			return new OMERORefColumn(column.name, OMERORef.ROI);
		}
		if (column instanceof omero.grid.StringColumn) {
			return new DefaultColumn<String>(String.class, column.name);
		}
		if (column instanceof omero.grid.WellColumn) {
			return new OMERORefColumn(column.name, OMERORef.WELL);
		}
		throw new IllegalArgumentException("Unsupported column type: " +
			column.getClass().getName());
	}

	// -- Helper methods --

	private static long[] toLongArray(final boolean[] b, final int size) {
		final long[] l = new long[size];
		for (int q = 0; q < l.length; q++) {
			l[q] = b[q] ? 1 : 0;
		}
		return l;
	}

	private static long[] toLongArray(final byte[] b, final int size) {
		final long[] l = new long[size];
		for (int q = 0; q < l.length; q++) {
			l[q] = b[q];
		}
		return l;
	}

	private static long[] toLongArray(final short[] s, final int size) {
		final long[] l = new long[size];
		for (int q = 0; q < l.length; q++) {
			l[q] = s[q];
		}
		return l;
	}

	private static long[] toLongArray(final int[] i, final int size) {
		final long[] l = new long[size];
		for (int q = 0; q < l.length; q++) {
			l[q] = i[q];
		}
		return l;
	}

	private static double[] toDoubleArray(final float[] i, final int size) {
		final double[] d = new double[size];
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

	private static omero.grid.Column createOMERORefColumn(final OMERORef refType)
	{
		if (refType == OMERORef.FILE) {
			return new omero.grid.FileColumn();
		}
		else if (refType == OMERORef.IMAGE) {
			return new omero.grid.ImageColumn();
		}
		else if (refType == OMERORef.PLATE) {
			return new omero.grid.PlateColumn();
		}
		else if (refType == OMERORef.ROI) {
			return new omero.grid.RoiColumn();
		}
		else if (refType == OMERORef.WELL) {
			return new omero.grid.WellColumn();
		}
		else {
			throw new UnsupportedOperationException(
				"Not yet implemented reference column for " + refType.name());
		}
	}

	private static void populateOMERORefColumn(final OMERORef refType,
		final long[] values, final omero.grid.Column omeroColumn, final int size)
	{
		final long[] ar = new long[size];
		System.arraycopy(values, 0, ar, 0, size);
		if (refType == OMERORef.FILE) {
			((omero.grid.FileColumn) omeroColumn).values = ar.clone();
		}
		else if (refType == OMERORef.IMAGE) {
			((omero.grid.ImageColumn) omeroColumn).values = ar.clone();
		}
		else if (refType == OMERORef.PLATE) {
			((omero.grid.PlateColumn) omeroColumn).values = ar.clone();
		}
		else if (refType == OMERORef.ROI) {
			((omero.grid.RoiColumn) omeroColumn).values = ar.clone();
		}
		else if (refType == OMERORef.WELL) {
			((omero.grid.WellColumn) omeroColumn).values = ar.clone();
		}
	}

	private static long longestString(final String[] array) {
		long longest = 0;
		for(int i = 0; i < array.length; i++) {
			if(array[i] != null && array[i].length() > longest) {
				longest = array[i].length();
			}
		}
		return longest;
	}

}
