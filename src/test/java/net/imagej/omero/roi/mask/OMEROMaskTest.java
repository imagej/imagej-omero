/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2016 Open Microscopy Environment:
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

package net.imagej.omero.roi.mask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.roi.mask.DefaultOMEROMask;
import net.imagej.omero.roi.mask.OMEROMask;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;

import org.junit.BeforeClass;
import org.junit.Test;

import omero.gateway.model.MaskData;

/**
 * Tests {@link OMEROMask} and {@link DefaultOMEROMask}.
 * <p>
 * The behavior of {@link OMEROMask}s when {@link MaskData} has {@code double}
 * width, height, x, or y position is not defined.
 * </p>
 *
 * @author Alison Walter
 */
public class OMEROMaskTest {

	private static MaskData md;
	private static OMEROMask om;

	@BeforeClass
	public static void setup() {
		// 1 0 0 0 1 1
		// 1 0 0 0 0 0
		// 0 0 1 0 0 0
		// 0 0 0 0 1 1
		final byte[] data = new byte[] { -114, 2, 3 };
		md = new MaskData(12, 34, 6, 4, data);
		om = new DefaultOMEROMask(md);
	}

	@Test
	public void testTest() {
		final RealPoint test = new RealPoint(new double[] { 12, 34 });
		assertTrue(om.test(test));

		// First element
		test.setPosition(new double[] { 12, 34 });
		assertTrue(om.test(test));

		// Rounds to the first
		test.setPosition(new double[] { 11.75, 34 });
		assertTrue(om.test(test));

		// Second to last position
		test.setPosition(new double[] { 17, 37 });
		assertTrue(om.test(test));

		test.setPosition(new double[] { 17.25, 37 });
		assertTrue(om.test(test));

		// Rounds to the last position
		test.setPosition(new double[] { 17, 37.5 });
		assertFalse(om.test(test));

		// Last position
		test.setPosition(new double[] { 18, 38 });
		assertFalse(om.test(test));

		test.setPosition(new double[] { 14, 36 });
		assertTrue(om.test(test));

		test.setPosition(new double[] { 14.25, 36.1 });
		assertTrue(om.test(test));

		test.setPosition(new double[] { 13, 0 });
		assertFalse(om.test(test));
	}

	@Test
	public void testBoundaryType() {
		assertEquals(BoundaryType.UNSPECIFIED, om.boundaryType());
	}

	@Test
	public void testMinMax() {
		assertEquals(2, om.numDimensions());

		assertEquals(md.getX(), om.realMin(0), 0);
		assertEquals(md.getY(), om.realMin(1), 0);
		assertEquals(md.getX() + md.getWidth(), om.realMax(0), 0);
		assertEquals(md.getY() + md.getHeight(), om.realMax(1), 0);
	}
}
