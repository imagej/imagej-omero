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
import java.util.List;

import net.imagej.axis.TypedAxis;
import net.imagej.space.TypedSpace;

/**
 * Default implementation of {@link OMERORoiElement}.
 *
 * @author Alison Walter
 */
public class DefaultOMERORoiElement implements OMERORoiElement {

	private final OMERORealMask<?> source;
	private DataNode<?> parent;
	private final List<DataNode<?>> children;
	private final DefaultOMERORoiProjector projector;

	public DefaultOMERORoiElement(final OMERORealMask<?> source,
		final DataNode<?> parent, final List<DataNode<?>> children)
	{
		this.source = source;
		this.parent = parent;
		if (children == null) this.children = new ArrayList<>();
		else this.children = children;
		this.projector = new DefaultOMERORoiProjector();
	}

	@Override
	public DataNode<?> getParent() {
		return parent;
	}

	@Override
	public void setParent(final DataNode<?> parent) {
		this.parent = parent;
	}

	@Override
	public List<DataNode<?>> children() {
		return children;
	}

	@Override
	public OMERORealMask<?> getData() {
		return source;
	}

	@Override
	public OMERORealMask<?> projectIntoSpace(
		final TypedSpace<? extends TypedAxis> space)
	{
		return projector.project(source, space);
	}

}
