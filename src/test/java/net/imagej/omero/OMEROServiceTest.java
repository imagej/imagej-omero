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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import omero.RArray;
import omero.RBool;
import omero.RDouble;
import omero.RFloat;
import omero.RInt;
import omero.RList;
import omero.RLong;
import omero.RMap;
import omero.RSet;
import omero.RString;
import omero.RType;
import omero.grid.Param;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.module.AbstractModuleItem;
import org.scijava.module.ModuleItem;
import org.scijava.util.ColorRGB;
import org.scijava.util.MersenneTwisterFast;

/**
 * Tests {@link DefaultOMEROService}.
 * 
 * @author Curtis Rueden
 */
public class OMEROServiceTest {

	private OMEROService omeroService;

	@Before
	public void setUp() {
		omeroService = new Context(OMEROService.class).service(OMEROService.class);
	}

	@After
	public void tearDown() {
		if (omeroService != null) omeroService.getContext().dispose();
	}

	/** Tests {@link OMEROService#getJobParam(org.scijava.module.ModuleItem)}. */
	@Test
	public void testGetJobParam() {
		// -- test primitive types --

		assertParam(omeroService, RBool.class, boolean.class);
		assertParam(omeroService, RString.class, byte.class);
		assertParam(omeroService, RString.class, char.class);
		assertParam(omeroService, RDouble.class, double.class);
		assertParam(omeroService, RFloat.class, float.class);
		assertParam(omeroService, RInt.class, int.class);
		assertParam(omeroService, RLong.class, long.class);
		assertParam(omeroService, RString.class, short.class);

		// -- test primitive object wrappers --

		assertParam(omeroService, RBool.class, Boolean.class);
		assertParam(omeroService, RString.class, Byte.class);
		assertParam(omeroService, RString.class, Character.class);
		assertParam(omeroService, RDouble.class, Double.class);
		assertParam(omeroService, RFloat.class, Float.class);
		assertParam(omeroService, RInt.class, Integer.class);
		assertParam(omeroService, RLong.class, Long.class);
		assertParam(omeroService, RString.class, Short.class);

		// -- test array types --

		assertParam(omeroService, RArray.class, boolean[].class);
		assertParam(omeroService, RArray.class, byte[].class);
		assertParam(omeroService, RArray.class, char[].class);
		assertParam(omeroService, RArray.class, double[].class);
		assertParam(omeroService, RArray.class, float[].class);
		assertParam(omeroService, RArray.class, int[].class);
		assertParam(omeroService, RArray.class, long[].class);
		assertParam(omeroService, RArray.class, short[].class);
		assertParam(omeroService, RArray.class, Object[].class);
		assertParam(omeroService, RArray.class, String[].class);

		// -- test collection types --

		assertParam(omeroService, RList.class, List.class);
		assertParam(omeroService, RMap.class, Map.class);
		assertParam(omeroService, RSet.class, Set.class);

		assertParam(omeroService, RList.class, ArrayList.class);
		assertParam(omeroService, RMap.class, HashMap.class);
		assertParam(omeroService, RSet.class, HashSet.class);

		// -- test ImageJ image types --

		assertParam(omeroService, RLong.class, Dataset.class);
		assertParam(omeroService, RLong.class, DatasetView.class);
		assertParam(omeroService, RLong.class, ImageDisplay.class);

		// -- test other object types --

		assertParam(omeroService, RString.class, BigDecimal.class);
		assertParam(omeroService, RString.class, BigInteger.class);
		assertParam(omeroService, RString.class, ColorRGB.class);
		assertParam(omeroService, RString.class, File.class);
	}

	/** Tests {@link OMEROService#prototype(Class)}. */
	@Test
	public void testPrototype() {
		// -- test primitive types --

		assertPrototype(omeroService, RBool.class, boolean.class);
		assertPrototype(omeroService, RString.class, byte.class);
		assertPrototype(omeroService, RString.class, char.class);
		assertPrototype(omeroService, RDouble.class, double.class);
		assertPrototype(omeroService, RFloat.class, float.class);
		assertPrototype(omeroService, RInt.class, int.class);
		assertPrototype(omeroService, RLong.class, long.class);
		assertPrototype(omeroService, RString.class, short.class);

		// -- test primitive object wrappers --

		assertPrototype(omeroService, RBool.class, Boolean.class);
		assertPrototype(omeroService, RString.class, Byte.class);
		assertPrototype(omeroService, RString.class, Character.class);
		assertPrototype(omeroService, RDouble.class, Double.class);
		assertPrototype(omeroService, RFloat.class, Float.class);
		assertPrototype(omeroService, RInt.class, Integer.class);
		assertPrototype(omeroService, RLong.class, Long.class);
		assertPrototype(omeroService, RString.class, Short.class);

		// -- test array types --

		assertPrototype(omeroService, RArray.class, boolean[].class);
		assertPrototype(omeroService, RArray.class, byte[].class);
		assertPrototype(omeroService, RArray.class, char[].class);
		assertPrototype(omeroService, RArray.class, double[].class);
		assertPrototype(omeroService, RArray.class, float[].class);
		assertPrototype(omeroService, RArray.class, int[].class);
		assertPrototype(omeroService, RArray.class, long[].class);
		assertPrototype(omeroService, RArray.class, short[].class);
		assertPrototype(omeroService, RArray.class, Object[].class);
		assertPrototype(omeroService, RArray.class, String[].class);

		// -- test collection types --

		assertPrototype(omeroService, RList.class, List.class);
		assertPrototype(omeroService, RMap.class, Map.class);
		assertPrototype(omeroService, RSet.class, Set.class);

		assertPrototype(omeroService, RList.class, ArrayList.class);
		assertPrototype(omeroService, RMap.class, HashMap.class);
		assertPrototype(omeroService, RSet.class, HashSet.class);

		// -- test ImageJ image types --

		assertPrototype(omeroService, RLong.class, Dataset.class);
		assertPrototype(omeroService, RLong.class, DatasetView.class);
		assertPrototype(omeroService, RLong.class, ImageDisplay.class);

		// -- test other object types --

		assertPrototype(omeroService, RString.class, BigDecimal.class);
		assertPrototype(omeroService, RString.class, BigInteger.class);
		assertPrototype(omeroService, RString.class, ColorRGB.class);
		assertPrototype(omeroService, RString.class, File.class);
	}

	// -- Helper methods --

	private <T> ModuleItem<T> createItem(final Class<T> type) {
		return new TestModuleItem<T>(type);
	}

	private <T> void assertParam(final OMEROService omeroService,
		final Class<?> omero, final Class<T> pojo)
	{
		final ModuleItem<T> item = createItem(pojo);
		final Param param = omeroService.getJobParam(item);
		assertEquals(!item.isRequired(), param.optional);
		assertInstance(omero, param.prototype);
		assertEquals(item.getDescription(), param.description);
	}

	private void assertPrototype(final OMEROService omeroService,
		final Class<?> omero, final Class<?> pojo)
	{
		final RType proto = omeroService.prototype(pojo);
		assertInstance(omero, proto);
	}

	private void assertInstance(final Class<?> expected, final Object actual) {
		if (expected.isInstance(actual)) return;
		throw new AssertionError("expected:" + expected.getName() + " but was:" +
			actual.getClass().getName());
	}

	// -- Helper classes --

	private static class TestModuleItem<T> extends AbstractModuleItem<T> {

		private final Class<T> type;
		private final boolean required;
		private final String description;

		public TestModuleItem(final Class<T> type) {
			super(null);
			this.type = type;

			final MersenneTwisterFast r = new MersenneTwisterFast();
			required = r.nextBoolean();

			// generate a random description
			final StringBuilder sb = new StringBuilder();
			final int length = r.nextInt(10) + 5;
			for (int i = 0; i < length; i++) {
				sb.append('A' + r.nextInt(26));
			}
			description = sb.toString();
		}

		@Override
		public Class<T> getType() {
			return type;
		}

		@Override
		public boolean isRequired() {
			return required;
		}

		@Override
		public String getDescription() {
			return description;
		}

	}

}
