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

package net.imagej.omero;

import net.imagej.table.BoolColumn;
import net.imagej.table.Column;
import net.imagej.table.DefaultBoolTable;
import net.imagej.table.DefaultColumn;
import net.imagej.table.DefaultGenericTable;
import net.imagej.table.DefaultLongTable;
import net.imagej.table.DefaultResultsTable;
import net.imagej.table.DoubleColumn;
import net.imagej.table.GenericColumn;
import net.imagej.table.LongColumn;
import net.imagej.table.Table;

import org.apache.commons.lang.ArrayUtils;
import org.scijava.convert.ConvertService;
import org.scijava.util.BoolArray;
import org.scijava.util.ByteArray;
import org.scijava.util.DoubleArray;
import org.scijava.util.FloatArray;
import org.scijava.util.IntArray;
import org.scijava.util.LongArray;
import org.scijava.util.ShortArray;

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

	public static TableDataColumn createOMEROColumn(final Column<?> imageJColumn,
		final int index)
	{
		final Class<?> type = imageJColumn.getType();
		final Class<?> omeroType;
		if (OMERORefColumn.class.isInstance(imageJColumn)) omeroType =
			createOMERORefColumn(((OMERORefColumn) imageJColumn).getOMERORef());
		else if (type.equals(Double.class) || type.equals(Float.class)) omeroType =
			Double.class;
		else if (type.equals(Boolean.class)) omeroType = Boolean.class;
		else if (type.equals(Long.class) || type.equals(Integer.class) || type
			.equals(Short.class) || type.equals(Byte.class)) omeroType = Long.class;
		else if (type.equals(DoubleArray.class)) omeroType = Double[].class;
		else if (type.equals(FloatArray.class)) omeroType = Float[].class;
		else if (type.equals(LongArray.class) || type.equals(IntArray.class) || type
			.equals(ShortArray.class) || type.equals(ByteArray.class) || type.equals(
				BoolArray.class)) omeroType = Long[].class;
		else if (type.equals(Character.class) || type.equals(String.class))
			omeroType = String.class;
		else if (type.equals(Object.class) && checkIfDoubleColumn(imageJColumn))
			omeroType = Double.class;
		else if (type.equals(Object.class) && checkIfStringColumn(imageJColumn))
			omeroType = String.class;
		else {
			throw new UnsupportedOperationException("Not yet implemented: " + type
				.getName());
			/* TODO:
			 * MaskColumn
			 */
		}
		String header = imageJColumn.getHeader();
		if (header == null) header = "" + index;
		return new TableDataColumn(header, index, omeroType);
	}

	public static Object[] populateOMEROColumn(final Column<?> imageJColumn,
		final ConvertService convertService)
	{
		final Class<?> type = imageJColumn.getType();
		if (OMERORefColumn.class.isInstance(imageJColumn))
			return ((OMERORefColumn) imageJColumn).getOriginalData();
		if (type.equals(Boolean.class)) return imageJColumn.toArray();
		if (type.equals(Double.class) || type.equals(Float.class))
			return convertService.convert(imageJColumn.toArray(), Double[].class);
		if (type.equals(Long.class) || type.equals(Integer.class) || type.equals(
			Short.class) || type.equals(Byte.class)) return convertService.convert(
				imageJColumn.toArray(), Long[].class);
		if (type.equals(DoubleArray.class)) return convertService.convert(
			imageJColumn.toArray(), Double[][].class);
		if (type.equals(FloatArray.class)) return convertService.convert(
			imageJColumn.toArray(), Float[][].class);
		if (type.equals(LongArray.class) || type.equals(IntArray.class) || type
			.equals(ShortArray.class) || type.equals(ByteArray.class))
			return convertService.convert(imageJColumn.toArray(), Long[][].class);
		if (type.equals(String.class) || type.equals(Character.class))
			return convertService.convert(imageJColumn.toArray(), String[].class);
		if (type.equals(Object.class) && checkIfDoubleColumn(imageJColumn))
			return getGenericColumnValuesDouble(imageJColumn);
		if (type.equals(Object.class) && checkIfStringColumn(imageJColumn))
			return getGenericColumnValuesString(imageJColumn);
		throw new UnsupportedOperationException("Unsupported column type: " +
			imageJColumn.getClass().getName());
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

	private static Class<?> createOMERORefColumn(final OMERORef refType) {
		if (refType == OMERORef.FILE) return FileAnnotationData.class;
		if (refType == OMERORef.IMAGE) return ImageData.class;
		if (refType == OMERORef.PLATE) return PlateData.class;
		if (refType == OMERORef.ROI) return ROIData.class;
		if (refType == OMERORef.WELL) return WellSampleData.class;
		throw new UnsupportedOperationException(
			"Not yet implemented reference column for " + refType.name());
	}

	private static boolean checkIfDoubleColumn(final Column<?> c) {
		for (int i = 0; i < c.size(); i++) {
			if (c.get(i) instanceof Double) continue;
			return false;
		}
		return true;
	}

	private static boolean checkIfStringColumn(final Column<?> c) {
		for (int i = 0; i < c.size(); i++) {
			if (c.get(i) instanceof String) continue;
			return false;
		}
		return true;
	}

	private static Double[] getGenericColumnValuesDouble(final Column<?> c) {
		final Double[] values = new Double[c.size()];
		for (int i = 0; i < c.size(); i++)
			values[i] = (Double) c.get(i);
		return values;
	}

	private static String[] getGenericColumnValuesString(final Column<?> c) {
		final String[] values = new String[c.size()];
		for (int i = 0; i < c.size(); i++)
			values[i] = (String) c.get(i);
		return values;
	}

}
