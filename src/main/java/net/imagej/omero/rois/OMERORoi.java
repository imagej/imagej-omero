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

package net.imagej.omero.rois;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.imagej.axis.AxisType;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.Operators;
import net.imglib2.roi.Operators.MaskOperator;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.composite.CompositeMaskPredicate;

import org.scijava.util.IntArray;

import omero.gateway.model.ROIData;
import omero.model.Roi;

/**
 * Wraps an OMERO Roi with multiple shapes on multiple planes as multiple ors.
 *
 * @author Alison Walter
 */
public class OMERORoi implements CompositeMaskPredicate<RealLocalizable>,
	RealMask
{

	private final HashMap<IntArray, RealMask> shapes;
	private final ROIData roi;
	private final List<AxisType> axes;

	public OMERORoi(final HashMap<IntArray, RealMask> shapes, final ROIData roi,
		final List<AxisType> axes)
	{
		this.shapes = shapes;
		this.roi = roi;
		this.axes = axes;
	}

	/**
	 * The test point {@code l} should have 2 to 5 coordinates. The first two
	 * coordinates must correspond to x and y. If there is a z, it is always the
	 * third coordinate. If there is a c, it is always the last coordinate. In
	 * other words, the priority order for z, t, and c coordinates is z, t, c.
	 */
	@Override
	public boolean test(final RealLocalizable l) {
		// OMERO Rois do not exist at negative z, t, or c
		for (int i = 2; i < l.numDimensions(); i++) {
			if (l.getDoublePosition(i) < 0) return false;
		}

		final IntArray test = new IntArray(new int[] { -1, -1, -1 });
		// Given ROI is defined for all z, t, c
		if (axes == null) {
			return shapes.get(test).test(l);
		}

		boolean hasNeighbors = false;
		// Don't bother creating all the neighbors. Since there is a shape which
		// is defined for all z, t, c there's always neighbors.
		if (shapes.containsKey(test)) hasNeighbors = true;

		// Check all neighbors
		hasNeighbors = testNeighbors(l);

		if (hasNeighbors) {
			for (int i = 0; i < axes.size(); i++) {
				if (axes.get(i).getLabel().equals("Z")) test.set(0, (int) Math.round(l
					.getDoublePosition(i + 2)));
				if (axes.get(i).getLabel().equals("Time")) test.set(1, (int) Math.round(
					l.getDoublePosition(i + 2)));
				if (axes.get(i).getLabel().equals("Channel")) test.set(2, (int) Math
					.round(l.getDoublePosition(i + 2)));
			}
			// It is possible but there isn't an exact match, because one of the
			// matches was for a shape which encompasses an entire axis.
			return checkShapes(test, l);
		}

		return false;
	}

	@Override
	public int numDimensions() {
		return axes == null ? 2 : axes.size() + 2;
	}

	public Roi getRoi() {
		return (Roi) roi.asIObject();
	}

	/**
	 * Returns a map of location in z, t, c space to the corresponding
	 * {@link RealMask}. This map is intended for read only use.
	 *
	 * @return An unmodifiable map
	 */
	public Map<IntArray, RealMask> getShapes() {
		return Collections.unmodifiableMap(shapes);
	}

	@Override
	public BoundaryType boundaryType() {
		final BoundaryType bt = shapes.values().iterator().next().boundaryType();
		for (final RealMask m : shapes.values()) {
			if (!bt.equals(m.boundaryType())) return BoundaryType.UNSPECIFIED;
		}
		return bt;
	}

	@Override
	public MaskOperator operator() {
		return Operators.OR;
	}

	/**
	 * @throws UnsupportedOperationException the shapes in this composite do not
	 *           have a defined order
	 */
	@Override
	public Predicate<? super RealLocalizable> operand(final int index) {
		throw new UnsupportedOperationException("operand(int)");
	}

	/**
	 * Adding/removing items from this list will not affect this {@code Mask}.
	 */
	@Override
	public List<Predicate<?>> operands() {
		return Collections.unmodifiableList(new ArrayList<>(shapes.values()));
	}

	// -- Helper methods --

	private boolean testNeighbors(final RealLocalizable l) {
		final int[][] n = createNeighbors(l);

		boolean contained = true;
		for (int i = 0; i < n.length; i++) {
			contained &= checkAllZTC(n[i]);
		}

		return contained;
	}

	private int[][] createNeighbors(final RealLocalizable l) {
		final int[][] neighbors = new int[(int) Math.pow(2, l.numDimensions() -
			2)][3];
		for (int i = 0; i < neighbors.length; i++)
			Arrays.fill(neighbors[i], -1);

		int mod = 1;
		for (int t = 0; t < axes.size(); t++) {
			if (axes.get(t).getLabel().equals("Z")) fill(neighbors, 0, mod, l
				.getDoublePosition(t + 2));
			if (axes.get(t).getLabel().equals("Time")) fill(neighbors, 1, mod, l
				.getDoublePosition(t + 2));
			if (axes.get(t).getLabel().equals("Channel")) fill(neighbors, 2, mod, l
				.getDoublePosition(t + 2));
			mod *= 2;
		}

		return neighbors;
	}

	private void fill(final int[][] points, final int axis, final int mod,
		final double pos)
	{
		boolean floor = false;
		for (int i = 0; i < points.length; i++) {
			if (i % mod == 0) floor = !floor;
			if (floor) points[i][axis] = (int) Math.floor(pos);
			else points[i][axis] = (int) Math.ceil(pos);
		}
	}

	/**
	 * Accounts for -1 in {@code shapes}. It is possible that some of the OMERO
	 * Shapes in this occupy an entire z, t, or c. This method checks if the given
	 * point is contained, or if there are any points which match due to
	 * containing an entire axis.
	 *
	 * @param point the position in Z, T, C space to check
	 * @return true if the point is contained, false if not
	 */
	private boolean checkAllZTC(final int[] point) {
		boolean hasZ = false;
		boolean hasT = false;
		boolean hasC = false;

		boolean b = false;

		b |= shapes.containsKey(new IntArray(point));

		if (axes.size() > 1) {
			for (int t = 0; t < axes.size(); t++) {
				final int[] c = point.clone();
				if (axes.get(t).getLabel().equals("Z")) {
					c[0] = -1;
					hasZ = true;
				}
				if (axes.get(t).getLabel().equals("Time")) {
					c[1] = -1;
					hasT = true;
				}
				if (axes.get(t).getLabel().equals("Channel")) {
					c[2] = -1;
					hasC = true;
				}
				b |= shapes.containsKey(new IntArray(c));
			}
		}

		if (hasZ && hasT && hasC) {
			b |= shapes.containsKey(new IntArray(new int[] { -1, -1, point[2] }));
			b |= shapes.containsKey(new IntArray(new int[] { -1, point[1], -1 }));
			b |= shapes.containsKey(new IntArray(new int[] { point[0], -1, -1 }));
		}

		return b;
	}

	private boolean checkShapes(final IntArray point, final RealLocalizable l) {
		boolean hasZ = false;
		boolean hasT = false;
		boolean hasC = false;

		boolean b = false;

		if (shapes.containsKey(point)) b |= shapes.get(point).test(l);

		if (axes.size() > 1) {
			for (int t = 0; t < axes.size(); t++) {
				final IntArray c = new IntArray(point.copyArray());
				if (axes.get(t).getLabel().equals("Z")) {
					c.set(0, -1);
					hasZ = true;
				}
				if (axes.get(t).getLabel().equals("Time")) {
					c.set(1, -1);
					hasT = true;
				}
				if (axes.get(t).getLabel().equals("Channel")) {
					c.set(2, -1);
					hasC = true;
				}
				if (shapes.containsKey(c)) b |= shapes.get(c).test(l);
			}
		}

		if (hasZ && hasT && hasC) {
			final IntArray one = new IntArray(new int[] { -1, -1, point.get(2) });
			final IntArray two = new IntArray(new int[] { -1, point.get(1), -1 });
			final IntArray three = new IntArray(new int[] { point.get(0), -1, -1 });

			if (shapes.containsKey(one)) b |= shapes.get(one).test(l);
			if (shapes.containsKey(two)) b |= shapes.get(two).test(l);
			if (shapes.containsKey(three)) b |= shapes.get(three).test(l);
		}

		return b;
	}

}
