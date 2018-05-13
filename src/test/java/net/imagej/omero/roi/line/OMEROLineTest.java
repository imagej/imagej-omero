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

package net.imagej.omero.roi.line;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.roi.line.DefaultOMEROLine;
import net.imagej.omero.roi.line.OMEROLine;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import org.junit.Before;
import org.junit.Test;

import omero.gateway.model.LineData;

/**
 * Tests for {@link OMEROLine} and {@link DefaultOMEROLine}.
 *
 * @author Alison Walter
 */
public class OMEROLineTest {

	private LineData ld;
	private OMEROLine ol;

	@Before
	public void setup() {
		ld = new LineData(120, 10, 80, 20);
		ol = new DefaultOMEROLine(ld);
	}

	@Test
	public void testTest() {
		final RealPoint rp = new RealPoint(100, 15);
		assertTrue(ol.test(rp));

		rp.setPosition(new double[] { 120, 10 });
		assertTrue(ol.test(rp));

		rp.setPosition(new double[] { 80, 20 });
		assertTrue(ol.test(rp));

		rp.setPosition(new double[] { 150, 2.5 });
		assertFalse(ol.test(rp));

		rp.setPosition(new double[] { 100.25, 15 });
		assertFalse(ol.test(rp));
	}

	@Test
	public void testBoundaryType() {
		assertEquals(BoundaryType.CLOSED, ol.boundaryType());
	}

	@Test
	public void testMinMax() {
		assertEquals(2, ol.numDimensions());

		assertEquals(ld.getX2(), ol.realMin(0), 0);
		assertEquals(ld.getY1(), ol.realMin(1), 0);

		assertEquals(ld.getX1(), ol.realMax(0), 0);
		assertEquals(ld.getY2(), ol.realMax(1), 0);
	}

	@Test
	public void testGetEndPointOne() {
		assertEquals(ld.getX1(), ol.endpointOne().getDoublePosition(0), 0);
		assertEquals(ld.getY1(), ol.endpointOne().getDoublePosition(1), 0);
	}

	@Test
	public void testGetEndPointTwo() {
		assertEquals(ld.getX2(), ol.endpointTwo().getDoublePosition(0), 0);
		assertEquals(ld.getY2(), ol.endpointTwo().getDoublePosition(1), 0);
	}

	@Test
	public void testSetEndPointOne() {
		final RealPoint rpOne = new RealPoint(new double[] { 100, 15 });
		final RealPoint rpTwo = new RealPoint(new double[] { 100, 40 });
		assertTrue(ol.test(rpOne));
		assertFalse(ol.test(rpTwo));

		final RealLocalizableRealPositionable ep = ol.endpointOne();
		ep.setPosition(new double[] { 180, 120 });

		// Wrapper
		assertEquals(180, ol.endpointOne().getDoublePosition(0), 0);
		assertEquals(120, ol.endpointOne().getDoublePosition(1), 0);
		assertFalse(ol.test(rpOne));
		assertTrue(ol.test(rpTwo));
		assertEquals(80, ol.realMin(0), 0);
		assertEquals(20, ol.realMin(1), 0);
		assertEquals(180, ol.realMax(0), 0);
		assertEquals(120, ol.realMax(1), 0);

		// Underlying LineData
		assertEquals(180, ld.getX1(), 0);
		assertEquals(120, ld.getY1(), 0);
		assertEquals(80, ld.getX2(), 0);
		assertEquals(20, ld.getY2(), 0);
	}

	@Test
	public void testSetEndPointTwo() {
		final RealPoint rpOne = new RealPoint(new double[] { 95, 16.25 });
		final RealPoint rpTwo = new RealPoint(new double[] { 160, 10 });
		assertTrue(ol.test(rpOne));
		assertFalse(ol.test(rpTwo));

		final RealLocalizableRealPositionable ep = ol.endpointTwo();
		ep.setPosition(new double[] { 200, 10 });

		// Wrapper
		assertEquals(200, ol.endpointTwo().getDoublePosition(0), 0);
		assertEquals(10, ol.endpointTwo().getDoublePosition(1), 0);
		assertFalse(ol.test(rpOne));
		assertTrue(ol.test(rpTwo));
		assertEquals(120, ol.realMin(0), 0);
		assertEquals(10, ol.realMin(1), 0);
		assertEquals(200, ol.realMax(0), 0);
		assertEquals(10, ol.realMax(1), 0);

		// Underlying LineData
		assertEquals(120, ld.getX1(), 0);
		assertEquals(10, ld.getY1(), 0);
		assertEquals(200, ld.getX2(), 0);
		assertEquals(10, ld.getY2(), 0);
	}
}
