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

package net.imagej.omero.roi.polyline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import java.awt.geom.Point2D;
import java.util.List;

import net.imagej.omero.roi.polyline.DefaultOMEROPolyline;
import net.imagej.omero.roi.polyline.OMEROPolyline;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;

import org.junit.Before;
import org.junit.Test;

import omero.gateway.model.PolylineData;

/**
 * Tests {@link OMEROPolyline} and {@link DefaultOMEROPolyline}.
 *
 * @author Alison Walter
 */
public class OMEROPolylineTest {

	private PolylineData pd;
	private OMEROPolyline op;

	@Before
	public void setup() {
		final Point2D.Double p1 = new Point2D.Double(10, 12);
		final Point2D.Double p2 = new Point2D.Double(21, 23);
		final Point2D.Double p3 = new Point2D.Double(5, 55);
		final Point2D.Double p4 = new Point2D.Double(20, 0);
		pd = new PolylineData(Lists.newArrayList(p1, p2, p3, p4));
		op = new DefaultOMEROPolyline(pd);
	}

	@Test
	public void testTest() {
		final RealPoint test = new RealPoint(new double[] { 10, 45 });
		assertTrue(op.test(test));

		test.setPosition(new double[] { 11, 33 });
		assertTrue(op.test(test));

		test.setPosition(new double[] { 9, 11 });
		assertFalse(op.test(test));

		test.setPosition(new double[] { 11, 14 });
		assertFalse(op.test(test));
	}

	@Test
	public void testBoundaryType() {
		assertEquals(BoundaryType.CLOSED, op.boundaryType());
	}

	@Test
	public void testMinMax() {
		assertEquals(2, op.numDimensions());

		assertEquals(5, op.realMin(0), 0);
		assertEquals(0, op.realMin(1), 0);
		assertEquals(21, op.realMax(0), 0);
		assertEquals(55, op.realMax(1), 0);
	}

	@Test
	public void testNumVertices() {
		assertEquals(pd.getPoints().size(), op.numVertices());
	}

	@Test
	public void testAddVertex() {
		final RealPoint on = new RealPoint(new double[] { 11, 13 });
		final RealPoint off = new RealPoint(new double[] { 11, 14 });
		assertTrue(op.test(on));
		assertFalse(op.test(off));

		op.addVertex(1, new RealPoint(new double[] { 15, 22 }));

		// Check wrapper
		assertEquals(5, op.numVertices());

		assertFalse(op.test(on));
		assertTrue(op.test(off));

		assertEquals(5, op.realMin(0), 0);
		assertEquals(0, op.realMin(1), 0);
		assertEquals(21, op.realMax(0), 0);
		assertEquals(55, op.realMax(1), 0);

		// Check PolylineData
		assertEquals(15, pd.getPoints().get(1).getX(), 0);
		assertEquals(22, pd.getPoints().get(1).getY(), 0);
		assertEquals(5, pd.getPoints().size());
	}

	@Test
	public void testRemoveVertex() {
		final RealPoint on = new RealPoint(new double[] { 11, 33 });
		final RealPoint off = new RealPoint(new double[] { 20.5, 11.5 });
		assertTrue(op.test(on));
		assertFalse(op.test(off));

		op.removeVertex(2);

		// Check wrapper
		assertEquals(3, op.numVertices());

		assertFalse(op.test(on));
		assertTrue(op.test(off));

		assertEquals(10, op.realMin(0), 0);
		assertEquals(0, op.realMin(1), 0);
		assertEquals(21, op.realMax(0), 0);
		assertEquals(23, op.realMax(1), 0);

		// Check PolylineData
		assertEquals(3, pd.getPoints().size());

		final List<Point2D.Double> pts = pd.getPoints();
		assertEquals(10, pts.get(0).getX(), 0);
		assertEquals(12, pts.get(0).getY(), 0);
		assertEquals(21, pts.get(1).getX(), 0);
		assertEquals(23, pts.get(1).getY(), 0);
		assertEquals(20, pts.get(2).getX(), 0);
		assertEquals(0, pts.get(2).getY(), 0);
	}

	@Test
	public void testGetVertex() {
		final List<Point2D.Double> pts = pd.getPoints();

		assertEquals(pts.get(0).getX(), op.vertex(0).getDoublePosition(0), 0);
		assertEquals(pts.get(0).getY(), op.vertex(0).getDoublePosition(1), 0);

		assertEquals(pts.get(1).getX(), op.vertex(1).getDoublePosition(0), 0);
		assertEquals(pts.get(1).getY(), op.vertex(1).getDoublePosition(1), 0);

		assertEquals(pts.get(2).getX(), op.vertex(2).getDoublePosition(0), 0);
		assertEquals(pts.get(2).getY(), op.vertex(2).getDoublePosition(1), 0);

		assertEquals(pts.get(3).getX(), op.vertex(3).getDoublePosition(0), 0);
		assertEquals(pts.get(3).getY(), op.vertex(3).getDoublePosition(1), 0);
	}

	@Test
	public void testSetVertex() {
		final RealPoint on = new RealPoint(new double[] { 10, 45 });
		final RealPoint off = new RealPoint(new double[] { 45, 47 });
		assertTrue(op.test(on));
		assertFalse(op.test(off));

		op.vertex(2).setPosition(new double[] { 61, 63 });

		// Check wrapper
		assertFalse(op.test(on));
		assertTrue(op.test(off));

		assertEquals(10, op.realMin(0), 0);
		assertEquals(0, op.realMin(1), 0);
		assertEquals(61, op.realMax(0), 0);
		assertEquals(63, op.realMax(1), 0);

		// Check PolylineData
		assertEquals(61, pd.getPoints().get(2).getX(), 0);
		assertEquals(63, pd.getPoints().get(2).getY(), 0);
	}
}
