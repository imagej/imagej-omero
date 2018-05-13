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

package net.imagej.omero.roi.line;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.DefaultWritableLine;
import net.imglib2.roi.geom.real.Line;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.LineData;

/**
 * Tests {@link OMEROToImageJLine}, {@link ImageJToOMEROLine}, and
 * {@link OMEROLineUnwrapper}.
 *
 * @author Alison Walter
 */
public class LineConversionTest {

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
		final Line ijLine = new DefaultWritableLine(new double[] { 1.5, 6 },
			new double[] { 12.5, 4.25 }, false);
		final Line ijLine3D = new DefaultWritableLine(new double[] { 1.5, 6, 9 },
			new double[] { 12.5, 4.25, 5 }, false);
		final LineData omeroLine = new LineData(1, 1, 3.5, 7);
		final OMEROLine wrap = new DefaultOMEROLine(omeroLine);

		final Converter<?, ?> cOne = convert.getHandler(ijLine, LineData.class);
		assertTrue(cOne instanceof ImageJToOMEROLine);

		final Converter<?, ?> cTwo = convert.getHandler(ijLine3D, LineData.class);
		assertNull(cTwo);

		final Converter<?, ?> cThree = convert.getHandler(omeroLine, Line.class);
		assertTrue(cThree instanceof OMEROToImageJLine);

		final Converter<?, ?> cFour = convert.getHandler(wrap, LineData.class);
		assertTrue(cFour instanceof OMEROLineUnwrapper);
	}

	@Test
	public void testOMEROToImageJ() {
		final LineData omeroLine = new LineData(11, 3, 4, 8);
		final Line ijLine = convert.convert(omeroLine, Line.class);

		assertEquals(BoundaryType.CLOSED, ijLine.boundaryType());
		assertEquals(2, ijLine.numDimensions());
		assertEquals(omeroLine.getX1(), ijLine.endpointOne().getDoublePosition(0),
			0);
		assertEquals(omeroLine.getY1(), ijLine.endpointOne().getDoublePosition(1),
			0);
		assertEquals(omeroLine.getX2(), ijLine.endpointTwo().getDoublePosition(0),
			0);
		assertEquals(omeroLine.getY2(), ijLine.endpointTwo().getDoublePosition(1),
			0);
	}

	@Test
	public void testImageJToOMERO() {
		final Line ijLine = new DefaultWritableLine(new double[] { 11, 13 },
			new double[] { 200.5, 98 }, false);
		final LineData omeroLine = convert.convert(ijLine, LineData.class);

		assertEquals(ijLine.endpointOne().getDoublePosition(0), omeroLine.getX1(),
			0);
		assertEquals(ijLine.endpointOne().getDoublePosition(1), omeroLine.getY1(),
			0);
		assertEquals(ijLine.endpointTwo().getDoublePosition(0), omeroLine.getX2(),
			0);
		assertEquals(ijLine.endpointTwo().getDoublePosition(1), omeroLine.getY2(),
			0);
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, omeroLine.getText());
	}

	@Test
	public void testUnwrapping() {
		final LineData omeroLine = new LineData(103, 89.5, 62.25, 7);
		omeroLine.setId(88);
		final OMEROLine wrap = new DefaultOMEROLine(omeroLine);
		final LineData unwrap = convert.convert(wrap, LineData.class);

		assertEquals(omeroLine.getId(), unwrap.getId());
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, unwrap.getText());
	}

	@Test
	public void testLineDataWithTextValue() {
		final LineData omeroLine = new LineData(98, 31, 325, 43);
		omeroLine.setId(74);
		omeroLine.setText("line");
		final Line ijLine = convert.convert(omeroLine, Line.class);

		assertEquals(BoundaryType.CLOSED, ijLine.boundaryType());

		final LineData unwrap = convert.convert(ijLine, LineData.class);
		assertEquals("line " + ROIConverters.CLOSED_BOUNDARY_TEXT, unwrap
			.getText());
	}
}
