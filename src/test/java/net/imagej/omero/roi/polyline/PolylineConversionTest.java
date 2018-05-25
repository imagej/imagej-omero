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

package net.imagej.omero.roi.polyline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.polyline.DefaultOMEROPolyline;
import net.imagej.omero.roi.polyline.ImageJToOMEROPolyline;
import net.imagej.omero.roi.polyline.OMEROPolyline;
import net.imagej.omero.roi.polyline.OMEROPolylineUnwrapper;
import net.imagej.omero.roi.polyline.OMEROToImageJPolyline;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.DefaultWritablePolyline;
import net.imglib2.roi.geom.real.Polyline;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.PolylineData;

/**
 * Tests {@link OMEROToImageJPolyline}, {@link ImageJToOMEROPolyline}, and
 * {@link OMEROPolylineUnwrapper}.
 *
 * @author Alison Walter
 */
public class PolylineConversionTest {

	private List<RealLocalizable> ijPts;
	private List<Point2D.Double> omeroPts;
	private ConvertService convert;

	@Before
	public void setUp() {
		final Context c = new Context(OMEROService.class, ConvertService.class,
			LogService.class);
		convert = c.getService(ConvertService.class);

		ijPts = new ArrayList<>(4);
		ijPts.add(new RealPoint(new double[] { 1, 82 }));
		ijPts.add(new RealPoint(new double[] { 3.25, 60 }));
		ijPts.add(new RealPoint(new double[] { 50, 75 }));
		ijPts.add(new RealPoint(new double[] { 100, 92.5 }));

		omeroPts = new ArrayList<>(3);
		omeroPts.add(new Point2D.Double(25.25, 10));
		omeroPts.add(new Point2D.Double(0.125, 93));
		omeroPts.add(new Point2D.Double(64, 58.5));
	}

	@After
	public void tearDown() {
		convert.getContext().dispose();
	}

	@Test
	public void testConverterMatching() {
		final List<RealLocalizable> threeDPts = new ArrayList<>(3);
		threeDPts.add(new RealPoint(new double[] { 9, 5, 0.25 }));
		threeDPts.add(new RealPoint(new double[] { 56, 80.25, 91 }));
		threeDPts.add(new RealPoint(new double[] { 1094, 89.5, 798 }));

		final Polyline ijPolyline = new DefaultWritablePolyline(ijPts);
		final Polyline ijPolyline3D = new DefaultWritablePolyline(threeDPts);
		final PolylineData omeroPolyline = new PolylineData(omeroPts);
		final OMEROPolyline wrap = new DefaultOMEROPolyline(omeroPolyline);

		final Converter<?, ?> cOne = convert.getHandler(ijPolyline,
			PolylineData.class);
		assertTrue(cOne instanceof ImageJToOMEROPolyline);

		final Converter<?, ?> cTwo = convert.getHandler(ijPolyline3D,
			PolylineData.class);
		assertNull(cTwo);

		final Converter<?, ?> cThree = convert.getHandler(omeroPolyline,
			Polyline.class);
		assertTrue(cThree instanceof OMEROToImageJPolyline);

		final Converter<?, ?> cFour = convert.getHandler(wrap, PolylineData.class);
		assertTrue(cFour instanceof OMEROPolylineUnwrapper);
	}

	@Test
	public void testOMEROToImageJ() {
		final PolylineData omeroPolyline = new PolylineData(omeroPts);
		final Polyline ijPolyline = convert.convert(omeroPolyline, Polyline.class);

		assertEquals(BoundaryType.CLOSED, ijPolyline.boundaryType());
		assertEquals(2, ijPolyline.numDimensions());

		final List<Point2D.Double> omeroPolyPts = omeroPolyline.getPoints();
		assertEquals(omeroPolyPts.size(), ijPolyline.numVertices());
		for (int i = 0; i < omeroPolyPts.size(); i++) {
			assertEquals(omeroPolyPts.get(i).getX(), ijPolyline.vertex(i)
				.getDoublePosition(0), 0);
			assertEquals(omeroPolyPts.get(i).getY(), ijPolyline.vertex(i)
				.getDoublePosition(1), 0);
		}
	}

	@Test
	public void testImageJToOMERO() {
		final Polyline ijPolyline = new DefaultWritablePolyline(ijPts);
		final PolylineData omeroPolyline = convert.convert(ijPolyline,
			PolylineData.class);

		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, omeroPolyline.getText());

		final List<Point2D.Double> omeroPolyPts = omeroPolyline.getPoints();
		assertEquals(ijPolyline.numVertices(), omeroPolyPts.size());
		for (int i = 0; i < ijPolyline.numVertices(); i++) {
			assertEquals(ijPolyline.vertex(i).getDoublePosition(0), omeroPolyPts.get(
				i).getX(), 0);
			assertEquals(ijPolyline.vertex(i).getDoublePosition(1), omeroPolyPts.get(
				i).getY(), 0);
		}
	}

	@Test
	public void testUnwrapping() {
		final PolylineData omeroPolyline = new PolylineData(omeroPts);
		omeroPolyline.setId(88);
		final OMEROPolyline wrap = new DefaultOMEROPolyline(omeroPolyline);
		final PolylineData unwrap = convert.convert(wrap, PolylineData.class);

		assertEquals(omeroPolyline.getId(), unwrap.getId());
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, unwrap.getText());

		final List<Point2D.Double> omeroPolyPts = omeroPolyline.getPoints();
		final List<Point2D.Double> unwrapPolyPts = unwrap.getPoints();
		assertEquals(omeroPolyPts.size(), unwrapPolyPts.size());
		for (int i = 0; i < omeroPolyPts.size(); i++) {
			assertEquals(omeroPolyPts.get(i).getX(), unwrapPolyPts.get(i).getX(), 0);
			assertEquals(omeroPolyPts.get(i).getY(), unwrapPolyPts.get(i).getY(), 0);
		}
	}

	@Test
	public void testPolylineDataWithTextValue() {
		final PolylineData omeroPolyline = new PolylineData(omeroPts);
		omeroPolyline.setId(74);
		omeroPolyline.setText("polyline");
		final Polyline ijPolyline = convert.convert(omeroPolyline, Polyline.class);

		assertEquals(BoundaryType.CLOSED, ijPolyline.boundaryType());

		final PolylineData unwrap = convert.convert(ijPolyline, PolylineData.class);
		assertEquals("polyline " + ROIConverters.CLOSED_BOUNDARY_TEXT, unwrap
			.getText());
	}
}
