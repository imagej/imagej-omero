/*
 * #%L
 * Call ImageJ commands from OMERO on the server side.
 * %%
 * Copyright (C) 2013 Board of Regents of the University of
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

package imagej.omero.server;

import imagej.data.Dataset;
import imagej.data.display.DatasetView;
import imagej.data.display.ImageDisplay;
import imagej.module.ModuleItem;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for converting between data types.
 * 
 * @author Curtis Rueden
 */
public final class ConversionUtils {

	private ConversionUtils() {
		// NB: Prevent instantiation of utility class.
	}

	// -- Utility methods --

	/** Converts ImageJ parameter metadata to OMERO parameter metadata. */
	public static omero.grid.Param convertItem(final ModuleItem<?> item) {
		final omero.grid.Param param = new omero.grid.Param();
		param.optional = !item.isRequired();
		param.prototype = prototype(item.getType());
		param.description = item.getDescription();
		return param;
	}

	/** Creates an OMERO parameter prototype for the given Java class. */
	public static omero.RType prototype(final Class<?> type) {
		// image types
		if (Dataset.class.isAssignableFrom(type) ||
			DatasetView.class.isAssignableFrom(type) ||
			ImageDisplay.class.isAssignableFrom(type))
		{
			// use a pixels ID
			return omero.rtypes.rlong(0);
		}

		// primitive types
		if (Boolean.class.isAssignableFrom(type)) {
			return omero.rtypes.rbool(false);
		}
		if (Double.class.isAssignableFrom(type)) {
			return omero.rtypes.rdouble(Double.NaN);
		}
		if (Float.class.isAssignableFrom(type)) {
			return omero.rtypes.rfloat(Float.NaN);
		}
		if (Integer.class.isAssignableFrom(type)) {
			return omero.rtypes.rint(0);
		}
		if (Long.class.isAssignableFrom(type)) {
			return omero.rtypes.rlong(0L);
		}

		// data structure types
		if (type.isArray()) {
			return omero.rtypes.rarray();
		}
		if (List.class.isAssignableFrom(type)) {
			return omero.rtypes.rlist();
		}
		if (Map.class.isAssignableFrom(type)) {
			return omero.rtypes.rmap();
		}
		if (Set.class.isAssignableFrom(type)) {
			return omero.rtypes.rset();
		}

		// default case: convert to string
		// works for many types, including but not limited to:
		// - char
		// - imagej.util.ColorRGB
		// - java.io.File
		// - java.lang.Character
		// - java.lang.String
		// - java.math.BigDecimal
		// - java.math.BigInteger
		return omero.rtypes.rstring("");
	}

	/** Converts a {@link Collection} to an array of the given type. */
	public static <T> T[] toArray(final Collection<Object> collection,
		final Class<T> type)
	{
		final Object array = Array.newInstance(type, 0);
		@SuppressWarnings("unchecked")
		final T[] typedArray = (T[]) array;
		return collection.toArray(typedArray);
	}

}
