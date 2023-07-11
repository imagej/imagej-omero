/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2023 Open Microscopy Environment:
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

import net.imagej.omero.OMEROException;
import net.imagej.omero.OMEROSession;
import net.imagej.roi.ROITree;

import org.scijava.log.LogService;
import org.scijava.util.TreeNode;

/**
 * {@link ROITree} which downloads the associated ROIs only when the children
 * are requested.
 *
 * @author Alison Walter
 */
public class LazyROITree implements ROITree {

	private TreeNode<?> parent;
	private final long imageID;
	private final OMEROSession session;
	private final LogService log;
	private List<TreeNode<?>> children;
	private boolean roisLoaded;

	public LazyROITree(final long imageID, final OMEROSession session) {
		this.imageID = imageID;
		this.session = session;
		this.log = session.log();
		roisLoaded = false;
	}

	public boolean areROIsLoaded() {
		return roisLoaded;
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
		roisLoaded = true;
		return getChildren();
	}

	@Override
	public Void data() {
		return null;
	}

	// -- Helper methods --

	private List<TreeNode<?>> getChildren() {
		if (children == null) retrieveChildren();
		return children;
	}

	@SuppressWarnings("null")
	private synchronized void retrieveChildren() {
		if (children != null) return;
		List<TreeNode<?>> c = null;
		try {
			c = session.downloadROIs(imageID).children();
		}
		catch (final OMEROException exc) {
			log.error("Error retrieving ROIs", exc);
		}

		if (c == null || c.isEmpty()) children = new ArrayList<>();

		for (final TreeNode<?> child : c)
			child.setParent(this);
		children = c;
	}

}
