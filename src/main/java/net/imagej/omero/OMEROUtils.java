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

import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.quantity.Time;
import ome.units.unit.Unit;
import omero.model.enums.UnitsLength;
import omero.model.enums.UnitsTime;

/**
 * Utility class for working with OMERO data structures.
 * 
 * @author Curtis Rueden
 */
public final class OMEROUtils {

	private OMEROUtils() {
		// NB: Prevent instantiation of utility class.
	}

	/**
	 * Converts a {@link UnitsTime} object into a {@link Unit}{@code <Length>}.
	 */
	public static Unit<Length> unit(final UnitsLength unit) {
		switch (unit) {
			case ANGSTROM:
				return UNITS.ANGSTROM;
			case ASTRONOMICALUNIT:
				return UNITS.UA;
			case ATTOMETER:
				return UNITS.AM;
			case CENTIMETER:
				return UNITS.CM;
			case DECAMETER:
				return UNITS.DAM;
			case DECIMETER:
				return UNITS.DM;
			case EXAMETER:
				return UNITS.EXAM;
			case FEMTOMETER:
				return UNITS.FM;
			case FOOT:
				return UNITS.FT;
			case GIGAMETER:
				return UNITS.GIGAM;
			case HECTOMETER:
				return UNITS.HM;
			case INCH:
				return UNITS.INCH;
			case KILOMETER:
				return UNITS.KM;
			case LIGHTYEAR:
				return UNITS.LY;
			case LINE:
				return UNITS.LI;
			case MEGAMETER:
				return UNITS.MEGAM;
			case METER:
				return UNITS.M;
			case MICROMETER:
				return UNITS.MICROM;
			case MILE:
				return UNITS.MI;
			case MILLIMETER:
				return UNITS.MM;
			case NANOMETER:
				return UNITS.NM;
			case PARSEC:
				return UNITS.PC;
			case PETAMETER:
				return UNITS.PETAM;
			case PICOMETER:
				return UNITS.PM;
			case PIXEL:
				return UNITS.PIXEL;
			case POINT:
				return UNITS.PT;
			case REFERENCEFRAME:
				return UNITS.REFERENCEFRAME;
			case TERAMETER:
				return UNITS.TERAM;
			case THOU:
				return UNITS.THOU;
			case YARD:
				return UNITS.YD;
			case YOCTOMETER:
				return UNITS.YM;
			case YOTTAMETER:
				return UNITS.YOTTAM;
			case ZEPTOMETER:
				return UNITS.ZM;
			case ZETTAMETER:
				return UNITS.ZETTAM;
			default:
				throw new IllegalArgumentException("Unknown unit: " + unit);
		}
	}

	/** Converts a {@link UnitsTime} object into a {@link Unit}{@code <Time>}. */
	public static Unit<Time> unit(final UnitsTime unit) {
		switch (unit) {
			case ATTOSECOND:
				return UNITS.AS;
			case CENTISECOND:
				return UNITS.CS;
			case DAY:
				return UNITS.D;
			case DECASECOND:
				return UNITS.DAS;
			case DECISECOND:
				return UNITS.DS;
			case EXASECOND:
				return UNITS.EXAS;
			case FEMTOSECOND:
				return UNITS.FS;
			case GIGASECOND:
				return UNITS.GIGAS;
			case HECTOSECOND:
				return UNITS.HS;
			case HOUR:
				return UNITS.H;
			case KILOSECOND:
				return UNITS.KS;
			case MEGASECOND:
				return UNITS.MEGAS;
			case MICROSECOND:
				return UNITS.MICROS;
			case MILLISECOND:
				return UNITS.MS;
			case MINUTE:
				return UNITS.MIN;
			case NANOSECOND:
				return UNITS.NS;
			case PETASECOND:
				return UNITS.PETAS;
			case PICOSECOND:
				return UNITS.PS;
			case SECOND:
				return UNITS.S;
			case TERASECOND:
				return UNITS.TERAS;
			case YOCTOSECOND:
				return UNITS.YS;
			case YOTTASECOND:
				return UNITS.YOTTAS;
			case ZEPTOSECOND:
				return UNITS.ZS;
			case ZETTASECOND:
				return UNITS.ZETTAS;
			default:
				throw new IllegalArgumentException("Unknown unit: " + unit);
		}
	}

}
