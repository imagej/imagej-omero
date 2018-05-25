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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.mask.MaskIntervalToMaskData;
import net.imagej.omero.roi.mask.RAIToMaskData;
import net.imagej.omero.roi.mask.RealMaskRealIntervalToMaskData;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.KnownConstant;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.composite.CompositeMaskPredicate;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.Sphere;
import net.imglib2.roi.mask.integer.DefaultMaskInterval;
import net.imglib2.type.logic.BoolType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;

import omero.gateway.model.MaskData;
import omero.gateway.model.ShapeData;

/**
 * Tests converting {@link CompositeMaskPredicate} to {@link MaskData}.
 *
 * @author Alison Walter
 */
public class CompositeMaskPredicateToMaskData {

	public ConvertService convert;

	@Before
	public void setup() {
		final Context c = new Context(OMEROService.class, LogService.class,
			ConvertService.class);
		convert = c.getService(ConvertService.class);
	}

	@After
	public void teardown() {
		convert.getContext().dispose();
	}

	@Test
	public void testAnd() {
		final Box b = GeomMasks.closedBox(new double[] { 0, 0 },
			new double[] { 20, 20 });
		final Box b2 = GeomMasks.closedBox(new double[] { 10, 10 },
			new double[] { 30, 30 });

		final RealMaskRealInterval and = b.and(b2);
		final Converter<?, ?> c = convert.getHandler(and, MaskData.class);
		assertTrue(c instanceof RealMaskRealIntervalToMaskData);

		final MaskData md = c.convert(and, MaskData.class);

		final RandomAccessibleInterval<BoolType> rai = Views.interval(Views.raster(
			Masks.toRealRandomAccessibleRealInterval(and)), Intervals
				.smallestContainingInterval(and));

		assertIsEqual(rai, md);
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, md.getText());
	}

	@Test
	public void testMinus() {
		final Sphere s = GeomMasks.closedSphere(new double[] { 30, 30 }, 5);
		final Sphere s2 = GeomMasks.closedSphere(new double[] { 30, 30 },
			2);

		final RealMaskRealInterval minus = s.minus(s2);
		final Converter<?, ?> c = convert.getHandler(minus, MaskData.class);
		assertTrue(c instanceof RealMaskRealIntervalToMaskData);

		final MaskData md = c.convert(minus, MaskData.class);

		final RandomAccessibleInterval<BoolType> rai = Views.interval(Views.raster(
			Masks.toRealRandomAccessibleRealInterval(minus)), Intervals
				.smallestContainingInterval(minus));

		assertIsEqual(rai, md);
		assertEquals(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, md.getText());
	}

	@Test
	public void testNegate() {
		final Box b = GeomMasks.closedBox(new double[] { 0, 0 },
			new double[] { 5, 6 });
		final RealMask negate = b.negate();

		final Converter<?, ?> c = convert.getHandler(negate, MaskData.class);
		assertNull(c); // needs an interval

		// say we gave it an interval, potentially the interval of the image it will
		// be attached to in OMERO

		final RandomAccessibleInterval<BoolType> rai = Views.interval(Views.raster(
			Masks.toRealRandomAccessible(negate)), new FinalInterval(new long[] { 0,
				0 }, new long[] { 100, 120 }));

		final Converter<?, ?> c2 = convert.getHandler(rai, MaskData.class);
		assertTrue(c2 instanceof RAIToMaskData);

		final MaskData md = c2.convert(rai, MaskData.class);

		assertIsEqual(rai, md);
		assertEquals(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, md.getText());
	}

	@Test
	public void testOr() {
		final Interval i = new FinalInterval(new long[] { 0, 0 }, new long[] { 23,
			17 });
		final Interval i2 = new FinalInterval(new long[] { 40, 123 }, new long[] {
			8, 30 });
		final MaskInterval mi = new DefaultMaskInterval(i, BoundaryType.CLOSED,
			t -> Intervals.contains(i, t), KnownConstant.UNKNOWN);
		final MaskInterval mi2 = new DefaultMaskInterval(i2, BoundaryType.CLOSED,
			t -> Intervals.contains(i2, t), KnownConstant.UNKNOWN);
		final MaskInterval or = mi.or(mi2);

		final Converter<?, ?> c = convert.getHandler(or, MaskData.class);
		assertTrue(c instanceof MaskIntervalToMaskData);

		final MaskData md = c.convert(or, MaskData.class);

		final RandomAccessibleInterval<BoolType> rai = Masks
			.toRandomAccessibleInterval(or);

		assertIsEqual(rai, md);
		assertEquals(ROIConverters.CLOSED_BOUNDARY_TEXT, md.getText());
	}

	@Test
	public void testXor() {
		final Ellipsoid e = GeomMasks.closedEllipsoid(new double[] { 30,
			25 }, new double[] { 15, 20 });
		final Sphere s = GeomMasks.closedSphere(new double[] { 14, 14 },
			13);
		final RealMaskRealInterval xor = e.xor(s);

		final Converter<?, ?> c = convert.getHandler(xor, MaskData.class);
		assertTrue(c instanceof RealMaskRealIntervalToMaskData);

		final MaskData md = c.convert(xor, MaskData.class);

		final RandomAccessibleInterval<BoolType> rai = Views.interval(Views.raster(
			Masks.toRealRandomAccessibleRealInterval(xor)), Intervals
				.smallestContainingInterval(xor));

		assertIsEqual(rai, md);
		assertEquals(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, md.getText());
	}

	@Test
	public void testMultipleOperands() {
		final Ellipsoid e = GeomMasks.closedEllipsoid(new double[] { 30,
			25 }, new double[] { 15, 20 });
		final Sphere s = GeomMasks.closedSphere(new double[] { 14, 14 },
			13);
		final Box b = GeomMasks.closedBox(new double[] { 0, 0 },
			new double[] { 5, 6 });
		final AffineTransform2D transform = new AffineTransform2D();
		transform.rotate(Math.PI / 3);

		final RealMaskRealInterval rmri = e.or(s.xor(b.transform(transform
			.inverse())));
		final Converter<?, ?> c = convert.getHandler(rmri, ShapeData.class);
		assertTrue(c instanceof RealMaskRealIntervalToMaskData);

		final MaskData md = c.convert(rmri, MaskData.class);

		final RandomAccessibleInterval<BoolType> rai = Views.interval(Views.raster(
			Masks.toRealRandomAccessibleRealInterval(rmri)), Intervals
				.smallestContainingInterval(rmri));

		assertIsEqual(rai, md);
		// NB: transform is only preserved if it is the only operation
		assertNull(md.getTransform());
		assertEquals(ROIConverters.UNSPECIFIED_BOUNDARY_TEXT, md.getText());
	}

	// -- Helper methods --

	private void assertIsEqual(final RandomAccessibleInterval<BoolType> rai,
		final MaskData md)
	{
		assertEquals(rai.min(0), md.getX(), 0);
		assertEquals(rai.min(1), md.getY(), 0);
		assertEquals(rai.dimension(0) - 1, md.getWidth(), 0);
		assertEquals(rai.dimension(1) - 1, md.getHeight(), 0);

		final RandomAccess<BoolType> ra = rai.randomAccess();
		final byte[] bytes = md.getMask();

		for (long r = rai.min(1); r < rai.max(1); r++) {
			ra.setPosition(r, 1);
			for (long c = rai.min(0); c < rai.max(0); c++) {
				ra.setPosition(c, 0);
				final int omeroBitPos = (int) (((r - md.getY()) * md.getWidth()) + (c -
					md.getX()));
				assertEquals(ra.get().get(), md.getBit(bytes, omeroBitPos) == 1);
			}
		}
	}
}
