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

package net.imagej.omero.roi.ellipse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.ellipse.ClosedOMEROEllipse;
import net.imagej.omero.roi.ellipse.ImageJToOMEROEllipse;
import net.imagej.omero.roi.ellipse.OMEROEllipse;
import net.imagej.omero.roi.ellipse.OMEROEllipseUnwrapper;
import net.imagej.omero.roi.ellipse.OMEROToImageJEllipse;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.geom.real.ClosedWritableEllipsoid;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.OpenWritableEllipsoid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.EllipseData;
import omero.gateway.model.ShapeData;

/**
 * Tests {@link OMEROToImageJEllipse}, {@link ImageJToOMEROEllipse}, and
 * {@link OMEROEllipseUnwrapper}.
 *
 * @author Alison Walter
 */
public class EllipseConversionTest {

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
		final Ellipsoid ij = new ClosedWritableEllipsoid(new double[] { 12, 12 },
			new double[] { 13, 7.5 });
		final Converter<?, ?> cOne = convert.getHandler(ij, ShapeData.class);
		assertTrue(cOne instanceof ImageJToOMEROEllipse);

		final Ellipsoid threeD = new ClosedWritableEllipsoid(new double[] { 8, 9,
			10 }, new double[] { 1, 2, 3 });
		final Converter<?, ?> cTwo = convert.getHandler(threeD, ShapeData.class);
		assertNull(cTwo);

		final EllipseData omero = new EllipseData(10, 11, 5, 6.5);
		final Converter<?, ?> cThree = convert.getHandler(omero,
			MaskPredicate.class);
		assertTrue(cThree instanceof OMEROToImageJEllipse);

		final OMEROEllipse wrapped = new ClosedOMEROEllipse(omero);
		final Converter<?, ?> cFour = convert.getHandler(wrapped, ShapeData.class);
		assertTrue(cFour instanceof OMEROEllipseUnwrapper);
	}

	@Test
	public void testOMEROToImageJ() {
		final EllipseData omeroEllipse = new EllipseData(12.5, 6, 8, 5.5);
		final Ellipsoid ijEllipse = convert.convert(omeroEllipse, Ellipsoid.class);

		assertTrue(ijEllipse instanceof OMEROEllipse);
		assertTrue(ijEllipse.boundaryType() == BoundaryType.CLOSED);
		assertEquals(2, ijEllipse.numDimensions());
		assertEquals(omeroEllipse.getX(), ijEllipse.center().getDoublePosition(0),
			0);
		assertEquals(omeroEllipse.getY(), ijEllipse.center().getDoublePosition(1),
			0);
		assertEquals(omeroEllipse.getRadiusX(), ijEllipse.semiAxisLength(0), 0);
		assertEquals(omeroEllipse.getRadiusY(), ijEllipse.semiAxisLength(1), 0);
	}

	@Test
	public void testImageJToOMEROClosedEllipse() {
		final Ellipsoid ijEllipse = new ClosedWritableEllipsoid(new double[] { 5.5,
			3 }, new double[] { 2, 0.5 });
		final EllipseData omeroEllipse = convert.convert(ijEllipse,
			EllipseData.class);

		assertEquals(ijEllipse.center().getDoublePosition(0), omeroEllipse.getX(),
			0);
		assertEquals(ijEllipse.center().getDoublePosition(1), omeroEllipse.getY(),
			0);
		assertEquals(ijEllipse.semiAxisLength(0), omeroEllipse.getRadiusX(), 0);
		assertEquals(ijEllipse.semiAxisLength(1), omeroEllipse.getRadiusY(), 0);
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, omeroEllipse.getText());
		assertEquals(-1, omeroEllipse.getZ());
		assertEquals(-1, omeroEllipse.getT());
		assertEquals(-1, omeroEllipse.getC());
	}

	@Test
	public void testImageJToOMEROOpenEllipse() {
		final Ellipsoid ijEllipse = new OpenWritableEllipsoid(new double[] { 90,
			52.25 }, new double[] { 8, 7 });
		final EllipseData omeroEllipse = convert.convert(ijEllipse,
			EllipseData.class);

		assertEquals(ijEllipse.center().getDoublePosition(0), omeroEllipse.getX(),
			0);
		assertEquals(ijEllipse.center().getDoublePosition(1), omeroEllipse.getY(),
			0);
		assertEquals(ijEllipse.semiAxisLength(0), omeroEllipse.getRadiusX(), 0);
		assertEquals(ijEllipse.semiAxisLength(1), omeroEllipse.getRadiusY(), 0);
		assertEquals(ROIConverters.OPEN_BOUNDARY_TEXT, omeroEllipse.getText());
		assertEquals(-1, omeroEllipse.getZ());
		assertEquals(-1, omeroEllipse.getT());
		assertEquals(-1, omeroEllipse.getC());
	}

	@Test
	public void testUnwrapping() {
		final EllipseData omeroEllipse = new EllipseData(10, 22.5, 2, 8);
		omeroEllipse.setId(900);
		omeroEllipse.setText("ellipse");
		final OMEROEllipse wrap = new ClosedOMEROEllipse(omeroEllipse);
		final EllipseData unwrapped = convert.convert(wrap, EllipseData.class);

		assertEquals(omeroEllipse.getX(), unwrapped.getX(), 0);
		assertEquals(omeroEllipse.getY(), unwrapped.getY(), 0);
		assertEquals(omeroEllipse.getRadiusX(), unwrapped.getRadiusX(), 0);
		assertEquals(omeroEllipse.getRadiusY(), unwrapped.getRadiusY(), 0);
		assertEquals("ellipse " + ROIConverters.CLOSED_BOUNDARY_TEXT, unwrapped
			.getText());
		assertEquals(omeroEllipse.getId(), unwrapped.getId());
	}

	@Test
	public void testEllipseDataWithTextValue() {
		final EllipseData omeroEllipse = new EllipseData(13, 6, 2.5, 0.25);
		omeroEllipse.setText("ellipse" + ROIConverters.OPEN_BOUNDARY_TEXT +
			" stuff");
		final Ellipsoid ijEllipse = convert.convert(omeroEllipse, Ellipsoid.class);

		assertEquals(ijEllipse.boundaryType(), BoundaryType.OPEN);

		final EllipseData unwrapped = convert.convert(ijEllipse, EllipseData.class);
		assertEquals("ellipse" + ROIConverters.OPEN_BOUNDARY_TEXT + " stuff",
			unwrapped.getText());
	}
}
