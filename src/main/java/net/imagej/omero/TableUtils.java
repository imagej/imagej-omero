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

import org.apache.commons.lang.ArrayUtils;
import org.scijava.util.BoolArray;
import org.scijava.util.ByteArray;
import org.scijava.util.DoubleArray;
import org.scijava.util.FloatArray;
import org.scijava.util.IntArray;
import org.scijava.util.LongArray;
import org.scijava.util.PrimitiveArray;
import org.scijava.util.ShortArray;

import omero.ServerError;
import omero.gateway.model.DataObject;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PlateData;
import omero.gateway.model.ROIData;
import omero.gateway.model.TableDataColumn;
import omero.gateway.model.WellSampleData;

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
		final Column<?> imageJColumn, final int index) throws ServerError
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
			// NB: Must set the width of arrays contained in this column
			((omero.grid.DoubleArrayColumn) omeroColumn).size =
				width((DefaultColumn<PrimitiveArray<?, ?>>) imageJColumn);
		}
		else if (type == FloatArray.class) {
			omeroColumn = new omero.grid.FloatArrayColumn();
			// NB: Must set the width of arrays contained in this column
			((omero.grid.FloatArrayColumn) omeroColumn).size =
				width((DefaultColumn<PrimitiveArray<?, ?>>) imageJColumn);
		}
		else if (type == LongArray.class || type == IntArray.class ||
			type == ShortArray.class || type == ByteArray.class ||
			type == BoolArray.class)
		{
			omeroColumn = new omero.grid.LongArrayColumn();
			// NB: Must set the width of arrays contained in this column
			((omero.grid.LongArrayColumn) omeroColumn).size =
				width((DefaultColumn<PrimitiveArray<?, ?>>) imageJColumn);
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
				if (defaultColumn.get(i) == null) continue;
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
				if (defaultColumn.get(i) == null) continue;
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
				if (defaultColumn.get(i) == null) continue;
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
				if (defaultColumn.get(i) == null) continue;
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
				if (defaultColumn.get(i) == null) continue;
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
				if (defaultColumn.get(i) == null) continue;
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
				if (defaultColumn.get(i) == null) continue;
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
	public static void populateImageJColumn(final Class<?> type,
		final Object[] omeroColumnData, final Column<?> imageJColumn)
	{
		if (type.equals(Double.class)) {
			for (int i = 0; i < omeroColumnData.length; i++)
				((DoubleColumn) imageJColumn).add(i, (Double) omeroColumnData[i]);
		}
		else if (type.equals(Boolean.class)) {
			for (int i = 0; i < omeroColumnData.length; i++)
				((BoolColumn) imageJColumn).add(i, (Boolean) omeroColumnData[i]);
		}
		else if (type.equals(Long.class)) {
			for (int i = 0; i < omeroColumnData.length; i++)
				((LongColumn) imageJColumn).add(i, (Long) omeroColumnData[i]);
		}
		else if (type.equals(Double[].class) || type.equals(Float[].class) || type
			.equals(Long[].class))
		{
			populateArrayColumn((DefaultColumn<?>) imageJColumn, omeroColumnData);
		}
		else if (type.equals(String.class)) {
			for (int i = 0; i < omeroColumnData.length; i++)
				((DefaultColumn<String>) imageJColumn).add(i,
					(String) omeroColumnData[i]);
		}
		else if (type.equals(FileAnnotationData.class) || type.equals(
			ImageData.class) || type.equals(PlateData.class) || type.equals(
				ROIData.class) || type.equals(WellSampleData.class))
		{
			populateOMERORefColumn((OMERORefColumn) imageJColumn, omeroColumnData);
		}
		else {
			((GenericColumn) imageJColumn).setArray(omeroColumnData);
		}
	}

	public static Table<?, ?> createImageJTable(
		final TableDataColumn[] omeroColumns)
	{
		TableDataColumn prev = omeroColumns[0];
		for (int c = 1; c < omeroColumns.length; c++) {
			// if table contains a mixture of column types, return a GenericTable
			if (!prev.getType().equals(omeroColumns[c].getType())) {
				return new DefaultGenericTable();
			}
			prev = omeroColumns[c];
		}

		// NB: Despite TableData being able to contain any type of data, the backing
		// omero.grid structures are still limited in their types. For example,
		// if a ShortColumn was created when you went to upload it, it would be
		// stored as null since no matching column type was found.
		if (prev.getType().equals(Double.class)) {
			return new DefaultResultsTable();
		}
		else if (prev.getType().equals(Long.class)) {
			return new DefaultLongTable();
		}
		else if (prev.getType().equals(Boolean.class)) {
			return new DefaultBoolTable();
		}
		else {
			return new DefaultGenericTable();
		}
	}

	public static Column<?> createImageJColumn(final TableDataColumn column) {
		if (column.getType().equals(Boolean.class)) return new BoolColumn(column
			.getName());
		if (column.getType().equals(Double[].class)) return new DefaultColumn<>(
			DoubleArray.class, column.getName());
		if (column.getType().equals(Double.class)) return new DoubleColumn(column
			.getName());
		if (column.getType().equals(FileAnnotationData.class))
			return new OMERORefColumn(column.getName(), OMERORef.FILE);
		if (column.getType().equals(Float[].class)) return new DefaultColumn<>(
			FloatArray.class, column.getName());
		if (column.getType().equals(ImageData.class)) return new OMERORefColumn(
			column.getName(), OMERORef.IMAGE);
		if (column.getType().equals(Long[].class)) return new DefaultColumn<>(
			LongArray.class, column.getName());
		if (column.getType().equals(Long.class)) return new LongColumn(column
			.getName());
		if (column.getType().equals(MaskData.class)) {
			// TODO: Implement MaskColumn for efficiency.
//		  return new GenericColumn(column.getName());
		}
		if (column.getType().equals(PlateData.class)) return new OMERORefColumn(
			column.getName(), OMERORef.PLATE);
		if (column.getType().equals(ROIData.class)) return new OMERORefColumn(column
			.getName(), OMERORef.ROI);
		if (column.getType().equals(String.class)) return new DefaultColumn<>(
			String.class, column.getName());
		if (column.getType().equals(WellSampleData.class))
			return new OMERORefColumn(column.getName(), OMERORef.WELL);
		throw new IllegalArgumentException("Unsupported column type: " + column
			.getType());
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

	@SuppressWarnings("unchecked")
	private static void populateArrayColumn(final DefaultColumn<?> col,
		final Object[] data)
	{
		if (col.getType().equals(FloatArray.class)) {
			for (int i = 0; i < data.length; i++) {
				final Float[] f = (Float[]) data[i];
				((DefaultColumn<FloatArray>) col).add(i, new FloatArray(ArrayUtils
					.toPrimitive(f)));
			}
		}
		else if (col.getType().equals(LongArray.class)) {
			for (int i = 0; i < data.length; i++) {
				final Long[] f = (Long[]) data[i];
				((DefaultColumn<LongArray>) col).add(i, new LongArray(ArrayUtils
					.toPrimitive(f)));
			}
		}
		else if (col.getType().equals(DoubleArray.class)) {
			for (int i = 0; i < data.length; i++) {
				final Double[] f = (Double[]) data[i];
				((DefaultColumn<DoubleArray>) col).add(i, new DoubleArray(ArrayUtils
					.toPrimitive(f)));
			}
		}
	}

	private static void populateOMERORefColumn(final OMERORefColumn col,
		final Object[] data)
	{
		for (int i = 0; i < data.length; i++)
			col.add(i, ((DataObject) data[i]).getId());
		col.setOriginalData(data);
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
		for (int i = 0; i < array.length; i++) {
			if (array[i] != null && array[i].length() > longest) {
				longest = array[i].length();
			}
		}
		return longest;
	}

	private static long width(final DefaultColumn<PrimitiveArray<?, ?>> col)
		throws ServerError
	{
		long width = 0;
		if (col.get(0) != null) width = col.get(0).size();
		// All arrays in an OMERO array column must have equal widths
		for (int i = 0; i < col.size(); i++) {
			if (col.get(i) != null && col.get(i).size() != width) {
				throw new omero.ServerError(null, null,
					"Arrays in column must have equal widths");
			}
		}
		return width;
	}

}
