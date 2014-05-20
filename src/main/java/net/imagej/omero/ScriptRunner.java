/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2014 Board of Regents of the University of
 * Wisconsin-Madison and University of Dundee.
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

import java.util.Date;

import net.imagej.ImageJ;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.ModuleInfo;

/**
 * Executes ImageJ {@link Module}s as OMERO scripts.
 * 
 * @author Curtis Rueden
 * @see "https://www.openmicroscopy.org/site/support/omero4/developers/Modules/Scripts.html"
 */
public class ScriptRunner extends AbstractContextual {

	// -- Instance fields --

	/** The ImageJ application gateway. */
	private final ImageJ ij;

	// -- Constructors --

	public ScriptRunner() {
		this(new ImageJ());
	}

	public ScriptRunner(final Context context) {
		this(new ImageJ(context));
	}

	public ScriptRunner(final ImageJ ij) {
		this.ij = ij;
		setContext(ij.getContext());
	}

	// -- ScriptRunner methods --

	/** Gets the ImageJ application gateway used by this script runner. */
	public ImageJ ij() {
		return ij;
	}

	/** Invokes the given ImageJ module identifier as an OMERO script. */
	public boolean invoke(final String id) {
		// look for a module matching the given identifier
		final ModuleInfo info = ModuleUtils.findModule(ij.module(), id);
		return invoke(info);
	}

	/** Invokes the given ImageJ module as an OMERO script. */
	public boolean invoke(final ModuleInfo info) {
		// initialize OMERO client session
		final omero.client c = new omero.client();

		// initialize module converter
		final ModuleAdapter adaptedModule =
			new ModuleAdapter(ij.getContext(), info, c);

		// perform appropriate action (either parse or launch)
		try {
			c.createSession().detachOnDestroy();
			final String parse = c.getProperty("omero.scripts.parse");
			if (!parse.isEmpty()) adaptedModule.params();
			else adaptedModule.launch();
		}
		catch (final Throwable t) {
			ij.log().error(t);
			return false;
		}
		finally {
			c.__del__();
		}
		return true;
	}

	// -- Main method --

	/** Simple entry point for executing ImageJ modules as scripts. */
	public static void main(final String... args) throws Exception {
		System.out.println(new Date() + ": initializing script runner");

		// NB: Make ImageJ startup less verbose.
		System.setProperty("scijava.log.level", "warn");

		// initialize script runner
		final ScriptRunner scriptRunner = new ScriptRunner();

		// execute modules
		int failed = 0;
		for (final String id : args) {
			System.out.println(new Date() + ": executing: " + id);
			final boolean success = scriptRunner.invoke(id);
			if (!success) failed++;
		}

		// clean up resources
		scriptRunner.getContext().dispose();

		System.out.println(new Date() + ": executions completed");

		// shut down the JVM
		System.exit(failed);
	}

}
