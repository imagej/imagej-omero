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

import net.imagej.omero.commands.OpenImageFromOMERO;

import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.ui.UIService;

/**
 * Manual test for {@link OpenImageFromOMERO}.
 * 
 * @author Curtis Rueden
 */
public final class Main {

	private Main() {
		// prevent instantiation of utility class
	}

	public static void main(final String... args) {
		final Context c = new Context();
		c.service(UIService.class).showUI();
		c.service(CommandService.class).run(OpenImageFromOMERO.class, true);
	}

}
