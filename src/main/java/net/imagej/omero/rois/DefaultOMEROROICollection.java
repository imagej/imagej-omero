/*-
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

package net.imagej.omero.rois;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.scijava.convert.ConvertService;
import org.scijava.util.TreeNode;

import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;

/**
 * Default implementation of {@link OMEROROICollection}.
 *
 * @author Alison Walter
 */
public class DefaultOMEROROICollection implements OMEROROICollection {

	private final ConvertService convert;
	private final ROIData roi;
	private TreeNode<?> parent;

	public DefaultOMEROROICollection(final TreeNode<?> parent,
		final ROIData omeroRoi, final ConvertService convert)
	{
		roi = omeroRoi;
		this.parent = parent;
		this.convert = convert;
	}

	public DefaultOMEROROICollection(final ROIData omeroRoi,
		final ConvertService convert)
	{
		roi = omeroRoi;
		this.convert = convert;
	}

	@Override
	public TreeNode<?> parent() {
		return parent;
	}

	@Override
	public void setParent(final TreeNode<?> parent) {
		this.parent = parent;
	}

	@Override
	public List<TreeNode<?>> children() {
		final ArrayList<TreeNode<?>> children = new ArrayList<>(roi
			.getShapeCount());
		final Iterator<List<ShapeData>> itr = roi.getIterator();
		while (itr.hasNext()) {
			final List<ShapeData> shapes = itr.next();
			for (final ShapeData shape : shapes) {
				final OMERORealMask<?> orm = convert.convert(shape,
					OMERORealMask.class);
				children.add(new DefaultOMEROROIElement(orm, this, null));
			}
		}
		return Collections.unmodifiableList(children);
	}

	@Override
	public ROIData data() {
		return roi;
	}
}
