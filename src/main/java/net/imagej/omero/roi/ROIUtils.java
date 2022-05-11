package net.imagej.omero.roi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.Mask;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.type.logic.BoolType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

import org.scijava.util.DefaultTreeNode;
import org.scijava.util.TreeNode;

import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;

/**
 * Utility class for working with converting between ImageJ and OMERO ROIs.
 * 
 * @author Alison Walter
 * @author Curtis Rueden
 *
 */
public class ROIUtils {

	/**
	 * Puts interval bounds on an unbounded {@link MaskPredicate}. The bounds will
	 * correspond to the size of the image. If the {@link MaskPredicate} is a
	 * {@link RealMask}, it is also rasterized.
	 *
	 * @param m an unbounded {@link MaskPredicate}
	 * @param interval the interval to apply
	 * @return a TreeNode whose data is a RandomAccessibleInterval representation
	 *         of the original data
	 */
	public static TreeNode<RandomAccessibleInterval<BoolType>> interval(
		final MaskPredicate<?> m, final Interval interval)
	{
		RandomAccessibleInterval<BoolType> rai;
		if (m instanceof Mask) rai = Views.interval(Masks.toRandomAccessible(
			(Mask) m), interval);
		else rai = Views.interval(Views.raster(Masks.toRealRandomAccessible(
			(RealMask) m)), interval);

		return new DefaultTreeNode<>(rai, null);
	}

	/**
	 * Collects all {@link TreeNode}s in the given "tree" whose data is a Roi.
	 *
	 * @param dn TreeNode whose data and children are check for ROIs
	 * @return a list of TreeNodes whose data is a ROI type.
	 */
	public static List<TreeNode<?>> collectROITreeNodes(final TreeNode<?> dn) {
		if (dn == null) return Collections.emptyList();

		if (dn.children() == null || dn.children().isEmpty()) {
			if (dn.data() instanceof MaskPredicate) return Collections
				.singletonList(dn);
			return Collections.emptyList();
		}
		if (dn.data() instanceof ROIData) return Collections.singletonList(dn);

		final List<TreeNode<?>> rois = new ArrayList<>();
		for (final TreeNode<?> child : dn.children()) {
			if (child.data() instanceof ROIData) rois.add(child);
			else if ((child.children() == null || child.children().isEmpty()) && child
				.data() instanceof MaskPredicate) rois.add(child);
			else collectROITreeNodes(child, rois);
		}

		return rois;
	}

	public static void collectROITreeNodes(final TreeNode<?> dn,
		final List<TreeNode<?>> rois)
	{
		if (dn.children() == null || dn.children().isEmpty()) return;

		for (final TreeNode<?> child : dn.children()) {
			if (child.data() instanceof ROIData) rois.add(child);
			else if (child.children() == null && child
				.data() instanceof MaskPredicate) rois.add(child);
			else collectROITreeNodes(child, rois);
		}
	}

	public static long[] getROIIds(final Collection<ROIData> rois) {
		final long[] ids = new long[rois.size()];
		final Iterator<ROIData> itr = rois.iterator();
		for (int i = 0; i < ids.length; i++)
			ids[i] = itr.next().getId();
		return ids;
	}

	/**
	 * Splits the ROIs within the given {@link TreeNode} into a {@link Pair} of
	 * ROIs which originated from OMERO and ROIs which originated from ImageJ.
	 *
	 * @param dn ROIs to separate
	 * @return a {@link Pair} of list of roi {@link TreeNode}s
	 */
	public static Pair<List<OMEROROICollection>, List<TreeNode<?>>> split(
		final TreeNode<?> dn)
	{
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs =
			new ValuePair<>(new ArrayList<>(), new ArrayList<>());

		if (dn instanceof OMEROROICollection) {
			splitROIs.getA().add((OMEROROICollection) dn);
			return splitROIs;
		}
		if (dn.data() instanceof MaskPredicate) splitROIs.getB().add(dn);
		if (dn.children() == null || dn.children().isEmpty()) return splitROIs;

		for (final TreeNode<?> child : dn.children())
			split(child, splitROIs);

		return splitROIs;
	}

	/**
	 * Splits the ROIs within the given {@link TreeNode} into a {@link Pair} of
	 * ROIs which originated from OMERO and ROIs which originated from ImageJ.
	 *
	 * @param dn ROIs to separate
	 */
	public static void split(final TreeNode<?> dn,
		final Pair<List<OMEROROICollection>, List<TreeNode<?>>> splitROIs)
	{
		if (dn instanceof OMEROROICollection) {
			splitROIs.getA().add((OMEROROICollection) dn);
			return;
		}
		if (dn.data() instanceof MaskPredicate) splitROIs.getB().add(dn);
		if (dn.children() == null || dn.children().isEmpty()) return;
		for (final TreeNode<?> child : dn.children())
			split(child, splitROIs);
	}

	/**
	 * Clears ids of the given {@link ROIData} objects, to ensure they are
	 * uploaded to the server as a new object.
	 *
	 * @param rois {@link ROIData} objects whose ids must be cleared
	 */
	public static void clearROIs(final List<ROIData> rois) {
		for (final ROIData roi : rois) {
			roi.asIObject().setId(null);
			final Iterator<List<ShapeData>> itr = roi.getIterator();
			while (itr.hasNext()) {
				for (final ShapeData shape : itr.next())
					shape.asIObject().setId(null);
			}
		}
	}

	/**
	 * Sets the backing {@link ROIData} and {@link ShapeData} objects to have the
	 * same IDs as the newly saved version on the server.
	 *
	 * @param orc {@link OMEROROICollection} whose backing ROIData will be updated
	 * @param saved recently saved {@link ROIData} whose IDs will be copied
	 */
	public static void updateROIData(final OMEROROICollection orc,
		final ROIData saved)
	{
		final ROIData rd = orc.data();
		rd.setId(saved.getId());

		// Shapes may not be in the same order
		final List<ShapeData> shapes = new ArrayList<>();
		final List<ShapeData> savedShapes = new ArrayList<>();
		final Iterator<List<ShapeData>> itr = rd.getIterator();
		final Iterator<List<ShapeData>> savedItr = saved.getIterator();
		while (itr.hasNext())
			shapes.addAll(itr.next());
		while (savedItr.hasNext())
			savedShapes.addAll(savedItr.next());

		for (final ShapeData shape : shapes) {
			ShapeData match = null;
			for (final ShapeData savedShape : savedShapes) {
				if (ROIConverters.shapeDataEquals(shape, savedShape)) {
					match = savedShape;
					shape.setId(savedShape.getId());
					break;
				}
			}
			if (match == null) throw new IllegalArgumentException(
				"Uploaded ROIData is missing a shape!");
			savedShapes.remove(match);
		}
	}
}
