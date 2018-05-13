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

package net.imagej.omero.roi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.OMEROROICollection;
import net.imagej.omero.roi.OMEROROIElement;
import net.imagej.omero.roi.ROIConverters;
import net.imagej.omero.roi.ROIDataToOMEROROICollection;
import net.imagej.omero.roi.ellipse.OMEROEllipse;
import net.imagej.omero.roi.ellipse.OpenOMEROEllipse;
import net.imagej.omero.roi.point.OMEROPoint;
import net.imagej.omero.roi.rectangle.OMERORectangle;
import net.imagej.omero.roi.transform.TransformedOMERORealMaskRealInterval;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;
import org.scijava.util.TreeNode;

import omero.gateway.model.EllipseData;
import omero.gateway.model.PointData;
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.model.AffineTransform;
import omero.model.AffineTransformI;

/**
 * Tests converting {@link ROIData} to {@link TreeNode}.
 *
 * @author Alison Walter
 */
public class ROIDataToTreeNodeConversionTest {

	private ConvertService convert;

	@Before
	public void setup() {
		final Context c = new Context(ConvertService.class, OMEROService.class,
			LogService.class);
		convert = c.getService(ConvertService.class);
	}

	@After
	public void teardown() {
		convert.getContext().dispose();
	}

	@Test
	public void testConverterMatching() {
		final ROIData omeroRoi = new ROIData();
		omeroRoi.setId(13);
		omeroRoi.addShapeData(new PointData(1, 1));

		final Converter<?, ?> match = convert.getHandler(omeroRoi, TreeNode.class);
		assertTrue(match instanceof ROIDataToOMEROROICollection);
	}

	@Test
	public void testSingleShape() {
		final ROIData omeroRoi = new ROIData();
		omeroRoi.setId(98);
		final EllipseData e = new EllipseData(10, 13.5, 6.5, 9);
		e.setZ(0);
		e.setT(9);
		omeroRoi.addShapeData(e);
		final TreeNode<?> ijDN = convert.convert(omeroRoi, TreeNode.class);

		assertTrue(ijDN instanceof OMEROROICollection);
		assertNull(ijDN.parent());

		final OMEROROICollection ijORC = (OMEROROICollection) ijDN;
		assertEquals(98, ijORC.data().getId());

		final List<TreeNode<?>> children = ijORC.children();
		assertTrue(children.size() == 1);
		assertTrue(children.get(0) instanceof OMEROROIElement);

		final OMEROROIElement ijORE = (OMEROROIElement) children.get(0);
		assertTrue(ijORE.parent() instanceof OMEROROICollection);
		assertTrue(ijORE.children().isEmpty());
		assertTrue(ijORE.data() instanceof OMEROEllipse);
	}

	@Test
	public void testMultiShapeSinglePlane() {
		final ROIData omeroRoi = new ROIData();
		omeroRoi.setId(92);
		final PointData pOne = new PointData(11, 12);
		pOne.setZ(0);
		pOne.setT(0);
		final PointData pTwo = new PointData(0.25, 84);
		pTwo.setZ(0);
		pTwo.setT(0);
		final RectangleData r = new RectangleData(12, 15, 9, 4.5);
		r.setZ(0);
		r.setT(0);
		omeroRoi.addShapeData(pOne);
		omeroRoi.addShapeData(pTwo);
		omeroRoi.addShapeData(r);

		final TreeNode<?> ijDN = convert.convert(omeroRoi, TreeNode.class);

		assertTrue(ijDN instanceof OMEROROICollection);
		assertNull(ijDN.parent());

		final OMEROROICollection ijORC = (OMEROROICollection) ijDN;
		assertEquals(92, ijORC.data().getId());

		final List<TreeNode<?>> children = ijORC.children();
		assertTrue(children.size() == 3);
		for (final TreeNode<?> child : children)
			assertTrue(child instanceof OMEROROIElement);

		final OMEROROIElement ijORE1 = (OMEROROIElement) children.get(0);
		assertTrue(ijORE1.parent() instanceof OMEROROICollection);
		assertTrue(ijORE1.children().isEmpty());
		assertTrue(ijORE1.data() instanceof OMEROPoint);

		final OMEROROIElement ijORE2 = (OMEROROIElement) children.get(1);
		assertTrue(ijORE2.parent() instanceof OMEROROICollection);
		assertTrue(ijORE2.children().isEmpty());
		assertTrue(ijORE2.data() instanceof OMEROPoint);

		final OMEROROIElement ijORE3 = (OMEROROIElement) children.get(2);
		assertTrue(ijORE3.parent() instanceof OMEROROICollection);
		assertTrue(ijORE3.children().isEmpty());
		assertTrue(ijORE3.data() instanceof OMERORectangle);
	}

	@Test
	public void testMultiShapeMultiPlane() {
		final ROIData omeroRoi = new ROIData();
		omeroRoi.setId(2);
		final RectangleData rOne = new RectangleData(1, 3, 6, 9);
		rOne.setZ(0);
		rOne.setT(0);
		final RectangleData rTwo = new RectangleData(1, 3, 6, 9);
		rTwo.setZ(0);
		rTwo.setT(1);
		final RectangleData rThree = new RectangleData(1, 3, 6, 9);
		rThree.setZ(0);
		rThree.setT(2);
		final RectangleData rFour = new RectangleData(1, 3, 6, 9);
		rFour.setZ(0);
		rFour.setT(3);
		omeroRoi.addShapeData(rOne);
		omeroRoi.addShapeData(rTwo);
		omeroRoi.addShapeData(rThree);
		omeroRoi.addShapeData(rFour);

		final TreeNode<?> ijDN = convert.convert(omeroRoi, TreeNode.class);

		assertTrue(ijDN instanceof OMEROROICollection);
		assertNull(ijDN.parent());

		final OMEROROICollection ijORC = (OMEROROICollection) ijDN;
		assertEquals(2, ijORC.data().getId());

		final List<TreeNode<?>> children = ijORC.children();
		assertTrue(children.size() == 4);
		for (final TreeNode<?> child : children)
			assertTrue(child instanceof OMEROROIElement);

		final OMEROROIElement ijORE1 = (OMEROROIElement) children.get(0);
		assertTrue(ijORE1.parent() instanceof OMEROROICollection);
		assertTrue(ijORE1.children().isEmpty());
		assertTrue(ijORE1.data() instanceof OMERORectangle);

		final OMEROROIElement ijORE2 = (OMEROROIElement) children.get(1);
		assertTrue(ijORE2.parent() instanceof OMEROROICollection);
		assertTrue(ijORE2.children().isEmpty());
		assertTrue(ijORE2.data() instanceof OMERORectangle);

		final OMEROROIElement ijORE3 = (OMEROROIElement) children.get(2);
		assertTrue(ijORE3.parent() instanceof OMEROROICollection);
		assertTrue(ijORE3.children().isEmpty());
		assertTrue(ijORE3.data() instanceof OMERORectangle);

		final OMEROROIElement ijORE4 = (OMEROROIElement) children.get(3);
		assertTrue(ijORE4.parent() instanceof OMEROROICollection);
		assertTrue(ijORE4.children().isEmpty());
		assertTrue(ijORE4.data() instanceof OMERORectangle);
	}

	@Test
	public void testShapeWithBoundaryType() {
		final ROIData omeroRoi = new ROIData();
		omeroRoi.setId(98);
		final EllipseData e = new EllipseData(10, 10, 2, 2);
		e.setId(798);
		e.setText(ROIConverters.OPEN_BOUNDARY_TEXT);
		e.setZ(0);
		e.setT(0);
		omeroRoi.addShapeData(e);

		final TreeNode<?> ijDN = convert.convert(omeroRoi, TreeNode.class);

		assertTrue(ijDN instanceof OMEROROICollection);
		assertNull(ijDN.parent());

		final OMEROROICollection ijORC = (OMEROROICollection) ijDN;
		assertEquals(98, ijORC.data().getId());

		final List<TreeNode<?>> children = ijORC.children();
		assertTrue(children.size() == 1);
		assertTrue(children.get(0) instanceof OMEROROIElement);

		final OMEROROIElement ijORE = (OMEROROIElement) children.get(0);
		assertTrue(ijORE.parent() instanceof OMEROROICollection);
		assertTrue(ijORE.children().isEmpty());
		assertTrue(ijORE.data() instanceof OpenOMEROEllipse);
	}

	@Test
	public void testTransformedSingleShape() {
		final ROIData omeroRoi = new ROIData();
		omeroRoi.setId(34);
		final RectangleData r = new RectangleData(1, 1, 10, 13);
		final AffineTransform affine = new AffineTransformI();
		affine.setA00(omero.rtypes.rdouble(1));
		affine.setA01(omero.rtypes.rdouble(0));
		affine.setA02(omero.rtypes.rdouble(2.5));
		affine.setA10(omero.rtypes.rdouble(0));
		affine.setA11(omero.rtypes.rdouble(1));
		affine.setA12(omero.rtypes.rdouble(5));
		r.setTransform(affine);
		r.setZ(0);
		r.setT(0);
		omeroRoi.addShapeData(r);

		final TreeNode<?> ijDN = convert.convert(omeroRoi, TreeNode.class);

		assertTrue(ijDN instanceof OMEROROICollection);
		assertNull(ijDN.parent());

		final OMEROROICollection ijORC = (OMEROROICollection) ijDN;
		assertEquals(34, ijORC.data().getId());

		final List<TreeNode<?>> children = ijORC.children();
		assertTrue(children.size() == 1);
		assertTrue(children.get(0) instanceof OMEROROIElement);

		final OMEROROIElement ijORE = (OMEROROIElement) children.get(0);
		assertTrue(ijORE.parent() instanceof OMEROROICollection);
		assertTrue(ijORE.children().isEmpty());
		assertTrue(ijORE.data() instanceof TransformedOMERORealMaskRealInterval);

		final TransformedOMERORealMaskRealInterval<?> ijTransformed =
			(TransformedOMERORealMaskRealInterval<?>) ijORE.data();

		// NB: Don't call getSource(), this returns the transformed interval only!
		assertTrue(ijTransformed.arg0() instanceof OMERORectangle);
	}

	@Test
	public void testTransformedShapeInMultiShape() {
		final ROIData omeroRoi = new ROIData();
		omeroRoi.setId(34);
		final RectangleData r = new RectangleData(1, 1, 10, 13);
		final AffineTransform affine = new AffineTransformI();
		affine.setA00(omero.rtypes.rdouble(1));
		affine.setA01(omero.rtypes.rdouble(0));
		affine.setA02(omero.rtypes.rdouble(2.5));
		affine.setA10(omero.rtypes.rdouble(0));
		affine.setA11(omero.rtypes.rdouble(1));
		affine.setA12(omero.rtypes.rdouble(5));
		r.setTransform(affine);
		r.setZ(0);
		r.setT(0);
		final EllipseData e = new EllipseData(12, 13, 5, 2);
		e.setZ(0);
		e.setT(0);
		final EllipseData e2 = new EllipseData(12, 13, 5, 2);
		e2.setZ(0);
		e2.setT(0);
		omeroRoi.addShapeData(r);
		omeroRoi.addShapeData(e);
		omeroRoi.addShapeData(e2);

		final TreeNode<?> ijDN = convert.convert(omeroRoi, TreeNode.class);

		assertTrue(ijDN instanceof OMEROROICollection);
		assertNull(ijDN.parent());

		final OMEROROICollection ijORC = (OMEROROICollection) ijDN;
		assertEquals(34, ijORC.data().getId());

		final List<TreeNode<?>> children = ijORC.children();
		assertTrue(children.size() == 3);
		for (final TreeNode<?> child : children)
			assertTrue(child instanceof OMEROROIElement);

		final OMEROROIElement ijORE = (OMEROROIElement) children.get(0);
		assertTrue(ijORE.parent() instanceof OMEROROICollection);
		assertTrue(ijORE.children().isEmpty());
		assertTrue(ijORE.data() instanceof TransformedOMERORealMaskRealInterval);

		final TransformedOMERORealMaskRealInterval<?> ijTransformed =
			(TransformedOMERORealMaskRealInterval<?>) ijORE.data();

		// NB: Don't call getSource(), this returns the transformed interval only!
		assertTrue(ijTransformed.arg0() instanceof OMERORectangle);

		final OMEROROIElement ijORE2 = (OMEROROIElement) children.get(1);
		assertTrue(ijORE2.parent() instanceof OMEROROICollection);
		assertTrue(ijORE2.children().isEmpty());
		assertTrue(ijORE2.data() instanceof OMEROEllipse);

		final OMEROROIElement ijORE3 = (OMEROROIElement) children.get(2);
		assertTrue(ijORE3.parent() instanceof OMEROROICollection);
		assertTrue(ijORE3.children().isEmpty());
		assertTrue(ijORE3.data() instanceof OMEROEllipse);
	}
}
