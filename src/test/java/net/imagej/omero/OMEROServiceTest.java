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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.module.AbstractModuleItem;
import org.scijava.module.ModuleItem;
import org.scijava.util.ColorRGB;
import org.scijava.util.MersenneTwisterFast;

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

		assertParam(RBool.class, boolean.class);
		assertParam(RString.class, byte.class);
		assertParam(RString.class, char.class);
		assertParam(RDouble.class, double.class);
		assertParam(RFloat.class, float.class);
		assertParam(RInt.class, int.class);
		assertParam(RLong.class, long.class);
		assertParam(RString.class, short.class);

		// -- test primitive object wrappers --

		assertParam(RBool.class, Boolean.class);
		assertParam(RString.class, Byte.class);
		assertParam(RString.class, Character.class);
		assertParam(RDouble.class, Double.class);
		assertParam(RFloat.class, Float.class);
		assertParam(RInt.class, Integer.class);
		assertParam(RLong.class, Long.class);
		assertParam(RString.class, Short.class);

		// -- test array types --

		assertParam(RArray.class, boolean[].class);
		assertParam(RArray.class, byte[].class);
		assertParam(RArray.class, char[].class);
		assertParam(RArray.class, double[].class);
		assertParam(RArray.class, float[].class);
		assertParam(RArray.class, int[].class);
		assertParam(RArray.class, long[].class);
		assertParam(RArray.class, short[].class);
		assertParam(RArray.class, Object[].class);
		assertParam(RArray.class, String[].class);

		// -- test collection types --

		assertParam(RList.class, List.class);
		assertParam(RMap.class, Map.class);
		assertParam(RSet.class, Set.class);

		assertParam(RList.class, ArrayList.class);
		assertParam(RMap.class, HashMap.class);
		assertParam(RSet.class, HashSet.class);

		// -- test ImageJ image types --

		assertParam(RLong.class, Dataset.class);
		assertParam(RLong.class, DatasetView.class);
		assertParam(RLong.class, ImageDisplay.class);

		// -- test other object types --

		assertParam(RString.class, BigDecimal.class);
		assertParam(RString.class, BigInteger.class);
		assertParam(RString.class, ColorRGB.class);
		assertParam(RString.class, File.class);
	}

	/** Tests {@link OMEROService#prototype(Class)}. */
	@Test
	public void testPrototype() {
		// -- test primitive types --

		assertPrototype(RBool.class, boolean.class);
		assertPrototype(RString.class, byte.class);
		assertPrototype(RString.class, char.class);
		assertPrototype(RDouble.class, double.class);
		assertPrototype(RFloat.class, float.class);
		assertPrototype(RInt.class, int.class);
		assertPrototype(RLong.class, long.class);
		assertPrototype(RString.class, short.class);

		// -- test primitive object wrappers --

		assertPrototype(RBool.class, Boolean.class);
		assertPrototype(RString.class, Byte.class);
		assertPrototype(RString.class, Character.class);
		assertPrototype(RDouble.class, Double.class);
		assertPrototype(RFloat.class, Float.class);
		assertPrototype(RInt.class, Integer.class);
		assertPrototype(RLong.class, Long.class);
		assertPrototype(RString.class, Short.class);

		// -- test array types --

		assertPrototype(RArray.class, boolean[].class);
		assertPrototype(RArray.class, byte[].class);
		assertPrototype(RArray.class, char[].class);
		assertPrototype(RArray.class, double[].class);
		assertPrototype(RArray.class, float[].class);
		assertPrototype(RArray.class, int[].class);
		assertPrototype(RArray.class, long[].class);
		assertPrototype(RArray.class, short[].class);
		assertPrototype(RArray.class, Object[].class);
		assertPrototype(RArray.class, String[].class);

		// -- test collection types --

		assertPrototype(RList.class, List.class);
		assertPrototype(RMap.class, Map.class);
		assertPrototype(RSet.class, Set.class);

		assertPrototype(RList.class, ArrayList.class);
		assertPrototype(RMap.class, HashMap.class);
		assertPrototype(RSet.class, HashSet.class);

		// -- test ImageJ image types --

		assertPrototype(RLong.class, Dataset.class);
		assertPrototype(RLong.class, DatasetView.class);
		assertPrototype(RLong.class, ImageDisplay.class);

		// -- test other object types --

		assertPrototype(RString.class, BigDecimal.class);
		assertPrototype(RString.class, BigInteger.class);
		assertPrototype(RString.class, ColorRGB.class);
		assertPrototype(RString.class, File.class);
	}

	// -- Helper methods --

	private <T> ModuleItem<T> createItem(final Class<T> type) {
		return new TestModuleItem<>(type);
	}

	private <T> void assertParam(final Class<?> omero, final Class<T> pojo) {
		final ModuleItem<T> item = createItem(pojo);
		final Param param = omeroService.getJobParam(item);
		assertEquals(!item.isRequired(), param.optional);
		assertInstance(omero, param.prototype);
		assertEquals(item.getDescription(), param.description);
	}

	private void assertPrototype(final Class<?> omero, final Class<?> pojo) {
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
