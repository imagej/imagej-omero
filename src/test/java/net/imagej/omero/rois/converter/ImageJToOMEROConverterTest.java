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

package net.imagej.omero.rois.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.list.ListImg;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.ClosedBox;
import net.imglib2.roi.geom.real.ClosedSphere;
import net.imglib2.roi.geom.real.ClosedSuperEllipsoid;
import net.imglib2.roi.geom.real.DefaultLine;
import net.imglib2.roi.geom.real.DefaultPointMask;
import net.imglib2.roi.geom.real.DefaultPolygon2D;
import net.imglib2.roi.geom.real.DefaultPolyline;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.Line;
import net.imglib2.roi.geom.real.OpenBox;
import net.imglib2.roi.geom.real.OpenEllipsoid;
import net.imglib2.roi.geom.real.OpenSphere;
import net.imglib2.roi.geom.real.PointMask;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.Polyline;
import net.imglib2.roi.geom.real.Sphere;
import net.imglib2.roi.geom.real.SuperEllipsoid;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scijava.Context;
import org.scijava.convert.ConvertService;

import omero.gateway.model.EllipseData;
import omero.gateway.model.LineData;
import omero.gateway.model.MaskData;
import omero.gateway.model.PointData;
import omero.gateway.model.PolygonData;
import omero.gateway.model.PolylineData;
import omero.gateway.model.ROIData;
import omero.gateway.model.RectangleData;
import omero.gateway.model.ShapeData;
import omero.model.EllipseI;
import omero.model.LineI;
import omero.model.MaskI;
import omero.model.PointI;
import omero.model.PolygonI;
import omero.model.PolylineI;
import omero.model.RectangleI;
import omero.model.RoiI;
import omero.model.ShapeAnnotationLink;
import omero.model.TagAnnotation;

/**
 * Tests converters for going from ImageJ masks to OMERO Rois.
 *
 * @author Alison Walter
 */
public class ImageJToOMEROConverterTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	private ConvertService convertService;

	@Before
	public void setUp() {
		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	@Test
	public void testClosedBox() {
		final Box<RealPoint> b = new ClosedBox(new double[] { 0, 0 }, new double[] {
			30, 30 });
		final ROIData rd = convertService.convert(b, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof RectangleData);
		final RectangleData r = (RectangleData) s.get(0);

		assertEquals(r.getZ(), 0);
		assertEquals(r.getT(), 0);
		assertEquals(r.getC(), -1);
		assertEquals(r.getX(), 0, 0);
		assertEquals(r.getY(), 0, 0);
		assertEquals(r.getWidth(), 30, 0);
		assertEquals(r.getHeight(), 30, 0);

		final List<ShapeAnnotationLink> annotations = ((RectangleI) r.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "closed");
	}

	@Test
	public void testOpenBox() {
		final Box<RealPoint> b = new OpenBox(new double[] { 5.25, 2 },
			new double[] { 75.25, 30 });
		final ROIData rd = convertService.convert(b, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof RectangleData);
		final RectangleData r = (RectangleData) s.get(0);

		assertEquals(r.getZ(), 0);
		assertEquals(r.getT(), 0);
		assertEquals(r.getC(), -1);
		assertEquals(r.getX(), 5.25, 0);
		assertEquals(r.getY(), 2, 0);
		assertEquals(r.getWidth(), 70, 0);
		assertEquals(r.getHeight(), 28, 0);

		final List<ShapeAnnotationLink> annotations = ((RectangleI) r.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "open");
	}

	@Test
	public void testEllipsoid() {
		final Ellipsoid<RealPoint> e = new OpenEllipsoid(new double[] { 15, 15 },
			new double[] { 4.5, 10 });
		final ROIData rd = convertService.convert(e, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof EllipseData);
		final EllipseData ed = (EllipseData) s.get(0);

		assertEquals(ed.getZ(), 0);
		assertEquals(ed.getT(), 0);
		assertEquals(ed.getC(), -1);
		assertEquals(ed.getX(), 15, 0);
		assertEquals(ed.getY(), 15, 0);
		assertEquals(ed.getRadiusX(), 4.5, 0);
		assertEquals(ed.getRadiusY(), 10, 0);

		final List<ShapeAnnotationLink> annotations = ((EllipseI) ed.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "open");
	}

	@Test
	public void testLine() {
		final Line<RealPoint> l = new DefaultLine(new double[] { 20.25, 20.25 },
			new double[] { 6.125, -3 }, false);
		final ROIData rd = convertService.convert(l, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof LineData);
		final LineData ld = (LineData) s.get(0);

		assertEquals(ld.getZ(), 0);
		assertEquals(ld.getT(), 0);
		assertEquals(ld.getC(), -1);
		assertEquals(ld.getX1(), 20.25, 0);
		assertEquals(ld.getY1(), 20.25, 0);
		assertEquals(ld.getX2(), 6.125, 0);
		assertEquals(ld.getY2(), -3, 0);

		final List<ShapeAnnotationLink> annotations = ((LineI) ld.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "closed");
	}

	@Test
	public void testBitTypeMask() {
		// 1 1 0 0 0
		// 0 0 0 1 0
		// 0 0 0 0 0
		// 0 0 0 0 0
		// 0 0 1 0 1
		final ArrayImg<BitType, LongArray> img = ArrayImgs.bits(new long[] { 5,
			5 });
		final RandomAccess<BitType> ra = img.randomAccess();
		ra.setPosition(new int[] { 0, 0 });
		ra.get().set(true);
		ra.setPosition(new int[] { 1, 0 });
		ra.get().set(true);
		ra.setPosition(new int[] { 2, 4 });
		ra.get().set(true);
		ra.setPosition(new int[] { 3, 1 });
		ra.get().set(true);
		ra.setPosition(new int[] { 4, 4 });
		ra.get().set(true);
		final MaskInterval m = Masks.toMaskInterval(img);

		final ROIData rd = convertService.convert(m, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof MaskData);
		final MaskData md = (MaskData) s.get(0);

		assertEquals(md.getZ(), 0);
		assertEquals(md.getT(), 0);
		assertEquals(md.getC(), -1);
		assertEquals(md.getX(), 0, 0);
		assertEquals(md.getY(), 0, 0);
		assertEquals(md.getWidth(), 5, 0);
		assertEquals(md.getHeight(), 5, 0);

		final Cursor<BitType> c = img.cursor();
		final byte[] data = md.getMask();
		int count = 0;
		while (c.hasNext()) {
			final boolean val = md.getBit(data, count) == 1;
			assertEquals(c.next().get(), val);
			count++;
		}

		final List<ShapeAnnotationLink> annotations = ((MaskI) md.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "unspecified");
	}

	@Test
	public void testBoolTypeMask() {
		final ArrayList<BoolType> v = new ArrayList<>();
		v.add(new BoolType(false));
		v.add(new BoolType(false));
		v.add(new BoolType(true));
		v.add(new BoolType(false));
		final ListImg<BoolType> img = new ListImg<>(v, new long[] { 2, 2 });
		final MaskInterval m = Masks.toMaskInterval(img);

		final ROIData rd = convertService.convert(m, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof MaskData);
		final MaskData md = (MaskData) s.get(0);

		assertEquals(md.getZ(), 0);
		assertEquals(md.getT(), 0);
		assertEquals(md.getC(), -1);
		assertEquals(md.getX(), 0, 0);
		assertEquals(md.getY(), 0, 0);
		assertEquals(md.getWidth(), 2, 0);
		assertEquals(md.getHeight(), 2, 0);

		final byte[] data = md.getMask();
		assertTrue(data.length == 1);
		assertEquals(md.getBit(data, 0), 0);
		assertEquals(md.getBit(data, 1), 0);
		assertEquals(md.getBit(data, 2), 1);
		assertEquals(md.getBit(data, 3), 0);

		final List<ShapeAnnotationLink> annotations = ((MaskI) md.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "unspecified");
	}

	@Test
	public void testCellImgMask() {
		// 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
		// 0 0 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0
		// 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0
		// 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0
		// 0 0 0 0 0 0 0 0 1 1 1 1 1 0 0 0 0 0 0 0
		// 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
		// 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
		// 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
		// 0 1 0 0 0 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0
		// 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1
		final CellImgFactory<BitType> factory = new CellImgFactory<>();
		final CellImg<BitType, ?> cimg = factory.create(new long[] { 20, 10 },
			new BitType());
		final RandomAccess<BitType> ra = cimg.randomAccess();
		final int[][] pos = new int[][] { { 2, 1 }, { 3, 1 }, { 4, 1 }, { 5, 1 }, {
			6, 1 }, { 6, 2 }, { 7, 2 }, { 8, 2 }, { 8, 3 }, { 8, 4 }, { 9, 4 }, { 10,
				4 }, { 11, 4 }, { 12, 4 }, { 1, 8 }, { 5, 8 }, { 7, 8 }, { 0, 9 }, { 1,
					9 }, { 2, 9 }, { 3, 9 }, { 4, 9 }, { 5, 9 }, { 6, 9 }, { 7, 9 }, { 8,
						9 }, { 9, 9 }, { 10, 9 }, { 11, 9 }, { 12, 9 }, { 13, 9 }, { 14,
							9 }, { 15, 9 }, { 16, 9 }, { 17, 9 }, { 18, 9 }, { 19, 9 } };
		for (int i = 0; i < pos.length; i++) {
			ra.setPosition(pos[i]);
			ra.get().set(true);
		}

		final MaskInterval m = Masks.toMaskInterval(cimg);

		final ROIData rd = convertService.convert(m, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof MaskData);
		final MaskData md = (MaskData) s.get(0);
		final byte[] data = md.getMask();
		final int[] position = new int[2];

		for (int y = 0; y < 10; y++) {
			position[1] = y;
			for (int x = 0; x < 20; x++) {
				position[0] = x;
				ra.setPosition(position);
				final boolean value = md.getBit(data, y * 20 + x) == 1;
				assertEquals(ra.get().get(), value);
			}
		}
	}

	@Test
	public void testPoint() {
		final PointMask p = new DefaultPointMask(new double[] { 1.125, -3.03125 });
		final ROIData rd = convertService.convert(p, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof PointData);
		final PointData pd = (PointData) s.get(0);

		assertEquals(pd.getZ(), 0);
		assertEquals(pd.getT(), 0);
		assertEquals(pd.getC(), -1);
		assertEquals(pd.getX(), 1.125, 0);
		assertEquals(pd.getY(), -3.03125, 0);

		final List<ShapeAnnotationLink> annotations = ((PointI) pd.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "closed");
	}

	@Test
	public void testPolygon2D() {
		final Polygon2D<RealPoint> p = new DefaultPolygon2D(new double[] { 0, 10,
			20 }, new double[] { 0, 10, 0 });
		final ROIData rd = convertService.convert(p, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof PolygonData);
		final PolygonData pd = (PolygonData) s.get(0);

		assertEquals(pd.getZ(), 0);
		assertEquals(pd.getT(), 0);
		assertEquals(pd.getC(), -1);
		assertEquals(pd.getPoints().get(0).getX(), 0, 0);
		assertEquals(pd.getPoints().get(0).getY(), 0, 0);
		assertEquals(pd.getPoints().get(1).getX(), 10, 0);
		assertEquals(pd.getPoints().get(1).getY(), 0, 10);
		assertEquals(pd.getPoints().get(2).getX(), 20, 0);
		assertEquals(pd.getPoints().get(2).getY(), 0, 0);

		final List<ShapeAnnotationLink> annotations = ((PolygonI) pd.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "unspecified");
	}

	@Test
	public void testPolyline() {
		final List<RealPoint> pts = new ArrayList<>();
		pts.add(new RealPoint(new double[] { 1, 1 }));
		pts.add(new RealPoint(new double[] { 10, 10 }));
		pts.add(new RealPoint(new double[] { 19, 1 }));
		pts.add(new RealPoint(new double[] { 30.5, -1.25 }));
		final Polyline<RealPoint> p = new DefaultPolyline(pts);
		final ROIData rd = convertService.convert(p, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof PolylineData);
		final PolylineData pd = (PolylineData) s.get(0);

		assertEquals(pd.getZ(), 0);
		assertEquals(pd.getT(), 0);
		assertEquals(pd.getC(), -1);
		assertEquals(pd.getPoints().get(0).getX(), 1, 0);
		assertEquals(pd.getPoints().get(0).getY(), 1, 0);
		assertEquals(pd.getPoints().get(1).getX(), 10, 0);
		assertEquals(pd.getPoints().get(1).getY(), 10, 10);
		assertEquals(pd.getPoints().get(2).getX(), 19, 0);
		assertEquals(pd.getPoints().get(2).getY(), 1, 0);
		assertEquals(pd.getPoints().get(3).getX(), 30.5, 0);
		assertEquals(pd.getPoints().get(3).getY(), -1.25, 0);

		final List<ShapeAnnotationLink> annotations = ((PolylineI) pd.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "closed");
	}

	@Test
	public void testSphere() {
		final Sphere<RealPoint> e = new OpenSphere(new double[] { 5, 5 }, 2);
		final ROIData rd = convertService.convert(e, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof EllipseData);
		final EllipseData ed = (EllipseData) s.get(0);

		assertEquals(ed.getZ(), 0);
		assertEquals(ed.getT(), 0);
		assertEquals(ed.getC(), -1);
		assertEquals(ed.getX(), 5, 0);
		assertEquals(ed.getY(), 5, 0);
		assertEquals(ed.getRadiusX(), 2, 0);
		assertEquals(ed.getRadiusY(), 2, 0);

		final List<ShapeAnnotationLink> annotations = ((EllipseI) ed.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "open");
	}

	@Test
	public void testSuperEllipsoid() {
		final SuperEllipsoid<RealPoint> e = new ClosedSuperEllipsoid(new double[] {
			5, 5 }, new double[] { 10, 4 }, 3);
		exception.expect(IllegalArgumentException.class);
		convertService.convert(e, ROIData.class);
	}

	@Test
	public void testOMEROWrappedShape() {
		final EllipseData ed = new EllipseData(0, 0, 5, 8);
		final ROIData rd = new ROIData();
		((RoiI) rd.asIObject()).setName(omero.rtypes.rstring("Roi"));
		((RoiI) rd.asIObject()).setDescription(omero.rtypes.rstring(
			"I'm describing a thing!"));
		ed.setZ(5);
		ed.setT(12);
		ed.setId(112);
		rd.addShapeData(ed);

		final RealMask w = convertService.convert(rd, RealMask.class);
		final ROIData n = convertService.convert(w, ROIData.class);

		assertEquals(((RoiI) n.asIObject()).getName().getValue(), "Roi");
		assertEquals(((RoiI) n.asIObject()).getDescription().getValue(),
			"I'm describing a thing!");

		final List<ShapeData> s = n.getShapes(5, 12);
		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof EllipseData);
		final EllipseData ne = (EllipseData) s.get(0);

		assertEquals(ne.getId(), -1);
		assertEquals(ne.getZ(), 5);
		assertEquals(ne.getT(), 12);
		assertEquals(ne.getC(), -1);
		assertEquals(ne.getX(), 0, 0);
		assertEquals(ne.getY(), 0, 0);
		assertEquals(ne.getRadiusX(), 5, 0);
		assertEquals(ne.getRadiusY(), 8, 0);

		final List<ShapeAnnotationLink> annotations = ((EllipseI) ne.asIObject())
			.copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();

		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "closed");
	}

	@Test
	public void testSimpleOr() {
		final Box<RealPoint> b = new ClosedBox(new double[] { 0, 0 }, new double[] {
			10, 14 });
		final Sphere<RealPoint> s = new OpenSphere(new double[] { 6, 6 }, 5);
		final PointMask p = new DefaultPointMask(new double[] { 22, 3.125 });

		final RealMaskRealInterval or = b.or(s).or(p);
		final ROIData rd = convertService.convert(or, ROIData.class);
		final List<ShapeData> shapes = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 3);
		assertTrue(shapes.get(0) instanceof RectangleData);
		assertTrue(shapes.get(1) instanceof EllipseData);
		assertTrue(shapes.get(2) instanceof PointData);
		final RectangleData rect = (RectangleData) shapes.get(0);
		final EllipseData ell = (EllipseData) shapes.get(1);
		final PointData point = (PointData) shapes.get(2);

		assertEquals(rect.getX(), 0, 0);
		assertEquals(rect.getY(), 0, 0);
		assertEquals(rect.getWidth(), 10, 0);
		assertEquals(rect.getHeight(), 14, 0);
		assertEquals(ell.getX(), 6, 0);
		assertEquals(ell.getY(), 6, 0);
		assertEquals(ell.getRadiusX(), 5, 0);
		assertEquals(ell.getRadiusY(), 5, 0);
		assertEquals(point.getX(), 22, 0);
		assertEquals(point.getY(), 3.125, 0);

		final List<ShapeAnnotationLink> annotations = ((RectangleI) rect
			.asIObject()).copyAnnotationLinks();
		assertTrue(annotations.size() == 1);
		final ShapeAnnotationLink link = annotations.get(0);
		assertTrue(link.getChild() instanceof TagAnnotation);
		final TagAnnotation t = (TagAnnotation) link.getChild();
		assertEquals(t.getName().getValue(), "boundaryType");
		assertEquals(t.getTextValue().getValue(), "closed");

		final List<ShapeAnnotationLink> annotationsTwo = ((EllipseI) ell
			.asIObject()).copyAnnotationLinks();
		assertTrue(annotationsTwo.size() == 1);
		final ShapeAnnotationLink linkTwo = annotationsTwo.get(0);
		assertTrue(linkTwo.getChild() instanceof TagAnnotation);
		final TagAnnotation tTwo = (TagAnnotation) linkTwo.getChild();
		assertEquals(tTwo.getName().getValue(), "boundaryType");
		assertEquals(tTwo.getTextValue().getValue(), "open");

		final List<ShapeAnnotationLink> annotationsThree = ((PointI) point
			.asIObject()).copyAnnotationLinks();
		assertTrue(annotationsThree.size() == 1);
		final ShapeAnnotationLink linkThree = annotationsThree.get(0);
		assertTrue(linkTwo.getChild() instanceof TagAnnotation);
		final TagAnnotation tThree = (TagAnnotation) linkThree.getChild();
		assertEquals(tThree.getName().getValue(), "boundaryType");
		assertEquals(tThree.getTextValue().getValue(), "closed");
	}

	@Test
	public void testSimpleTransform() {
		final Box<RealPoint> b = new ClosedBox(new double[] { -11, -4 },
			new double[] { 11, 4 });
		final AffineTransform2D affine = new AffineTransform2D();
		affine.rotate(-Math.PI / 6);
		final RealMask transformed = b.transform(affine.inverse());
		final ROIData rd = convertService.convert(transformed, ROIData.class);
		final List<ShapeData> s = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 1);
		assertTrue(s.get(0) instanceof RectangleData);
		final RectangleData r = (RectangleData) s.get(0);

		assertEquals(r.getX(), -11, 0);
		assertEquals(r.getY(), -4, 0);
		assertEquals(r.getWidth(), 22, 0);
		assertEquals(r.getHeight(), 8, 0);
		assertTrue(r.getTransform() != null);

		assertEquals(Math.cos(-Math.PI / 6), r.getTransform().getA00().getValue(),
			0);
		assertEquals(-Math.sin(-Math.PI / 6), r.getTransform().getA01().getValue(),
			0);
		assertEquals(0, r.getTransform().getA02().getValue(), 0);
		assertEquals(Math.sin(-Math.PI / 6), r.getTransform().getA10().getValue(),
			0);
		assertEquals(Math.cos(-Math.PI / 6), r.getTransform().getA11().getValue(),
			0);
		assertEquals(0, r.getTransform().getA12().getValue(), 0);
	}

	@Test
	public void testMultipleOperations() {
		final Box<RealPoint> bOne = new ClosedBox(new double[] { 0, 0 },
			new double[] { 20, 10 });
		final Box<RealPoint> bTwo = new ClosedBox(new double[] { -5, -5 },
			new double[] { 5, 5 });
		final Box<RealPoint> bThree = new ClosedBox(new double[] { 12, 5 },
			new double[] { 36, 15 });
		final AffineTransform2D translate = new AffineTransform2D();
		translate.translate(new double[] { -10, -5 });
		final AffineTransform2D rotate = new AffineTransform2D();
		rotate.rotate(-Math.PI / 4);
		final RealMask m = bThree.or(bTwo.or(bOne.transform(translate.inverse())).transform(rotate.inverse()));

		final ROIData rd = convertService.convert(m, ROIData.class);
		final List<ShapeData> shapes = rd.getShapes(0, 0);

		assertTrue(rd.getShapeCount() == 3);
		assertTrue(shapes.get(0) instanceof RectangleData);
		assertTrue(shapes.get(1) instanceof RectangleData);
		assertTrue(shapes.get(2) instanceof RectangleData);
		final RectangleData rectTranRot = (RectangleData) shapes.get(2);
		final RectangleData rectRot = (RectangleData) shapes.get(1);
		final RectangleData rect = (RectangleData) shapes.get(0);

		// Test Rectangles
		assertEquals(rect.getX(), 12, 0);
		assertEquals(rect.getY(), 5, 0);
		assertEquals(rect.getWidth(), 24, 0);
		assertEquals(rect.getHeight(), 10, 0);
		assertEquals(rectRot.getX(), -5, 0);
		assertEquals(rectRot.getY(), -5, 0);
		assertEquals(rectRot.getWidth(), 10, 0);
		assertEquals(rectRot.getHeight(), 10, 0);
		assertEquals(rectTranRot.getX(), 0, 0);
		assertEquals(rectTranRot.getY(), 0, 0);
		assertEquals(rectTranRot.getWidth(), 20, 0);
		assertEquals(rectTranRot.getHeight(), 10, 0);

		// Test Transformations
		assertTrue(rect.getTransform() == null);
		assertTrue(rectRot.getTransform() != null);
		assertTrue(rectTranRot.getTransform() != null);
		final omero.model.AffineTransform rot = rectRot.getTransform();
		final omero.model.AffineTransform transRot = rectTranRot.getTransform();

		final double cos = Math.cos(-Math.PI / 4);
		final double sin = Math.sin(-Math.PI / 4);
		assertEquals(cos, rot.getA00().getValue(), 0);
		assertEquals(-sin, rot.getA01().getValue(), 0);
		assertEquals(0, rot.getA02().getValue(), 0);
		assertEquals(sin, rot.getA10().getValue(), 0);
		assertEquals(cos, rot.getA11().getValue(), 0);
		assertEquals(0, rot.getA12().getValue(), 0);

		assertEquals(cos, transRot.getA00().getValue(), 0);
		assertEquals(-sin, transRot.getA01().getValue(), 0);
		assertEquals(-10 * cos + -5 * -sin, transRot.getA02().getValue(), 0);
		assertEquals(sin, transRot.getA10().getValue(), 0);
		assertEquals(cos, transRot.getA11().getValue(), 0);
		assertEquals(-10 * sin + -5 * cos, transRot.getA12().getValue(), 0);
	}

	@Test
	public void testSimpleNonUnionOperation() {
		final Ellipsoid<RealPoint> e = new OpenEllipsoid(new double[] { 20, 30 },
			new double[] { 2.5, 7 });
		final Box<RealPoint> b = new ClosedBox(new double[] { 10, 30 },
			new double[] { 30, 40 });
		final RealMaskRealInterval and = e.and(b);

		exception.expect(IllegalArgumentException.class);
		convertService.convert(and, ROIData.class);
	}

	@Test
	public void testNestedNonUnionOperation() {
		final Ellipsoid<RealPoint> e = new OpenEllipsoid(new double[] { 4.5, 3.25 },
			new double[] { 6, 1 });
		final Box<RealPoint> b = new ClosedBox(new double[] { 10, 12 },
			new double[] { 28, 20 });
		final Sphere<RealPoint> s = new ClosedSphere(new double[] { 11, 18 }, 3);
		final PointMask p = new DefaultPointMask(new double[] { 100, 3.25 });
		final Line<RealPoint> l = new DefaultLine(new double[] { 20.25, 20.25 },
			new double[] { 6.125, -3 }, false);
		final AffineTransform2D a = new AffineTransform2D();
		a.rotate(Math.PI / 3);

		final RealMaskRealInterval plOr = l.or(p);
		final RealMaskRealInterval and = b.and(s);
		final RealMaskRealInterval transform = and.transform(a.inverse());
		final RealMaskRealInterval multiOr = plOr.or(and).or(transform).or(e);

		exception.expect(IllegalArgumentException.class);
		convertService.convert(multiOr, ROIData.class);
	}
}
