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

package net.imagej.omero.roi.rectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.roi.rectangle.ClosedOMERORectangle;
import net.imagej.omero.roi.rectangle.OMERORectangle;
import net.imagej.omero.roi.rectangle.OpenOMERORectangle;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.roi.BoundaryType;

import org.junit.Before;
import org.junit.Test;

import omero.gateway.model.RectangleData;

/**
 * Tests {@link OMERORectangle}, {@link ClosedOMERORectangle}, and
 * {@link OpenOMERORectangle}.
 *
 * @author Alison Walter
 */
public class OMERORectangleTest {

	private RectangleData rd;
	private OMERORectangle open;
	private OMERORectangle closed;
	private RealPoint inside;
	private RealPoint outside;
	private RealPoint boundary;

	@Before
	public void setup() {
		rd = new RectangleData(92, 8, 18.5, 23);
		open = new OpenOMERORectangle(rd);
		closed = new ClosedOMERORectangle(rd);

		inside = new RealPoint(new double[] { 110.25, 17 });
		outside = new RealPoint(new double[] { 91, 7 });
		boundary = new RealPoint(new double[] { 97, 31 });
	}

	// -- OMERORectangle --

	@Test
	public void testMinMax() {
		assertEquals(rd.getX(), open.realMin(0), 0);
		assertEquals(rd.getY(), open.realMin(1), 0);
		assertEquals(rd.getX() + rd.getWidth(), open.realMax(0), 0);
		assertEquals(rd.getY() + rd.getHeight(), open.realMax(1), 0);
	}

	@Test
	public void testSideLength() {
		assertEquals(rd.getWidth(), closed.sideLength(0), 0);
		assertEquals(rd.getHeight(), open.sideLength(1), 0);
	}

	@Test
	public void testSetSideLength() {
		// setting side length is relative to the center, i.e. plus/minus 15
		// not simply setting the width
		closed.setSideLength(0, 30);

		// Check wrapper
		assertEquals(86.25, closed.realMin(0), 0);
		assertEquals(8, closed.realMin(1), 0);
		assertEquals(116.25, closed.realMax(0), 0);
		assertEquals(31, closed.realMax(1), 0);

		// Check RectangleData
		assertEquals(30, rd.getWidth(), 0);
		assertEquals(86.25, rd.getX(), 0);
		assertEquals(23, rd.getHeight(), 0);
		assertEquals(8, rd.getY(), 0);
	}

	// -- ClosedOMERORectangle --

	@Test
	public void testTestClosed() {
		assertTrue(closed.test(inside));
		assertFalse(closed.test(outside));
		assertTrue(closed.test(boundary));
	}

	@Test
	public void testBoundaryTypeClosed() {
		assertEquals(BoundaryType.CLOSED, closed.boundaryType());
	}

	@Test
	public void testGetCenterClosed() {
		assertEquals(101.25, closed.center().getDoublePosition(0), 0);
		assertEquals(19.5, closed.center().getDoublePosition(1), 0);
	}

	@Test
	public void testSetCenterClosed() {
		final RealPoint inside2 = new RealPoint(new double[] { 93.5, 11 });
		final RealPoint outside2 = new RealPoint(new double[] { 112, 52 });
		assertTrue(closed.test(inside2));
		assertFalse(closed.test(outside2));

		final RealPositionable c = closed.center();
		c.setPosition(new double[] { 105, 55.5 });

		// check wrapper
		assertFalse(closed.test(inside2));
		assertTrue(closed.test(outside2));

		assertEquals(95.75, closed.realMin(0), 0);
		assertEquals(44, closed.realMin(1), 0);
		assertEquals(114.25, closed.realMax(0), 0);
		assertEquals(67, closed.realMax(1), 0);

		// check RectangleData
		assertEquals(18.5, rd.getWidth(), 0);
		assertEquals(95.75, rd.getX(), 0);
		assertEquals(23, rd.getHeight(), 0);
		assertEquals(44, rd.getY(), 0);
	}

	// -- OpenOMERORectangle --

	@Test
	public void testTestOpen() {
		assertTrue(open.test(inside));
		assertFalse(open.test(outside));
		assertFalse(open.test(boundary));
	}

	@Test
	public void testBoundaryTypeOpen() {
		assertEquals(BoundaryType.OPEN, open.boundaryType());
	}

	@Test
	public void testGetCenterOpen() {
		assertEquals(101.25, open.center().getDoublePosition(0), 0);
		assertEquals(19.5, open.center().getDoublePosition(1), 0);
	}

	@Test
	public void testSetCenterOpen() {
		final RealPoint inside2 = new RealPoint(new double[] { 100, 9.5 });
		final RealPoint outside2 = new RealPoint(new double[] { 1, 230 });
		assertTrue(open.test(inside2));
		assertFalse(open.test(outside2));

		final RealPositionable c = open.center();
		c.setPosition(new double[] { 10, 238.25 });

		// check wrapper
		assertFalse(open.test(inside2));
		assertTrue(open.test(outside2));

		assertEquals(0.75, open.realMin(0), 0);
		assertEquals(226.75, open.realMin(1), 0);
		assertEquals(19.25, open.realMax(0), 0);
		assertEquals(249.75, open.realMax(1), 0);

		// check RectangleData
		assertEquals(18.5, rd.getWidth(), 0);
		assertEquals(0.75, rd.getX(), 0);
		assertEquals(23, rd.getHeight(), 0);
		assertEquals(226.75, rd.getY(), 0);
	}
}
