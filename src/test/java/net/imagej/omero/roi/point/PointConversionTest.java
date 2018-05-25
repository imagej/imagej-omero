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

package net.imagej.omero.roi.point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.point.DefaultOMEROPoint;
import net.imagej.omero.roi.point.ImageJToOMEROPoint;
import net.imagej.omero.roi.point.OMEROPoint;
import net.imagej.omero.roi.point.OMEROPointUnwrapper;
import net.imagej.omero.roi.point.OMEROToImageJPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.DefaultWritablePointMask;
import net.imglib2.roi.geom.real.PointMask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.PointData;

/**
 * Tests {@link OMEROToImageJPoint}, {@link ImageJToOMEROPoint}, and
 * {@link OMEROPointUnwrapper}.
 *
 * @author Alison Walter
 */
public class PointConversionTest {

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
		final PointMask ijPoint = new DefaultWritablePointMask(new double[] { 1.5,
			6 });
		final PointMask ijPoint3D = new DefaultWritablePointMask(new double[] { 1.5,
			6, 9 });
		final PointData omeroPoint = new PointData(32.25, 94.5);
		final OMEROPoint wrap = new DefaultOMEROPoint(omeroPoint);

		final Converter<?, ?> cOne = convert.getHandler(ijPoint, PointData.class);
		assertTrue(cOne instanceof ImageJToOMEROPoint);

		final Converter<?, ?> cTwo = convert.getHandler(ijPoint3D, PointData.class);
		assertNull(cTwo);

		final Converter<?, ?> cThree = convert.getHandler(omeroPoint,
			PointMask.class);
		assertTrue(cThree instanceof OMEROToImageJPoint);

		final Converter<?, ?> cFour = convert.getHandler(wrap, PointData.class);
		assertTrue(cFour instanceof OMEROPointUnwrapper);
	}

	@Test
	public void testOMEROToImageJ() {
		final PointData omeroPoint = new PointData(11, 3.5);
		final PointMask ijPoint = convert.convert(omeroPoint, PointMask.class);

		assertEquals(BoundaryType.CLOSED, ijPoint.boundaryType());
		assertEquals(2, ijPoint.numDimensions());
		assertEquals(omeroPoint.getX(), ijPoint.getDoublePosition(0), 0);
		assertEquals(omeroPoint.getY(), ijPoint.getDoublePosition(1), 0);
	}

	@Test
	public void testImageJToOMERO() {
		final PointMask ijPoint = new DefaultWritablePointMask(new double[] { 13,
			251.25 });
		final PointData omeroPoint = convert.convert(ijPoint, PointData.class);

		assertEquals(ijPoint.getDoublePosition(0), omeroPoint.getX(), 0);
		assertEquals(ijPoint.getDoublePosition(1), omeroPoint.getY(), 0);
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, omeroPoint.getText());
	}

	@Test
	public void testUnwrapping() {
		final PointData omeroPoint = new PointData(103, 89.5);
		omeroPoint.setId(88);
		final OMEROPoint wrap = new DefaultOMEROPoint(omeroPoint);
		final PointData unwrap = convert.convert(wrap, PointData.class);

		assertEquals(omeroPoint.getId(), unwrap.getId());
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, unwrap.getText());
	}

	@Test
	public void testPointDataWithTextValue() {
		final PointData omeroPoint = new PointData(98, 31);
		omeroPoint.setId(74);
		omeroPoint.setText("Point");
		final PointMask ijPoint = convert.convert(omeroPoint, PointMask.class);

		assertEquals(BoundaryType.CLOSED, ijPoint.boundaryType());

		final PointData unwrap = convert.convert(ijPoint, PointData.class);
		assertEquals("Point " + ROIConverters.CLOSED_BOUNDARY_TEXT, unwrap
			.getText());
	}
}
