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

import org.scijava.Identifiable;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;

/**
 * Utility class for working with ImageJ modules, particularly
 * {@link Identifiable} ones.
 * 
 * @author Curtis Rueden
 */
public final class ModuleUtils {

	private ModuleUtils() {
		// NB: Prevent instantiation of utility class.
	}

	/** Looks up an {@link Identifiable} ImageJ module by its identifier. */
	public static ModuleInfo findModule(final ModuleService moduleService,
		final String id)
	{
		// TODO: Migrate logic to ModuleService, and cache identifiers in a hash.
		for (final ModuleInfo info : moduleService.getModules()) {
			if (!(info instanceof Identifiable)) continue;
			final String infoID = ((Identifiable) info).getIdentifier();
			if (id.equals(infoID)) return info;
		}
		throw new IllegalArgumentException("No module for ID: " + id);
	}

}
