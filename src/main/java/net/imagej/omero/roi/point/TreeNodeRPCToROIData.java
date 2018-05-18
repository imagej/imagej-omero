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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imagej.omero.OMEROService;
import net.imagej.omero.roi.ROIConverters;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.RealPointCollection;

import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.TreeNode;

import omero.gateway.model.PointData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;

/**
 * Converts a {@link RealPointCollection} to {@link ROIData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.HIGH)
public class TreeNodeRPCToROIData extends
	AbstractConverter<TreeNode<RealPointCollection<?>>, ROIData>
{

	@Parameter
	private OMEROService omero;

	@Override
	public boolean canConvert(final Object src, final Type dest) {
		if (super.canConvert(src, dest)) return check((TreeNode<?>) src);
		return false;
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (super.canConvert(src, dest)) return check((TreeNode<?>) src);
		return false;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<TreeNode<RealPointCollection<? extends RealLocalizable>>>
		getInputType()
	{
		return (Class) TreeNode.class;
	}

	@Override
	public Class<ROIData> getOutputType() {
		return ROIData.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(final Object src, final Class<T> dest) {
		if (src == null || dest == null) throw new NullPointerException();
		if (!getInputType().isInstance(src)) {
			throw new IllegalArgumentException("Expected: " + getInputType()
				.getSimpleName() + " Received: " + src.getClass().getSimpleName());
		}
		if (!dest.isAssignableFrom(getOutputType())) {
			throw new IllegalArgumentException("Expected: " + getOutputType()
				.getSimpleName() + " Received: " + dest.getSimpleName());
		}

		final ROIData r = new ROIData();
		final RealPointCollection<?> rpc =
			(RealPointCollection<?>) ((TreeNode<?>) src).data();
		final String bt = boundaryType(rpc.boundaryType());
		final Iterator<?> pts = rpc.points().iterator();
		while (pts.hasNext())
			r.addShapeData(createPoint((RealLocalizable) pts.next(), bt));

		if (omero.getROIMapping(rpc) != null) {
			final ROIData prev = omero.getROIMapping(rpc);
			r.setId(prev.getId());

			// FIXME: The ID equivalence isn't maintained! If a point is moved, the ID
			// is just assigned at "random"
			updatePoints(r, prev);
		}

		return (T) r;
	}

	// -- Helper methods --

	private boolean check(final TreeNode<?> src) {
		return src.data() instanceof RealPointCollection;
	}

	private String boundaryType(final BoundaryType bt) {
		if (bt.equals(BoundaryType.OPEN)) return ROIConverters.OPEN_BOUNDARY_TEXT;
		else if (bt.equals(BoundaryType.UNSPECIFIED))
			return ROIConverters.UNSPECIFIED_BOUNDARY_TEXT;
		return ROIConverters.CLOSED_BOUNDARY_TEXT;
	}

	private PointData createPoint(final RealLocalizable l,
		final String boundaryType)
	{
		final PointData pd = new PointData(l.getDoublePosition(0), l
			.getDoublePosition(1));
		pd.setText(boundaryType);
		return pd;
	}

	private void updatePoints(final ROIData current, final ROIData prev) {
		final List<ShapeData> currentPoints = new ArrayList<>();
		final List<ShapeData> prevPoints = new ArrayList<>();
		final Iterator<List<ShapeData>> currentItr = current.getIterator();
		while (currentItr.hasNext())
			currentPoints.addAll(currentItr.next());
		final Iterator<List<ShapeData>> prevItr = prev.getIterator();
		while (prevItr.hasNext())
			prevPoints.addAll(prevItr.next());
		final List<ShapeData> currentCopy = new ArrayList<>(currentPoints);
		final List<ShapeData> prevCopy = new ArrayList<>(prevPoints);

		// If there's an equivalent point, set the ID
		for (final ShapeData point : currentPoints) {
			for (final ShapeData prevPoint : prevPoints) {
				if (ROIConverters.shapeDataEquals(point, prevPoint)) {
					point.setId(prevPoint.getId());
					currentCopy.remove(point);
					prevCopy.remove(point);
					break;
				}
			}
		}

		if (currentCopy.isEmpty()) return;

		// If there's any remaining non-equivalent points, set the remaining IDs.
		// If not leave them alone, and they'll be uploaded as new PointData
		final Iterator<ShapeData> prevPointsItr = prevPoints.iterator();
		for (final ShapeData point : currentCopy) {
			if (!prevPointsItr.hasNext()) break;
			point.setId(prevPointsItr.next().getId());
		}
	}

}
