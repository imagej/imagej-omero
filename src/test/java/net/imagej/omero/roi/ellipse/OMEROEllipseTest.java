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

package net.imagej.omero.roi.ellipse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.roi.ellipse.ClosedOMEROEllipse;
import net.imagej.omero.roi.ellipse.OMEROEllipse;
import net.imagej.omero.roi.ellipse.OpenOMEROEllipse;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import org.junit.Before;
import org.junit.Test;

import omero.gateway.model.EllipseData;

/**
 * Tests for {@link OMEROEllipse}, {@link ClosedOMEROEllipse}, and
 * {@link OpenOMEROEllipse}.
 *
 * @author Alison Walter
 */
public class OMEROEllipseTest {

	private EllipseData ed;
	private ClosedOMEROEllipse coe;
	private OpenOMEROEllipse ooe;

	@Before
	public void setup() {
		ed = new EllipseData(110, 62, 45, 20);
		coe = new ClosedOMEROEllipse(ed);
		ooe = new OpenOMEROEllipse(ed);
	}

	// -- OMEROEllipse Tests --

	@Test
	public void testMinMax() {
		assertEquals(ooe.numDimensions(), 2);

		assertEquals(ed.getX() - ed.getRadiusX(), ooe.realMin(0), 0);
		assertEquals(ed.getY() - ed.getRadiusY(), ooe.realMin(1), 0);

		assertEquals(ed.getX() + ed.getRadiusX(), ooe.realMax(0), 0);
		assertEquals(ed.getY() + ed.getRadiusY(), ooe.realMax(1), 0);
	}

	@Test
	public void testGetSemiAxisLength() {
		assertEquals(ed.getRadiusX(), coe.semiAxisLength(0), 0);
		assertEquals(ed.getRadiusY(), coe.semiAxisLength(1), 0);
	}

	@Test
	public void testSetSemiAxisLength() {
		coe.setSemiAxisLength(1, 50);

		// Check IJ Wrapper
		assertEquals(45, coe.semiAxisLength(0), 0);
		assertEquals(50, coe.semiAxisLength(1), 0);
		assertEquals(65, coe.realMin(0), 0);
		assertEquals(12, coe.realMin(1), 0);
		assertEquals(155, coe.realMax(0), 0);
		assertEquals(112, coe.realMax(1), 0);

		// Check underlying OMERO EllipseData
		assertEquals(45, ed.getRadiusX(), 0);
		assertEquals(50, ed.getRadiusY(), 0);
	}

	// -- ClosedOMEROEllipse Tests --

	@Test
	public void testTestClosedEllipse() {
		final RealPoint rp = new RealPoint(110, 62);
		assertTrue(coe.test(rp));

		rp.setPosition(new double[] { 120, 55 });
		assertTrue(coe.test(rp));

		rp.setPosition(new double[] { 110, 82 });
		assertTrue(coe.test(rp));

		rp.setPosition(new double[] { 110, 90 });
		assertFalse(coe.test(rp));
	}

	@Test
	public void testBoundaryTypeClosedEllipse() {
		assertEquals(BoundaryType.CLOSED, coe.boundaryType());
	}

	@Test
	public void testGetCenterClosedEllipse() {
		final RealLocalizable c = coe.center();
		assertEquals(ed.getX(), c.getDoublePosition(0), 0);
		assertEquals(ed.getY(), c.getDoublePosition(1), 0);
	}

	@Test
	public void testSetCenterClosedEllipse() {
		final RealLocalizableRealPositionable c = coe.center();
		c.setPosition(new double[] { 80, 50 });

		// Check IJ Wrapper
		assertEquals(80, c.getDoublePosition(0), 0);
		assertEquals(50, c.getDoublePosition(1), 0);
		assertEquals(35, coe.realMin(0), 0);
		assertEquals(30, coe.realMin(1), 0);
		assertEquals(125, coe.realMax(0), 0);
		assertEquals(70, coe.realMax(1), 0);

		// Check underlying OMERO EllipseData
		assertEquals(80, ed.getX(), 0);
		assertEquals(50, ed.getY(), 0);
	}

	// -- OpenOMEROEllipse Tests --

	@Test
	public void testTestOpenEllipse() {
		final RealPoint rp = new RealPoint(110, 62);
		assertTrue(ooe.test(rp));

		rp.setPosition(new double[] { 120, 55 });
		assertTrue(ooe.test(rp));

		rp.setPosition(new double[] { 110, 82 });
		assertFalse(ooe.test(rp));

		rp.setPosition(new double[] { 110, 90 });
		assertFalse(ooe.test(rp));
	}

	@Test
	public void testBoundaryTypeOpenEllipse() {
		assertEquals(BoundaryType.OPEN, ooe.boundaryType());
	}

	@Test
	public void testGetCenterOpenEllipse() {
		final RealLocalizable c = ooe.center();
		assertEquals(ed.getX(), c.getDoublePosition(0), 0);
		assertEquals(ed.getY(), c.getDoublePosition(1), 1);
	}

	@Test
	public void testSetCenterOpenEllipse() {
		final RealLocalizableRealPositionable c = ooe.center();
		c.setPosition(new double[] { 45, 200.5 });

		// Check IJ Wrapper
		assertEquals(45, c.getDoublePosition(0), 0);
		assertEquals(200.5, c.getDoublePosition(1), 0);
		assertEquals(0, ooe.realMin(0), 0);
		assertEquals(180.5, ooe.realMin(1), 0);
		assertEquals(90, ooe.realMax(0), 0);
		assertEquals(220.5, ooe.realMax(1), 0);

		// Check underlying OMERO EllipseData
		assertEquals(45, ed.getX(), 0);
		assertEquals(200.5, ed.getY(), 0);
	}

}
