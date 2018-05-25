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

package net.imagej.omero.roi.polygon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import java.awt.geom.Point2D;
import java.util.List;

import net.imagej.omero.roi.polygon.ClosedOMEROPolygon;
import net.imagej.omero.roi.polygon.DefaultOMEROPolygon;
import net.imagej.omero.roi.polygon.OMEROPolygon;
import net.imagej.omero.roi.polygon.OpenOMEROPolygon;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.roi.BoundaryType;

import org.junit.Before;
import org.junit.Test;

import omero.gateway.model.PolygonData;

/**
 * Tests for {@link OMEROPolygon}, {@link DefaultOMEROPolygon},
 * {@link ClosedOMEROPolygon}, and {@link OpenOMEROPolygon}.
 *
 * @author Alison Walter
 */
public class OMEROPolygonTest {

	private PolygonData pd;
	private OMEROPolygon defaultPolygon;
	private OMEROPolygon closedPolygon;
	private OMEROPolygon openPolygon;
	private RealPoint inside;
	private RealPoint outside;
	private RealPoint boundary;
	private RealPoint boundaryTwo;

	@Before
	public void setup() {
		final Point2D.Double p1 = new Point2D.Double(100, 100);
		final Point2D.Double p2 = new Point2D.Double(100, 80);
		final Point2D.Double p3 = new Point2D.Double(120, 60);
		final Point2D.Double p4 = new Point2D.Double(140, 80);
		final Point2D.Double p5 = new Point2D.Double(140, 100);
		pd = new PolygonData(Lists.newArrayList(p1, p2, p3, p4, p5));

		defaultPolygon = new DefaultOMEROPolygon(pd);
		closedPolygon = new ClosedOMEROPolygon(pd);
		openPolygon = new OpenOMEROPolygon(pd);

		inside = new RealPoint(new double[] { 110, 90 });
		outside = new RealPoint(new double[] { 150, 99.5 });
		boundary = new RealPoint(new double[] { 100, 85 });
		boundaryTwo = new RealPoint(new double[] { 140, 85 });
	}

	// -- OMEROPolygon --

	@Test
	public void testNumVertices() {
		assertEquals(pd.getPoints().size(), defaultPolygon.numVertices());
	}

	@Test
	public void testMinMax() {
		assertEquals(2, closedPolygon.numDimensions());

		assertEquals(100, closedPolygon.realMin(0), 0);
		assertEquals(60, closedPolygon.realMin(1), 0);
		assertEquals(140, closedPolygon.realMax(0), 0);
		assertEquals(100, closedPolygon.realMax(1), 0);
	}

	@Test
	public void testAddVertex() {
		openPolygon.addVertex(5, new double[] { 120, 120 });

		// Check wrapper
		assertEquals(6, openPolygon.numVertices());

		assertEquals(100, openPolygon.realMin(0), 0);
		assertEquals(60, openPolygon.realMin(1), 0);
		assertEquals(140, openPolygon.realMax(0), 0);
		assertEquals(120, openPolygon.realMax(1), 0);

		// Check PolygonData
		assertEquals(6, pd.getPoints().size());
		assertEquals(120, pd.getPoints().get(5).getX(), 0);
		assertEquals(120, pd.getPoints().get(5).getY(), 0);
	}

	@Test
	public void testRemoveVertex() {
		closedPolygon.removeVertex(2);

		// Check wrapper
		assertEquals(4, closedPolygon.numVertices());

		assertEquals(100, closedPolygon.realMin(0), 0);
		assertEquals(80, closedPolygon.realMin(1), 0);
		assertEquals(140, closedPolygon.realMax(0), 0);
		assertEquals(100, closedPolygon.realMax(1), 0);

		// Check PolygonData
		assertEquals(4, pd.getPoints().size());
		final List<Point2D.Double> pt = pd.getPoints();
		assertEquals(100, pt.get(0).getX(), 0);
		assertEquals(100, pt.get(0).getY(), 0);
		assertEquals(100, pt.get(1).getX(), 0);
		assertEquals(80, pt.get(1).getY(), 0);
		assertEquals(140, pt.get(2).getX(), 0);
		assertEquals(80, pt.get(2).getY(), 0);
		assertEquals(140, pt.get(3).getX(), 0);
		assertEquals(100, pt.get(3).getY(), 0);
	}

	// -- DefaultOMEROPolygon --

	@Test
	public void testDefaultBoundaryType() {
		assertEquals(BoundaryType.UNSPECIFIED, defaultPolygon.boundaryType());
	}

	@Test
	public void testDefaultTest() {
		assertTrue(defaultPolygon.test(inside));
		assertFalse(defaultPolygon.test(outside));

		// Boundary behavior is inconsistent, which is expected
		assertTrue(defaultPolygon.test(boundary));
		assertFalse(defaultPolygon.test(boundaryTwo));
	}

	@Test
	public void testDefaultGetVertex() {
		final List<Point2D.Double> pts = pd.getPoints();

		assertEquals(pts.get(0).getX(), defaultPolygon.vertex(0).getDoublePosition(
			0), 0);
		assertEquals(pts.get(0).getY(), defaultPolygon.vertex(0).getDoublePosition(
			1), 0);

		assertEquals(pts.get(1).getX(), defaultPolygon.vertex(1).getDoublePosition(
			0), 0);
		assertEquals(pts.get(1).getY(), defaultPolygon.vertex(1).getDoublePosition(
			1), 0);

		assertEquals(pts.get(2).getX(), defaultPolygon.vertex(2).getDoublePosition(
			0), 0);
		assertEquals(pts.get(2).getY(), defaultPolygon.vertex(2).getDoublePosition(
			1), 0);

		assertEquals(pts.get(3).getX(), defaultPolygon.vertex(3).getDoublePosition(
			0), 0);
		assertEquals(pts.get(3).getY(), defaultPolygon.vertex(3).getDoublePosition(
			1), 0);

		assertEquals(pts.get(4).getX(), defaultPolygon.vertex(4).getDoublePosition(
			0), 0);
		assertEquals(pts.get(4).getY(), defaultPolygon.vertex(4).getDoublePosition(
			1), 0);
	}

	@Test
	public void testDefaultSetVertex() {
		assertTrue(defaultPolygon.test(inside));
		assertFalse(defaultPolygon.test(outside));

		final RealPositionable v = defaultPolygon.vertex(4);
		v.setPosition(new double[] { 160, 100 });

		// Wrapper
		assertTrue(defaultPolygon.test(inside));
		assertTrue(defaultPolygon.test(outside));

		assertEquals(100, defaultPolygon.realMin(0), 0);
		assertEquals(60, defaultPolygon.realMin(1), 0);
		assertEquals(160, defaultPolygon.realMax(0), 0);
		assertEquals(100, defaultPolygon.realMax(1), 0);

		// PolygonData
		assertEquals(160, pd.getPoints().get(4).getX(), 0);
		assertEquals(100, pd.getPoints().get(4).getY(), 0);
	}

	// -- ClosedOMEROPolygon --

	@Test
	public void testClosedBoundaryType() {
		assertEquals(BoundaryType.CLOSED, closedPolygon.boundaryType());
	}

	@Test
	public void testClosedTest() {
		assertTrue(closedPolygon.test(inside));
		assertFalse(closedPolygon.test(outside));

		// Boundaries included
		assertTrue(closedPolygon.test(boundary));
		assertTrue(closedPolygon.test(boundaryTwo));
	}

	@Test
	public void testClosedGetVertex() {
		final List<Point2D.Double> pts = pd.getPoints();

		assertEquals(pts.get(0).getX(), closedPolygon.vertex(0).getDoublePosition(
			0), 0);
		assertEquals(pts.get(0).getY(), closedPolygon.vertex(0).getDoublePosition(
			1), 0);

		assertEquals(pts.get(1).getX(), closedPolygon.vertex(1).getDoublePosition(
			0), 0);
		assertEquals(pts.get(1).getY(), closedPolygon.vertex(1).getDoublePosition(
			1), 0);

		assertEquals(pts.get(2).getX(), closedPolygon.vertex(2).getDoublePosition(
			0), 0);
		assertEquals(pts.get(2).getY(), closedPolygon.vertex(2).getDoublePosition(
			1), 0);

		assertEquals(pts.get(3).getX(), closedPolygon.vertex(3).getDoublePosition(
			0), 0);
		assertEquals(pts.get(3).getY(), closedPolygon.vertex(3).getDoublePosition(
			1), 0);

		assertEquals(pts.get(4).getX(), closedPolygon.vertex(4).getDoublePosition(
			0), 0);
		assertEquals(pts.get(4).getY(), closedPolygon.vertex(4).getDoublePosition(
			1), 0);
	}

	@Test
	public void testClosedSetVertex() {
		final RealPoint outside2 = new RealPoint(new double[] { 80, 75 });
		assertTrue(closedPolygon.test(inside));
		assertFalse(closedPolygon.test(outside2));

		final RealPositionable v = closedPolygon.vertex(1);
		v.setPosition(new double[] { 50, 70 });

		// Wrapper
		assertTrue(closedPolygon.test(inside));
		assertTrue(closedPolygon.test(outside2));

		assertEquals(50, closedPolygon.realMin(0), 0);
		assertEquals(60, closedPolygon.realMin(1), 0);
		assertEquals(140, closedPolygon.realMax(0), 0);
		assertEquals(100, closedPolygon.realMax(1), 0);

		// PolygonData
		assertEquals(50, pd.getPoints().get(1).getX(), 0);
		assertEquals(70, pd.getPoints().get(1).getY(), 0);
	}

	// -- OpenOMEROPolygon --

	@Test
	public void testOpenBoundaryType() {
		assertEquals(BoundaryType.OPEN, openPolygon.boundaryType());
	}

	@Test
	public void testOpenTest() {
		assertTrue(openPolygon.test(inside));
		assertFalse(openPolygon.test(outside));

		// Boundaries excluded
		assertFalse(openPolygon.test(boundary));
		assertFalse(openPolygon.test(boundaryTwo));
	}

	@Test
	public void testOpenGetVertex() {
		final List<Point2D.Double> pts = pd.getPoints();

		assertEquals(pts.get(0).getX(), openPolygon.vertex(0).getDoublePosition(0),
			0);
		assertEquals(pts.get(0).getY(), openPolygon.vertex(0).getDoublePosition(1),
			0);

		assertEquals(pts.get(1).getX(), openPolygon.vertex(1).getDoublePosition(0),
			0);
		assertEquals(pts.get(1).getY(), openPolygon.vertex(1).getDoublePosition(1),
			0);

		assertEquals(pts.get(2).getX(), openPolygon.vertex(2).getDoublePosition(0),
			0);
		assertEquals(pts.get(2).getY(), openPolygon.vertex(2).getDoublePosition(1),
			0);

		assertEquals(pts.get(3).getX(), openPolygon.vertex(3).getDoublePosition(0),
			0);
		assertEquals(pts.get(3).getY(), openPolygon.vertex(3).getDoublePosition(1),
			0);

		assertEquals(pts.get(4).getX(), openPolygon.vertex(4).getDoublePosition(0),
			0);
		assertEquals(pts.get(4).getY(), openPolygon.vertex(4).getDoublePosition(1),
			0);
	}

	@Test
	public void testOpenSetVertex() {
		final RealPoint inside2 = new RealPoint(new double[] { 120, 75.5 });
		assertTrue(openPolygon.test(inside2));
		assertFalse(openPolygon.test(outside));

		final RealPositionable v = openPolygon.vertex(2);
		v.setPosition(new double[] { 110, 90 });

		// Wrapper
		assertFalse(openPolygon.test(inside2));
		assertFalse(openPolygon.test(outside));

		assertEquals(100, openPolygon.realMin(0), 0);
		assertEquals(80, openPolygon.realMin(1), 0);
		assertEquals(140, openPolygon.realMax(0), 0);
		assertEquals(100, openPolygon.realMax(1), 0);

		// PolygonData
		assertEquals(110, pd.getPoints().get(2).getX(), 0);
		assertEquals(90, pd.getPoints().get(2).getY(), 0);
	}
}
