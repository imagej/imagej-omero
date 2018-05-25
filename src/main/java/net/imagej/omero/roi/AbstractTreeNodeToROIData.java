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

import java.lang.reflect.Type;

import net.imagej.omero.OMEROService;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.view.IntervalView;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.util.TreeNode;

import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;

/**
 * Abstract base layer for converting {@link TreeNode} to {@link ROIData}.
 *
 * @author Alison Walter
 */
public abstract class AbstractTreeNodeToROIData<D extends TreeNode<?>> extends
	AbstractConverter<D, ROIData>
{

	@Parameter
	private ConvertService convert;

	@Parameter
	private OMEROService omero;

	@Parameter
	private LogService log;

	@Override
	public Class<ROIData> getOutputType() {
		return ROIData.class;
	}

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

		MaskPredicate<?> mp = null;
		final ROIData r = new ROIData();
		final ShapeData s = convert.convert(((TreeNode<?>) src).data(),
			ShapeData.class);

		if (src instanceof IntervalView) {
			final RandomAccessible<?> ra = ((IntervalView<?>) src).getSource();
			if (ra instanceof RandomAccessibleOnRealRandomAccessible) {
				final RealRandomAccessible<?> rra =
					((RandomAccessibleOnRealRandomAccessible<?>) ra).getSource();
				mp = convert.convert(rra, MaskPredicate.class);
			}
			else mp = convert.convert(ra, MaskPredicate.class);
		}
		if (((TreeNode<?>) src).data() instanceof MaskPredicate) mp =
			(MaskPredicate<?>) ((TreeNode<?>) src).data();
		if (omero.getROIMapping(mp) != null) {
			final ROIData prev = omero.getROIMapping(mp);
			r.setId(prev.getId());

			// Assume there's only one shape, since only ROIData with one ShapeData is
			// equivalent to MaskPredicate
			s.setId(prev.getIterator().next().iterator().next().getId());
		}

		r.addShapeData(s);
		return (T) r;
	}

	abstract boolean check(TreeNode<?> src);
}
