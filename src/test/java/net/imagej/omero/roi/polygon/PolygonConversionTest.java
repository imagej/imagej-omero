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

package net.imagej.omero.roi.polygon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.polygon.ImageJToOMEROPolygon;
import net.imagej.omero.roi.polygon.OMEROPolygon;
import net.imagej.omero.roi.polygon.OMEROPolygonUnwrapper;
import net.imagej.omero.roi.polygon.OMEROToImageJPolygon;
import net.imagej.omero.roi.polygon.OpenOMEROPolygon;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.geom.real.ClosedWritablePolygon2D;
import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;
import net.imglib2.roi.geom.real.OpenWritablePolygon2D;
import net.imglib2.roi.geom.real.Polygon2D;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.PolygonData;
import omero.gateway.model.ShapeData;

/**
 * Tests {@link OMEROToImageJPolygon}, {@link ImageJToOMEROPolygon}, and
 * {@link OMEROPolygonUnwrapper}.
 *
 * @author Alison Walter
 */
public class PolygonConversionTest {

	private ConvertService convert;

	@Before
	public void setUp() {
		final Context c = new Context(OMEROService.class, ConvertService.class,
			LogService.class);
		convert = c.getService(ConvertService.class);
	}

	@After
	public void tearDown() {
		convert.getContext().dispose();
	}

	@Test
	public void testConverterMatching() {
		final Polygon2D ij = new DefaultWritablePolygon2D(new double[] { 12, 12, 20,
			28, 28 }, new double[] { 13, 7.5, 0, 7.5, 13 });
		final Converter<?, ?> cOne = convert.getHandler(ij, ShapeData.class);
		assertTrue(cOne instanceof ImageJToOMEROPolygon);

		final List<Point2D.Double> pts = new ArrayList<>(3);
		pts.add(new Point2D.Double(10, 10));
		pts.add(new Point2D.Double(20, 1));
		pts.add(new Point2D.Double(30, 10));
		final PolygonData omero = new PolygonData(pts);
		final Converter<?, ?> cThree = convert.getHandler(omero,
			MaskPredicate.class);
		assertTrue(cThree instanceof OMEROToImageJPolygon);

		final OMEROPolygon wrapped = new OpenOMEROPolygon(omero);
		final Converter<?, ?> cFour = convert.getHandler(wrapped, ShapeData.class);
		assertTrue(cFour instanceof OMEROPolygonUnwrapper);
	}

	@Test
	public void testOMEROToImageJ() {
		final List<Point2D.Double> pts = new ArrayList<>(6);
		pts.add(new Point2D.Double(10, 10));
		pts.add(new Point2D.Double(20, 1));
		pts.add(new Point2D.Double(30, 10));
		pts.add(new Point2D.Double(30, 20));
		pts.add(new Point2D.Double(20, 29));
		pts.add(new Point2D.Double(10, 20));
		final PolygonData omeroPolygon = new PolygonData(pts);
		final Polygon2D ijPolygon = convert.convert(omeroPolygon, Polygon2D.class);

		assertTrue(ijPolygon instanceof OMEROPolygon);
		assertTrue(ijPolygon.boundaryType() == BoundaryType.CLOSED);

		final List<Point2D.Double> omeroPolyPts = omeroPolygon.getPoints();
		assertEquals(omeroPolyPts.size(), ijPolygon.numVertices());

		for (int i = 0; i < omeroPolyPts.size(); i++) {
			assertEquals(omeroPolyPts.get(i).getX(), ijPolygon.vertex(i)
				.getDoublePosition(0), 0);
			assertEquals(omeroPolyPts.get(i).getY(), ijPolygon.vertex(i)
				.getDoublePosition(1), 0);
		}
	}

	@Test
	public void testImageJToOMEROClosedPolygon() {
		final Polygon2D ijPolygon = new ClosedWritablePolygon2D(new double[] { 5.5,
			11, 5.5 }, new double[] { 101.25, 6, 82 });
		final PolygonData omeroPolygon = convert.convert(ijPolygon,
			PolygonData.class);

		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, omeroPolygon.getText());

		final List<Point2D.Double> omeroPolyPts = omeroPolygon.getPoints();
		assertEquals(ijPolygon.numVertices(), omeroPolyPts.size());

		for (int i = 0; i < ijPolygon.numVertices(); i++) {
			assertEquals(ijPolygon.vertex(i).getDoublePosition(0), omeroPolyPts.get(i)
				.getX(), 0);
			assertEquals(ijPolygon.vertex(i).getDoublePosition(1), omeroPolyPts.get(i)
				.getY(), 0);
		}
	}

	@Test
	public void testImageJToOMEROOpenPolygon() {
		final Polygon2D ijPolygon = new OpenWritablePolygon2D(new double[] { 5.5,
			11, 5.5 }, new double[] { 101.25, 6, 82 });
		final PolygonData omeroPolygon = convert.convert(ijPolygon,
			PolygonData.class);

		assertEquals(ROIConverters.OPEN_BOUNDARY_TEXT, omeroPolygon.getText());

		final List<Point2D.Double> omeroPolyPts = omeroPolygon.getPoints();
		assertEquals(ijPolygon.numVertices(), omeroPolyPts.size());

		for (int i = 0; i < ijPolygon.numVertices(); i++) {
			assertEquals(ijPolygon.vertex(i).getDoublePosition(0), omeroPolyPts.get(i)
				.getX(), 0);
			assertEquals(ijPolygon.vertex(i).getDoublePosition(1), omeroPolyPts.get(i)
				.getY(), 0);
		}
	}

	@Test
	public void testImageJToOMERODefaultPolygon() {
		final Polygon2D ijPolygon = new DefaultWritablePolygon2D(new double[] { 5.5,
			11, 5.5 }, new double[] { 101.25, 6, 82 });
		final PolygonData omeroPolygon = convert.convert(ijPolygon,
			PolygonData.class);

		assertEquals(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, omeroPolygon
			.getText());

		final List<Point2D.Double> omeroPolyPts = omeroPolygon.getPoints();
		assertEquals(ijPolygon.numVertices(), omeroPolyPts.size());

		for (int i = 0; i < ijPolygon.numVertices(); i++) {
			assertEquals(ijPolygon.vertex(i).getDoublePosition(0), omeroPolyPts.get(i)
				.getX(), 0);
			assertEquals(ijPolygon.vertex(i).getDoublePosition(1), omeroPolyPts.get(i)
				.getY(), 0);
		}
	}

	@Test
	public void testUnwrapping() {
		final List<Point2D.Double> pts = new ArrayList<>(3);
		pts.add(new Point2D.Double(10, 10));
		pts.add(new Point2D.Double(20, 1.25));
		pts.add(new Point2D.Double(30, 10));
		final PolygonData omeroPolygon = new PolygonData(pts);
		omeroPolygon.setId(900);
		omeroPolygon.setText("Polygon");
		final OMEROPolygon wrap = new OpenOMEROPolygon(omeroPolygon);
		final PolygonData unwrapped = convert.convert(wrap, PolygonData.class);

		final List<Point2D.Double> omeroPolyPts = omeroPolygon.getPoints();
		final List<Point2D.Double> unwrappedPolyPts = unwrapped.getPoints();
		assertEquals(omeroPolyPts.size(), unwrappedPolyPts.size());
		for (int i = 0; i < omeroPolyPts.size(); i++) {
			assertEquals(omeroPolyPts.get(i).getX(), unwrappedPolyPts.get(i).getX(),
				0);
			assertEquals(omeroPolyPts.get(i).getY(), unwrappedPolyPts.get(i).getY(),
				0);
		}
		assertEquals(omeroPolygon.getId(), unwrapped.getId());
		assertEquals("Polygon " + ROIConverters.OPEN_BOUNDARY_TEXT, unwrapped
			.getText());
	}

	@Test
	public void testPolygonDataWithTextValue() {
		final PolygonData omeroPolygon = new PolygonData();
		omeroPolygon.setText("ij-bt[C]polygon " +
			ROIConverters.UNSPECIFIED_BOUNDARY_TEXT + " stuff");
		final Polygon2D ijPolygon = convert.convert(omeroPolygon, Polygon2D.class);

		assertEquals(ijPolygon.boundaryType(), BoundaryType.UNSPECIFIED);

		final PolygonData unwrapped = convert.convert(ijPolygon, PolygonData.class);
		assertEquals("ij-bt[C]polygon " + ROIConverters.UNSPECIFIED_BOUNDARY_TEXT +
			" stuff", unwrapped.getText());
	}
}
