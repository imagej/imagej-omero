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
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.imglib2.roi.MaskPredicate;

import org.scijava.convert.ConvertService;
import org.scijava.util.TreeNode;

import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;
import omero.model.Roi;
import omero.model.Shape;

/**
 * Default implementation of {@link OMEROROICollection}. Mutating the children,
 * mutates the backing {@link ROIData}.
 *
 * @author Alison Walter
 */
public class DefaultOMEROROICollection implements OMEROROICollection {

	private final ConvertService convert;
	private ROIData roi;
	private TreeNode<?> parent;
	private List<TreeNode<?>> children;
	private final Map<MaskPredicate<?>, ShapeData> newChildren;

	public DefaultOMEROROICollection(final TreeNode<?> parent,
		final ROIData omeroRoi, final ConvertService convert)
	{
		roi = omeroRoi;
		this.parent = parent;
		this.convert = convert;
		newChildren = new IdentityHashMap<>();
	}

	public DefaultOMEROROICollection(final ROIData omeroRoi,
		final ConvertService convert)
	{
		roi = omeroRoi;
		this.convert = convert;
		newChildren = new IdentityHashMap<>();
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
		if (children == null) createChildren();
		return new OMERORoiChildren(children);
	}

	@Override
	public ROIData data() {
		updateNewShapes();
		return roi;
	}

	@Override
	public void addChildren(final List<? extends TreeNode<?>> nodes) {
		children().addAll(nodes);
	}

	// -- Helper methods --

	private synchronized void createChildren() {
		if (children != null) return;
		final ArrayList<TreeNode<?>> c = new ArrayList<>(roi.getShapeCount());
		final Iterator<List<ShapeData>> itr = roi.getIterator();
		while (itr.hasNext()) {
			final List<ShapeData> shapes = itr.next();
			for (final ShapeData shape : shapes) {
				final OMERORealMask<?> orm = convert.convert(shape,
					OMERORealMask.class);
				c.add(new DefaultOMEROROIElement(orm, this, null));
			}
		}

		children = c;
	}

	private void updateNewShapes() {
		for (final MaskPredicate<?> mp : newChildren.keySet()) {
			final ShapeData old = newChildren.get(mp);
			final ShapeData updated = convert.convert(mp, ShapeData.class);
			ROIConverters.synchronizeShapeData(updated, old);
		}
	}

	private final class OMERORoiChildren implements List<TreeNode<?>> {

		private final List<TreeNode<?>> source;

		public OMERORoiChildren(final List<TreeNode<?>> source) {
			this.source = source;
		}

		@Override
		public int size() {
			return source.size();
		}

		@Override
		public boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public boolean contains(final Object o) {
			return source.contains(o);
		}

		@Override
		public Iterator<TreeNode<?>> iterator() {
			return source.iterator();
		}

		@Override
		public Object[] toArray() {
			return source.toArray();
		}

		@Override
		public <T> T[] toArray(final T[] a) {
			return source.toArray(a);
		}

		@Override
		public boolean add(final TreeNode<?> e) {
			checkMaskPredicateData(e);
			addToOMEROROICollection((MaskPredicate<?>) e.data());
			if (e.parent() != DefaultOMEROROICollection.this) e.setParent(
				DefaultOMEROROICollection.this);
			return source.add(e);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Rebuilds the {@link ROIData} object.
		 * </p>
		 */
		@Override
		public boolean remove(final Object o) {
			if (source.remove(o)) {
				removeFromOMEROROICollection(o);
				return true;
			}
			return false;
		}

		@Override
		public boolean containsAll(final Collection<?> c) {
			return source.containsAll(c);
		}

		@Override
		public boolean addAll(final Collection<? extends TreeNode<?>> c) {
			boolean modified = false;
			for (final TreeNode<?> d : c)
				modified |= add(d);
			return modified;
		}

		@Override
		public boolean addAll(final int index,
			final Collection<? extends TreeNode<?>> c)
		{
			final int sizeBefore = size();
			for (final TreeNode<?> d : c)
				add(index, d);
			return sizeBefore != size();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Rebuilds the {@link ROIData} object.
		 * </p>
		 */
		@Override
		public boolean removeAll(final Collection<?> c) {
			boolean modified = false;
			for (final Object o : c)
				modified |= remove(o);
			return modified;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Rebuilds the {@link ROIData} object.
		 * </p>
		 */
		@Override
		public boolean retainAll(final Collection<?> c) {
			boolean modified = false;
			for (final TreeNode<?> dn : source) {
				if (!c.contains(dn)) modified |= remove(dn);
			}
			return modified;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("clear");
		}

		@Override
		public TreeNode<?> get(final int index) {
			return source.get(index);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Rebuilds the {@link ROIData} object.
		 * </p>
		 */
		@Override
		public TreeNode<?> set(final int index, final TreeNode<?> element) {
			checkMaskPredicateData(element);

			final TreeNode<?> previous = source.set(index, element);
			removeFromOMEROROICollection(previous);
			addToOMEROROICollection((MaskPredicate<?>) element.data());

			return previous;
		}

		@Override
		public void add(final int index, final TreeNode<?> element) {
			checkMaskPredicateData(element);
			addToOMEROROICollection((MaskPredicate<?>) element.data());
			if (element.parent() != DefaultOMEROROICollection.this) element.setParent(
				DefaultOMEROROICollection.this);
			source.add(index, element);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Rebuilds the {@link ROIData} object.
		 * </p>
		 */
		@Override
		public TreeNode<?> remove(final int index) {
			final TreeNode<?> removed = source.remove(index);
			if (removed != null) removeFromOMEROROICollection(removed);
			return removed;
		}

		@Override
		public int indexOf(final Object o) {
			return source.indexOf(o);
		}

		@Override
		public int lastIndexOf(final Object o) {
			return source.lastIndexOf(o);
		}

		@Override
		public ListIterator<TreeNode<?>> listIterator() {
			return source.listIterator();
		}

		@Override
		public ListIterator<TreeNode<?>> listIterator(final int index) {
			return source.listIterator(index);
		}

		@Override
		public List<TreeNode<?>> subList(final int fromIndex, final int toIndex) {
			return source.subList(fromIndex, toIndex);
		}

		private void checkMaskPredicateData(final TreeNode<?> dn) {
			if (dn.data() instanceof MaskPredicate) return;
			throw new IllegalArgumentException("TreeNode data must be MaskPredicate" +
				" not " + dn.data().getClass());
		}

		private void removeFromOMEROROICollection(final Object o) {
			if (o instanceof OMEROROIElement) {
				final ShapeData s = ((OMEROROIElement) o).data().getShape();

				// NB: removing from ShapeData is a bad idea, if the shape in question's
				// Z or T have been modified attempting to remove it will result in an
				// Exception due to issues with the backing TreeMap not being updated.
				// So operate on the backing ice objects directly, then recreate the
				// ROIData which rebuilds the TreeMap.
				final Shape iceShape = (Shape) s.asIObject();
				final Roi iceROI = (Roi) roi.asIObject();
				iceROI.removeShape(iceShape);
				DefaultOMEROROICollection.this.roi = new ROIData(iceROI);
			}
			if (o instanceof TreeNode && newChildren.get(((TreeNode<?>) o)
				.data()) != null) newChildren.remove(((TreeNode<?>) o).data());
		}

		private void addToOMEROROICollection(final MaskPredicate<?> mp) {
			final ShapeData s = convert.convert(mp, ShapeData.class);
			roi.addShapeData(s);

			// NB: If the object is already backed by a ShapeData, there's no point
			// in storing a mapping
			if (!(mp instanceof OMERORealMask)) newChildren.put(mp, s);
		}
	}
}
