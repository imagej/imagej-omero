/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2022 Open Microscopy Environment:
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

import java.net.URI;

import org.scijava.io.location.AbstractLocationResolver;
import org.scijava.io.location.Location;
import org.scijava.io.location.LocationResolver;
import org.scijava.plugin.Plugin;

/**
 * Implementation of {@link LocationResolver} for {@link OMEROLocation}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = LocationResolver.class)
public class OMEROLocationResolver extends AbstractLocationResolver {

	public OMEROLocationResolver() {
		super("omero");
	}

	@Override
	public Location resolve(final URI uri) {
		if (!(supports(uri))) throw new IllegalArgumentException(
			"URI not supported: " + uri);
		return new OMEROLocation(uri);
	}
}
