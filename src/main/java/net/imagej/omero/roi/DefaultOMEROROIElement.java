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

package net.imagej.omero.roi;

import java.util.ArrayList;
import java.util.List;

import net.imagej.axis.TypedAxis;
import net.imagej.omero.roi.project.DefaultOMEROROIProjector;
import net.imagej.space.TypedSpace;

import org.scijava.util.TreeNode;

/**
 * Default implementation of {@link OMEROROIElement}.
 *
 * @author Alison Walter
 */
public class DefaultOMEROROIElement implements OMEROROIElement {

	private final OMERORealMask<?> source;
	private TreeNode<?> parent;
	private final List<TreeNode<?>> children;
	private final DefaultOMEROROIProjector projector;

	public DefaultOMEROROIElement(final OMERORealMask<?> source,
		final TreeNode<?> parent, final List<TreeNode<?>> children)
	{
		this.source = source;
		this.parent = parent;
		if (children == null) this.children = new ArrayList<>();
		else this.children = children;
		this.projector = new DefaultOMEROROIProjector();
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
		return children;
	}

	@Override
	public OMERORealMask<?> data() {
		return source;
	}

	@Override
	public OMERORealMask<?> projectIntoSpace(
		final TypedSpace<? extends TypedAxis> space)
	{
		return projector.project(source, space);
	}

}
