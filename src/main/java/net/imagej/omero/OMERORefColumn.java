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

import net.imagej.table.LongColumn;

import omero.gateway.facility.TablesFacility;

/**
 * Wrapper for OMERO reference columns (i.e. FileColumn, ImageColumn, RoiColumn,
 * etc.). These columns contain long IDs which reference OMERO Data objects.
 * <p>
 * Note, when the {@link TablesFacility} gets a Table it does create DataObjects
 * but does not actually load them.
 * </p>
 *
 * @author Alison Walter
 */
public class OMERORefColumn extends LongColumn {

	private final OMERORef ref;
	private Object[] originalData;

	public OMERORefColumn(final OMERORef referenceType) {
		super();
		ref = referenceType;
	}

	public OMERORefColumn(final String header, final OMERORef referenceType) {
		super(header);
		ref = referenceType;
	}

	public OMERORef getOMERORef() {
		return ref;
	}

	public Object[] getOriginalData() {
		return originalData;
	}

	public void setOriginalData(final Object[] data) {
		originalData = data;
	}

}
