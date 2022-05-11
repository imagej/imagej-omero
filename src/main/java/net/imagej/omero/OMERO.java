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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.unit.Unit;
import omero.model.enums.UnitsLength;
import omero.model.enums.UnitsTime;

/**
 * Static utility methods for working with OMERO.
 * 
 * @author Curtis Rueden
 */
public final class OMERO {

	private OMERO() {
		// NB: Prevent instantiation of utility class.
	}

	/**
	 * Converts a {@link UnitsLength} object into a {@link Unit}{@code <Length>}.
	 */
	public static Unit<Length> unit(final UnitsLength unit) {
		switch (unit) {
			case ANGSTROM:
				return UNITS.ANGSTROM;
			case ASTRONOMICALUNIT:
				return UNITS.ASTRONOMICALUNIT;
			case ATTOMETER:
				return UNITS.ATTOMETER;
			case CENTIMETER:
				return UNITS.CENTIMETER;
			case DECAMETER:
				return UNITS.DECAMETER;
			case DECIMETER:
				return UNITS.DECIMETER;
			case EXAMETER:
				return UNITS.EXAMETER;
			case FEMTOMETER:
				return UNITS.FEMTOMETER;
			case FOOT:
				return UNITS.FOOT;
			case GIGAMETER:
				return UNITS.GIGAMETER;
			case HECTOMETER:
				return UNITS.HECTOMETER;
			case INCH:
				return UNITS.INCH;
			case KILOMETER:
				return UNITS.KILOMETER;
			case LIGHTYEAR:
				return UNITS.LIGHTYEAR;
			case LINE:
				return UNITS.LINE;
			case MEGAMETER:
				return UNITS.MEGAMETER;
			case METER:
				return UNITS.METER;
			case MICROMETER:
				return UNITS.MICROMETER;
			case MILE:
				return UNITS.MILE;
			case MILLIMETER:
				return UNITS.MILLIMETER;
			case NANOMETER:
				return UNITS.NANOMETER;
			case PARSEC:
				return UNITS.PARSEC;
			case PETAMETER:
				return UNITS.PETAMETER;
			case PICOMETER:
				return UNITS.PICOMETER;
			case PIXEL:
				return UNITS.PIXEL;
			case POINT:
				return UNITS.POINT;
			case REFERENCEFRAME:
				return UNITS.REFERENCEFRAME;
			case TERAMETER:
				return UNITS.TERAMETER;
			case THOU:
				return UNITS.THOU;
			case YARD:
				return UNITS.YARD;
			case YOCTOMETER:
				return UNITS.YOCTOMETER;
			case YOTTAMETER:
				return UNITS.YOTTAMETER;
			case ZEPTOMETER:
				return UNITS.ZEPTOMETER;
			case ZETTAMETER:
				return UNITS.ZETTAMETER;
			default:
				throw new IllegalArgumentException("Unknown unit: " + unit);
		}
	}

	/** Converts a {@link UnitsTime} object into a {@link Unit}{@code <Time>}. */
	public static Unit<Time> unit(final UnitsTime unit) {
		switch (unit) {
			case ATTOSECOND:
				return UNITS.ATTOSECOND;
			case CENTISECOND:
				return UNITS.CENTISECOND;
			case DAY:
				return UNITS.DAY;
			case DECASECOND:
				return UNITS.DECASECOND;
			case DECISECOND:
				return UNITS.DECISECOND;
			case EXASECOND:
				return UNITS.EXASECOND;
			case FEMTOSECOND:
				return UNITS.FEMTOSECOND;
			case GIGASECOND:
				return UNITS.GIGASECOND;
			case HECTOSECOND:
				return UNITS.HECTOSECOND;
			case HOUR:
				return UNITS.HOUR;
			case KILOSECOND:
				return UNITS.KILOSECOND;
			case MEGASECOND:
				return UNITS.MEGASECOND;
			case MICROSECOND:
				return UNITS.MICROSECOND;
			case MILLISECOND:
				return UNITS.MILLISECOND;
			case MINUTE:
				return UNITS.MINUTE;
			case NANOSECOND:
				return UNITS.NANOSECOND;
			case PETASECOND:
				return UNITS.PETASECOND;
			case PICOSECOND:
				return UNITS.PICOSECOND;
			case SECOND:
				return UNITS.SECOND;
			case TERASECOND:
				return UNITS.TERASECOND;
			case YOCTOSECOND:
				return UNITS.YOCTOSECOND;
			case YOTTASECOND:
				return UNITS.YOTTASECOND;
			case ZEPTOSECOND:
				return UNITS.ZEPTOSECOND;
			case ZETTASECOND:
				return UNITS.ZETTASECOND;
			default:
				throw new IllegalArgumentException("Unknown unit: " + unit);
		}
	}

	public static omero.RType rtype(final Object value) {
		if (value == null) return null;

		// NB: Unfortunately, omero.rtypes.rtype is not smart enough
		// to recurse into data structures, so we do it ourselves!

		// TODO: Use omero.rtypes.wrap, now that it exists!
		// https://github.com/openmicroscopy/openmicroscopy/commit/0767a2e37996d553bbdec343488b7b385756490a

		if (value.getClass().isArray()) {
			final omero.RType[] val = new omero.RType[Array.getLength(value)];
			for (int i = 0; i < val.length; i++) {
				val[i] = rtype(Array.get(value, i));
			}
			return omero.rtypes.rarray(val);
		}
		if (value instanceof List) {
			final List<?> list = (List<?>) value;
			final omero.RType[] val = new omero.RType[list.size()];
			for (int i = 0; i < val.length; i++) {
				val[i] = rtype(list.get(i));
			}
			return omero.rtypes.rlist(val);
		}
		if (value instanceof Map) {
			final Map<?, ?> map = (Map<?, ?>) value;
			final HashMap<String, omero.RType> val = new HashMap<>();
			for (final Object key : map.keySet()) {
				val.put(key.toString(), rtype(map.get(key)));
			}
			return omero.rtypes.rmap(val);
		}
		if (value instanceof Set) {
			final Set<?> set = (Set<?>) value;
			final omero.RType[] val = new omero.RType[set.size()];
			int index = 0;
			for (final Object element : set) {
				val[index++] = rtype(element);
			}
			return omero.rtypes.rset(val);
		}

		// try generic OMEROification routine
		try {
			return omero.rtypes.rtype(value);
		}
		catch (final omero.ClientError err) {
			// default case: convert to string
			return omero.rtypes.rstring(value.toString());
		}
	}

	/**
	 * Gets the host associated with the given OMERO client.
	 *
	 * @throws IllegalArgumentException if the client has no associated host.
	 */
	public static String host(omero.client client) {
		final String host = client.getProperty("omero.host");
		if (host != null && !host.isEmpty()) return host;
		final String router = client.getProperty("Ice.Default.Router");
		final int index = router.indexOf("-h ");
		if (index < 0) throw new IllegalArgumentException("No host for client");
		return router.substring(index + 3, router.length());
	}

	/**
	 * Gets the port associated with the given OMERO client.
	 *
	 * @throws IllegalArgumentException if the client has no associated port.
	 */
	public static int port(omero.client client) {
		final String port = client.getProperty("omero.port");
		if (port == null || port.isEmpty()) {
			throw new IllegalArgumentException("No port for client");
		}
		try {
			return Integer.parseInt(port);
		}
		catch (final NumberFormatException exc) {
			throw new IllegalArgumentException("Invalid port for client: " + port, exc);
		}
	}

	/**
	 * Performs the given operation that might throw OMERO-related exceptions.
	 * 
	 * @return The result of the execution.
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public static <T> T ask(final Callable<T> c) throws OMEROException {
		try {
			return c.call();
		}
		catch (final Exception exc) {
			throw new OMEROException(exc);
		}
	}

	/**
	 * Performs the given operation that might throw OMERO-related exceptions.
	 * 
	 * @throws OMEROException if something goes wrong with OMERO.
	 */
	public static void tell(final VoidCallable c) throws OMEROException {
		try {
			c.call();
		}
		catch (final Exception exc) {
			throw new OMEROException(exc);
		}
	}

	/** {@link Callable}-like interface, but which returns {@code void}. */
	public interface VoidCallable {
		void call() throws Exception;
	}
}
