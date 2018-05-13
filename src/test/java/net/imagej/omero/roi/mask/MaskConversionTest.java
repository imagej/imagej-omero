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

package net.imagej.omero.roi.mask;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.KnownConstant;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.mask.integer.DefaultMaskInterval;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.MaskData;

/**
 * Tests {@link OMEROToImageJMask}, {@link MaskIntervalToMaskData}, and
 * {@link OMEROMaskUnwrapper}.
 *
 * @author Alison Walter
 */
public class MaskConversionTest {

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
		final MaskInterval ijMask = new DefaultMaskInterval(new FinalInterval(
			new long[] { 30, 15 }, new long[] { 90, 75 }), BoundaryType.UNSPECIFIED,
			t -> t.getDoublePosition(0) % 2 == 0, KnownConstant.UNKNOWN);
		final MaskInterval fiveD = Masks.emptyMaskInterval(5);
		final MaskData omeroMask = new MaskData();
		final OMEROMask wrap = new DefaultOMEROMask(omeroMask);

		final Converter<?, ?> cOne = convert.getHandler(ijMask, MaskData.class);
		assertTrue(cOne instanceof MaskIntervalToMaskData);

		final Converter<?, ?> cTwo = convert.getHandler(fiveD, MaskData.class);
		assertNull(cTwo);

		final Converter<?, ?> cThree = convert.getHandler(omeroMask,
			RealMaskRealInterval.class);
		assertTrue(cThree instanceof OMEROToImageJMask);

		final Converter<?, ?> cFour = convert.getHandler(wrap, MaskData.class);
		assertTrue(cFour instanceof OMEROMaskUnwrapper);
	}

	@Test
	public void testOMEROToImageJ() {
		// 0 1 0 1 0
		// 1 1 0 0 0
		// 0 0 1 0 1
		// 0 0 0 0 0
		final byte[] data = new byte[] { 86, 10, 0 };
		final MaskData omeroMask = new MaskData(12, 11, 5, 4, data);
		final RealMaskRealInterval ijMask = convert.convert(omeroMask,
			RealMaskRealInterval.class);

		assertEquals(BoundaryType.UNSPECIFIED, ijMask.boundaryType());
		assertEquals(omeroMask.getX(), ijMask.realMin(0), 0);
		assertEquals(omeroMask.getY(), ijMask.realMin(1), 0);
		assertEquals(omeroMask.getWidth(), ijMask.realMax(0) - ijMask.realMin(0),
			0);
		assertEquals(omeroMask.getHeight(), ijMask.realMax(1) - ijMask.realMin(1),
			0);

		final RealPoint test = new RealPoint(2);
		for (int r = 0; r < (int) omeroMask.getHeight(); r++)
			for (int c = 0; c < (int) omeroMask.getWidth(); c++) {
				test.setPosition(c + omeroMask.getX(), 0);
				test.setPosition(r + omeroMask.getY(), 1);
				final byte omeroBit = omeroMask.getBit(data, (int) (r * omeroMask
					.getWidth() + c));
				assertEquals(omeroBit == 1, ijMask.test(test));
			}
	}

	@Test
	public void testImageJToOMERODivisibleBy8() {
		final MaskInterval ijMask = new DefaultMaskInterval(new FinalInterval(
			new long[] { 18, 36 }, new long[] { 54, 92 }), BoundaryType.UNSPECIFIED,
			t -> (t.getDoublePosition(0) + t.getDoublePosition(1)) % 2 == 0,
			KnownConstant.UNKNOWN);
		final MaskData omeroMask = convert.convert(ijMask, MaskData.class);

		assertEquals(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, omeroMask.getText());
		assertEquals(ijMask.min(0), omeroMask.getX(), 0);
		assertEquals(ijMask.min(1), omeroMask.getY(), 0);
		assertEquals(ijMask.max(0), omeroMask.getX() + omeroMask.getWidth(), 0);
		assertEquals(ijMask.max(1), omeroMask.getY() + omeroMask.getHeight(), 0);

		final Point pt = new Point(2);
		final byte[] data = omeroMask.getMask();
		for (long r = ijMask.min(1); r < ijMask.max(1); r++) {
			pt.setPosition(r, 1);
			for (long c = ijMask.min(0); c < ijMask.max(0); c++) {
				pt.setPosition(c, 0);
				final int omeroBitPos = (int) (((r - omeroMask.getY()) * omeroMask
					.getWidth()) + (c - omeroMask.getX()));
				assertEquals(ijMask.test(pt), omeroMask.getBit(data, omeroBitPos) == 1);
			}
		}
	}

	@Test
	public void testImageJToOMERONotDivisibleBy8() {
		// 1 0 0 0 0
		// 0 1 0 0 0
		// 0 0 1 0 0
		// 0 0 0 1 0
		// 0 0 0 0 1
		// bytes = -126, 8, 32, -128
		final MaskInterval ijMask = new DefaultMaskInterval(new FinalInterval(
			new long[] { 0, 0 }, new long[] { 5, 5 }), BoundaryType.UNSPECIFIED,
			t -> t.getDoublePosition(0) == t.getDoublePosition(1),
			KnownConstant.UNKNOWN);
		final MaskData omeroMask = convert.convert(ijMask, MaskData.class);

		assertEquals(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, omeroMask.getText());
		assertEquals(ijMask.min(0), omeroMask.getX(), 0);
		assertEquals(ijMask.min(1), omeroMask.getY(), 0);
		assertEquals(ijMask.max(0), omeroMask.getX() + omeroMask.getWidth(), 0);
		assertEquals(ijMask.max(1), omeroMask.getY() + omeroMask.getHeight(), 0);

		final Point pt = new Point(2);
		final byte[] data = omeroMask.getMask();
		for (long r = ijMask.min(1); r < ijMask.max(1); r++) {
			pt.setPosition(r, 1);
			for (long c = ijMask.min(0); c < ijMask.max(0); c++) {
				pt.setPosition(c, 0);
				final int omeroBitPos = (int) (((r - omeroMask.getY()) * omeroMask
					.getWidth()) + (c - omeroMask.getX()));
				assertEquals(ijMask.test(pt), omeroMask.getBit(data, omeroBitPos) == 1);
			}
		}
	}

	@Test
	public void testUnwrapping() {
		final MaskData omeroMask = new MaskData(0, 0, 20, 4, new byte[] { -10, 8, 0,
			100, 36, 92, 12, -126, -7, 9 });
		omeroMask.setId(89);
		omeroMask.setText("text");
		final OMEROMask wrap = new DefaultOMEROMask(omeroMask);
		final MaskData unwrap = convert.convert(wrap, MaskData.class);

		assertArrayEquals(omeroMask.getMask(), unwrap.getMask());
		assertEquals(omeroMask.getId(), unwrap.getId());
		assertEquals("text " + ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, unwrap
			.getText());
	}

	@Test
	public void testMaskDataWithTextValue() {
		final MaskData omeroMask = new MaskData(0, 0, 20, 4, new byte[] { -10, 8, 0,
			100, 36, 92, 12, -126, -7, 9 });
		omeroMask.setId(89);
		omeroMask.setText("text " + ROIConverters.CLOSED_BOUNDARY_TEXT);

		final RealMaskRealInterval ijMask = convert.convert(omeroMask,
			RealMaskRealInterval.class);
		assertEquals(BoundaryType.UNSPECIFIED, ijMask.boundaryType());

		final MaskData unwrap = convert.convert(ijMask, MaskData.class);
		assertEquals("text " + ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, unwrap
			.getText());
	}
}
