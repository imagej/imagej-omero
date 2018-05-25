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

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.util.Util;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;
import org.scijava.util.TreeNode;

import omero.gateway.model.ROIData;

/**
 * Converts a {@code TreeNode<RandomAccessibleInterval<?>>} to a
 * {@link ROIData}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class TreeNodeRAIToROIData extends
	AbstractTreeNodeToROIData<TreeNode<RandomAccessibleInterval<?>>>
{

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<TreeNode<RandomAccessibleInterval<?>>> getInputType() {
		return (Class) TreeNode.class;
	}

	@Override
	protected boolean check(final TreeNode<?> dn) {
		return dn.data() instanceof RandomAccessibleInterval && Util
			.getTypeFromInterval(((RandomAccessibleInterval<?>) dn
				.data())) instanceof BooleanType;
	}

}
