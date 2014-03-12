/*
 * #%L
 * Server- and client-side communication between ImageJ and OMERO.
 * %%
 * Copyright (C) 2013 - 2014 Board of Regents of the University of
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

package imagej.omero;

import imagej.Identifiable;
import imagej.ImageJ;
import imagej.module.Module;
import imagej.module.ModuleInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.MenuPath;
import org.scijava.UIDetails;

/**
 * Generates Jython stubs for running ImageJ {@link Module}s as OMERO scripts.
 * 
 * @author Curtis Rueden
 * @see "https://www.openmicroscopy.org/site/support/omero4/developers/Modules/Scripts.html"
 */
public class ScriptGenerator extends AbstractContextual {

	// -- Instance fields --

	/** The ImageJ application gateway. */
	private final ImageJ ij;

	// -- Constructors --

	public ScriptGenerator() {
		this(new ImageJ());
	}

	public ScriptGenerator(final Context context) {
		this(new ImageJ(context));
	}

	public ScriptGenerator(final ImageJ ij) {
		this.ij = ij;
		setContext(ij.getContext());
	}

	// -- ScriptRunner methods --

	/** Generates OMERO script stubs for all available ImageJ modules. */
	public void generateAll(final File dir, final boolean headlessOnly)
		throws IOException
	{
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Invalid directory: " + dir);
		}
		for (final ModuleInfo info : ij.module().getModules()) {
			if (isValid(info, headlessOnly)) generate(info, dir);
		}
	}

	/** Generates an OMERO script stub for the given ImageJ module. */
	public void generate(final ModuleInfo info, final File dir)
		throws IOException
	{
		// validate arguments
		if (!(info instanceof Identifiable)) {
			throw new IllegalArgumentException("Unidentifiable module: " + info);
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Invalid directory: " + dir);
		}

		// sanitize identifier
		final String id = ((Identifiable) info).getIdentifier();
		final String escapedID = id.replaceAll("\n", "\\\\n");

		// write the stub
		final String filename = formatFilename(info);
		final File stubFile = new File(dir, filename);
		final PrintWriter out = new PrintWriter(new FileWriter(stubFile));
		out.println("#!/usr/bin/env jython");
		out.println("import imagej.omero.ScriptRunner, sys");
		out.println("id = \"" + escapedID + "\"");
		out.println("imagej.omero.ScriptRunner.main(id)");
		out.close();
	}

	// -- Main method --

	/** Entry point for generating OMERO script stubs. */
	public static void main(final String... args) throws Exception {
		// parse arguments
		boolean headlessOnly = true;
		File dir = null;
		for (final String arg : args) {
			if ("--all".equals(arg)) headlessOnly = false;
			else dir = new File(arg);
		}

		System.err.println(new Date() + ": generating scripts");

		// NB: Make ImageJ startup less verbose.
		System.setProperty("scijava.log.level", "warn");

		// generate script stubs
		final ScriptGenerator scriptGenerator = new ScriptGenerator();
		scriptGenerator.generateAll(dir, headlessOnly);

		// clean up resources
		scriptGenerator.getContext().dispose();

		System.err.println(new Date() + ": generation completed");

		// shut down the JVM, "just in case"
		System.exit(0);
	}

	// -- Helper methods --

	private String formatFilename(final ModuleInfo info) {
		final MenuPath menuPath = info.getMenuPath();

		String s;
		if (menuPath == null || menuPath.isEmpty()) s = info.getTitle();
		else s = menuPath.getMenuString();

		// replace undesirable characters (space, slash and backslash)
		s = s.replaceAll("[ /\\\\]", "_");

		// remove ellipsis if present
		if (s.endsWith("...")) s = s.substring(0, s.length() - 3);

		// add Jython file extension
		s = s + ".jy";

		return s;
	}

	private boolean isValid(final ModuleInfo info, final boolean headlessOnly) {
		if (!(info instanceof Identifiable)) return false;
		if (headlessOnly && !info.canRunHeadless()) return false;
		if (!UIDetails.APPLICATION_MENU_ROOT.equals(info.getMenuRoot())) return false;
		return true;
	}

}
