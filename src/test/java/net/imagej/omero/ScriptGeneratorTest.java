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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.menu.MenuService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginIndex;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;
import org.scijava.test.TestUtils;
import org.scijava.util.FileUtils;

/**
 * Tests {@link ScriptGenerator}.
 * 
 * @author Curtis Rueden
 */
public class ScriptGeneratorTest {

	@Test
	public void testGenerateAll() throws IOException {
		// create a context with a minimal command set
		final PluginIndex pluginIndex = new PluginIndex() {
			@Override
			public void discover() {
				super.discover();
				removeAll(getPlugins(Command.class));
				add(pluginInfo(FileNew.class));
				add(pluginInfo(FileOpen.class));
				add(pluginInfo(FileSave.class));
				add(pluginInfo(FileExit.class));
				add(pluginInfo(Lion.class));
				add(pluginInfo(Tiger.class));
				add(pluginInfo(Bear.class));
			}
		};
		final ArrayList<Class<? extends Service>> classes =
			new ArrayList<Class<? extends Service>>();
		classes.add(AppService.class);
		classes.add(CommandService.class);
		classes.add(MenuService.class);
		final Context context = new Context(classes, pluginIndex);
		final ScriptGenerator scriptGen = new ScriptGenerator(context);
		final File tempDir =
			TestUtils.createTemporaryDirectory("script-generator-");
		final File libDir = new File(tempDir, "lib");
		final File scriptsDir = new File(libDir, "scripts");
		assertTrue(scriptsDir.mkdirs());
		final int returnCode = scriptGen.generateAll(tempDir);
		context.dispose();

		assertEquals(0, returnCode);
		final File imagejDir = new File(scriptsDir, "imagej");
		assertTrue(imagejDir.isDirectory());
		final File fileDir = new File(imagejDir, "File");
		assertTrue(fileDir.isDirectory());
		final File animalsDir = new File(imagejDir, "Animals");
		assertTrue(animalsDir.isDirectory());
		assertTrue(new File(fileDir, "New.py").exists());
		assertTrue(new File(fileDir, "Open.py").exists());
		assertTrue(new File(fileDir, "Save.py").exists());
		assertTrue(new File(fileDir, "Exit.py").exists());
		assertTrue(new File(animalsDir, "Lion.py").exists());
		assertTrue(new File(animalsDir, "Tiger.py").exists());
		assertTrue(new File(animalsDir, "Bear.py").exists());
		FileUtils.deleteRecursively(tempDir);
	}

	// -- Helper methods --

	private PluginInfo<Command> pluginInfo(
		final Class<? extends Command> pluginClass)
	{
		final Plugin ann = pluginClass.getAnnotation(Plugin.class);
		return new PluginInfo<Command>(pluginClass, Command.class, ann);
	}

	// -- Helper classes --

	@Plugin(type = Command.class, menu = { @Menu(label = "File", weight = 0),
		@Menu(label = "New", weight = 0) }, headless = true)
	public static class FileNew extends DummyCommand {
		//
	}

	@Plugin(type = Command.class, menu = { @Menu(label = "File", weight = 0),
		@Menu(label = "Open", weight = 1) }, headless = true)
	public static class FileOpen extends DummyCommand {
		//
	}

	@Plugin(type = Command.class, menu = { @Menu(label = "File", weight = 0),
		@Menu(label = "Save", weight = 2) }, headless = true)
	public static class FileSave extends DummyCommand {
		//
	}

	@Plugin(type = Command.class, menu = { @Menu(label = "File", weight = 0),
		@Menu(label = "Exit", weight = 3) }, headless = true)
	public static class FileExit extends DummyCommand {
		//
	}

	@Plugin(type = Command.class, menu = { @Menu(label = "Animals", weight = 10),
		@Menu(label = "Lion", weight = 250) }, headless = true)
	public static class Lion extends DummyCommand {
		//
	}

	@Plugin(type = Command.class, menu = { @Menu(label = "Animals", weight = 10),
		@Menu(label = "Tiger", weight = 310) }, headless = true)
	public static class Tiger extends DummyCommand {
		//
	}

	@Plugin(type = Command.class, menu = { @Menu(label = "Animals", weight = 10),
		@Menu(label = "Bear", weight = 360) }, headless = true)
	public static class Bear extends DummyCommand {
		//
	}

	public static class DummyCommand implements Command {
		@Override
		public void run() {
			//
		}
	}

}
