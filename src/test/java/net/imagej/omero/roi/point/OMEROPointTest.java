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

package net.imagej.omero.roi.point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.roi.point.DefaultOMEROPoint;
import net.imagej.omero.roi.point.OMEROPoint;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;

import org.junit.Before;
import org.junit.Test;

import omero.gateway.model.PointData;

/**
 * Tests for {@link OMEROPoint} and {@link DefaultOMEROPoint}.
 *
 * @author Alison Walter
 */
public class OMEROPointTest {

	private PointData pd;
	private OMEROPoint op;

	@Before
	public void setup() {
		pd = new PointData(4.5, 8.25);
		op = new DefaultOMEROPoint(pd);
	}

	@Test
	public void testTest() {
		final RealPoint rp = new RealPoint(op);
		assertTrue(op.test(rp));

		rp.setPosition(new double[] { 4.25, 8.25 });
		assertFalse(op.test(rp));

		rp.setPosition(new double[] { 10, 42 });
		assertFalse(op.test(rp));
	}

	@Test
	public void testBoundaryType() {
		assertEquals(BoundaryType.CLOSED, op.boundaryType());
	}

	@Test
	public void testMinMax() {
		assertEquals(2, op.numDimensions());

		assertEquals(pd.getX(), op.realMin(0), 0);
		assertEquals(pd.getY(), op.realMin(1), 0);
		assertEquals(pd.getX(), op.realMax(0), 0);
		assertEquals(pd.getY(), op.realMax(1), 0);
	}

	@Test
	public void testSetPosition() {
		final RealPoint testOne = new RealPoint(op);
		final RealPoint testTwo = new RealPoint(new double[] { 13.5, 61 });
		assertTrue(op.test(testOne));
		assertFalse(op.test(testTwo));

		op.setPosition(testTwo);

		// Wrapper
		assertFalse(op.test(testOne));
		assertTrue(op.test(testTwo));

		assertEquals(13.5, op.realMin(0), 0);
		assertEquals(61, op.realMin(1), 0);
		assertEquals(13.5, op.realMax(0), 0);
		assertEquals(61, op.realMax(1), 0);

		// PointData
		assertEquals(13.5, pd.getX(), 0);
		assertEquals(61, pd.getY(), 0);
	}
}
