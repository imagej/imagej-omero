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

package net.imagej.omero.roi.rectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.rectangle.ClosedOMERORectangle;
import net.imagej.omero.roi.rectangle.ImageJToOMERORectangle;
import net.imagej.omero.roi.rectangle.OMERORectangle;
import net.imagej.omero.roi.rectangle.OMERORectangleUnwrapper;
import net.imagej.omero.roi.rectangle.OMEROToImageJRectangle;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.ClosedWritableBox;
import net.imglib2.roi.geom.real.OpenWritableBox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;

/**
 * Tests {@link OMEROToImageJRectangle}, {@link ImageJToOMERORectangle}, and
 * {@link OMERORectangleUnwrapper}.
 *
 * @author Alison Walter
 */
public class RectangleConversionTest {

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
		final Box ij = new ClosedWritableBox(new double[] { 20.125, 82 },
			new double[] { 36, 104 });
		final Converter<?, ?> cOne = convert.getHandler(ij, ShapeData.class);
		assertTrue(cOne instanceof ImageJToOMERORectangle);

		final Box threeD = new ClosedWritableBox(new double[] { 0, 1, 2 },
			new double[] { 1, 2, 3 });
		final Converter<?, ?> cTwo = convert.getHandler(threeD, ShapeData.class);
		assertNull(cTwo);

		final RectangleData omero = new RectangleData(0, 0, 71, 9);
		final Converter<?, ?> cThree = convert.getHandler(omero,
			MaskPredicate.class);
		assertTrue(cThree instanceof OMEROToImageJRectangle);

		final OMERORectangle wrapped = new ClosedOMERORectangle(omero);
		final Converter<?, ?> cFour = convert.getHandler(wrapped, ShapeData.class);
		assertTrue(cFour instanceof OMERORectangleUnwrapper);
	}

	@Test
	public void testOMEROToImageJ() {
		final RectangleData omeroRectangle = new RectangleData(9.5, 11, 5, 3.5);
		final Box ijRectangle = convert.convert(omeroRectangle, Box.class);

		assertTrue(ijRectangle instanceof OMERORectangle);
		assertTrue(ijRectangle.boundaryType() == BoundaryType.CLOSED);
		assertEquals(2, ijRectangle.numDimensions());

		assertEquals(omeroRectangle.getX(), ijRectangle.center().getDoublePosition(
			0) - ijRectangle.sideLength(0) / 2, 0);
		assertEquals(omeroRectangle.getY(), ijRectangle.center().getDoublePosition(
			1) - ijRectangle.sideLength(1) / 2, 0);
		assertEquals(omeroRectangle.getWidth(), ijRectangle.sideLength(0), 0);
		assertEquals(omeroRectangle.getHeight(), ijRectangle.sideLength(1), 0);
	}

	@Test
	public void testImageJToOMEROClosedRectangle() {
		final Box ijRectangle = new ClosedWritableBox(new double[] { 5.5, 3 },
			new double[] { 2, 0.5 });
		final RectangleData omeroRectangle = convert.convert(ijRectangle,
			RectangleData.class);

		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, omeroRectangle.getText());
		assertEquals(-1, omeroRectangle.getZ());
		assertEquals(-1, omeroRectangle.getT());
		assertEquals(-1, omeroRectangle.getC());

		assertEquals(ijRectangle.center().getDoublePosition(0), omeroRectangle
			.getX() + omeroRectangle.getWidth() / 2, 0);
		assertEquals(ijRectangle.center().getDoublePosition(1), omeroRectangle
			.getY() + omeroRectangle.getHeight() / 2, 0);
		assertEquals(ijRectangle.sideLength(0), omeroRectangle.getWidth(), 0);
		assertEquals(ijRectangle.sideLength(1), omeroRectangle.getHeight(), 0);
	}

	@Test
	public void testImageJToOMEROOpenRectangle() {
		final Box ijRectangle = new OpenWritableBox(new double[] { 90, 52.25 },
			new double[] { 8, 7 });
		final RectangleData omeroRectangle = convert.convert(ijRectangle,
			RectangleData.class);

		assertEquals(ROIConverters.OPEN_BOUNDARY_TEXT, omeroRectangle.getText());
		assertEquals(-1, omeroRectangle.getZ());
		assertEquals(-1, omeroRectangle.getT());
		assertEquals(-1, omeroRectangle.getC());

		assertEquals(ijRectangle.center().getDoublePosition(0), omeroRectangle
			.getX() + omeroRectangle.getWidth() / 2, 0);
		assertEquals(ijRectangle.center().getDoublePosition(1), omeroRectangle
			.getY() + omeroRectangle.getHeight() / 2, 0);
		assertEquals(ijRectangle.sideLength(0), omeroRectangle.getWidth(), 0);
		assertEquals(ijRectangle.sideLength(1), omeroRectangle.getHeight(), 0);
	}

	@Test
	public void testUnwrapping() {
		final RectangleData omeroRectangle = new RectangleData(10, 22.5, 2, 8);
		omeroRectangle.setId(900);
		omeroRectangle.setText("Rectangle");
		omeroRectangle.setZ(3);
		omeroRectangle.setT(22);
		omeroRectangle.setC(1);
		final OMERORectangle wrap = new ClosedOMERORectangle(omeroRectangle);
		final RectangleData unwrapped = convert.convert(wrap, RectangleData.class);

		assertEquals("Rectangle " + ROIConverters.CLOSED_BOUNDARY_TEXT, unwrapped
			.getText());
		assertEquals(omeroRectangle.getId(), unwrapped.getId());

		assertEquals(omeroRectangle.getX(), unwrapped.getX(), 0);
		assertEquals(omeroRectangle.getY(), unwrapped.getY(), 0);
		assertEquals(omeroRectangle.getWidth(), unwrapped.getWidth(), 0);
		assertEquals(omeroRectangle.getHeight(), unwrapped.getHeight(), 0);
		assertEquals(omeroRectangle.getZ(), unwrapped.getZ(), 0);
		assertEquals(omeroRectangle.getT(), unwrapped.getT(), 0);
		assertEquals(omeroRectangle.getC(), unwrapped.getC(), 0);
	}

	@Test
	public void testRectangleDataWithTextValue() {
		final RectangleData omeroRectangle = new RectangleData(13, 6, 2.5, 0.25);
		omeroRectangle.setText("rectangle" + ROIConverters.OPEN_BOUNDARY_TEXT +
			" stuff");
		final Box ijRectangle = convert.convert(omeroRectangle, Box.class);

		assertEquals(ijRectangle.boundaryType(), BoundaryType.OPEN);

		final RectangleData unwrapped = convert.convert(ijRectangle,
			RectangleData.class);
		assertEquals("rectangle" + ROIConverters.OPEN_BOUNDARY_TEXT + " stuff",
			unwrapped.getText());
	}
}
