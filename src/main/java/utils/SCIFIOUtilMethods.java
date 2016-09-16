
package utils;

import io.scif.Metadata;
import io.scif.filters.ReaderFilter;
import io.scif.img.ImgOpener;

import java.util.HashSet;
import java.util.Set;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;

public class SCIFIOUtilMethods {

	private SCIFIOUtilMethods() {
		// Helper class
	}

	/**
	 * Returns a list of all AxisTypes that should be split out. This is a list of
	 * all non-X,Y planar axes. Always tries to split {@link Axes#CHANNEL}. NB:
	 * Copied from a private method in {@link ImgOpener}
	 */
	public static final AxisType[] axesToSplit(final ReaderFilter r) {
		final Set<AxisType> axes = new HashSet<>();
		final Metadata meta = r.getTail().getMetadata();
		// Split any non-X,Y axis
		for (final CalibratedAxis t : meta.get(0).getAxesPlanar()) {
			final AxisType type = t.type();
			if (!(type == Axes.X || type == Axes.Y)) {
				axes.add(type);
			}
		}
		// Ensure channel is attempted to be split
		axes.add(Axes.CHANNEL);
		return axes.toArray(new AxisType[axes.size()]);
	}

}
