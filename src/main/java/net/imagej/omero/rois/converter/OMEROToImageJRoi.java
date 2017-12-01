/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2016 Open Microscopy Environment:
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

package net.imagej.omero.rois.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.omero.rois.OMERORoi;
import net.imglib2.roi.RealMaskRealInterval;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.IntArray;

import omero.gateway.model.ROIData;
import omero.gateway.model.ShapeData;

/**
 * Converts an OMERO Roi containing n rois, potentially on different planes, to
 * an {@link OMERORoi}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class OMEROToImageJRoi extends
	AbstractConverter<ROIData, RealMaskRealInterval>
{

	@Parameter
	ConvertService convertService;

	private boolean hasZ;
	private boolean hasT;
	private boolean hasC;

	private double[] currentMin;
	private double[] currentMax;

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

		final ROIData rd = (ROIData) src;
		currentMin = new double[] { Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
			Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY };
		currentMax = new double[] { Double.NEGATIVE_INFINITY,
			Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
			Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
		final HashMap<IntArray, RealMaskRealInterval> map = createHashMap(rd);
		final List<AxisType> axes = new ArrayList<>();

		if (hasZ) axes.add(Axes.Z);
		if (hasT) axes.add(Axes.TIME);
		if (hasC) axes.add(Axes.CHANNEL);

		final double[] min = new double[2 + axes.size()];
		final double[] max = new double[2 + axes.size()];
		populateMinMax(min, max);

		return (T) new OMERORoi(map, rd, axes.isEmpty() ? null : axes, min, max);
	}

	@Override
	public Class<RealMaskRealInterval> getOutputType() {
		return RealMaskRealInterval.class;
	}

	@Override
	public Class<ROIData> getInputType() {
		return ROIData.class;
	}

	// -- Helper methods --

	private HashMap<IntArray, RealMaskRealInterval> createHashMap(
		final ROIData roi)
	{
		final HashMap<IntArray, RealMaskRealInterval> shapes = new HashMap<>();
		final Iterator<List<ShapeData>> itr = roi.getIterator();
		while (itr.hasNext()) {
			final List<ShapeData> s = itr.next();
			for (int i = 0; i < s.size(); i++) {
				final ShapeData c = s.get(i);
				RealMaskRealInterval m = convertService.convert(c,
					RealMaskRealInterval.class);
				if (m == null) throw new IllegalArgumentException("Cannot convert");
				if (c.getTransform() != null) m = m.transform(RoiConverters
					.createAffine(c.getTransform()));
				updateMinMax(new double[] { m.realMin(0), m.realMin(1), c.getZ(), c
					.getT(), c.getC() }, new double[] { m.realMax(0), m.realMax(1), c
						.getZ(), c.getT(), c.getC() });

				// Check and update axes
				hasZ |= c.getZ() != -1;
				hasT |= c.getT() != -1;
				hasC |= c.getC() != -1;

				final IntArray ztc = new IntArray(new int[] { c.getZ(), c.getT(), c
					.getC() });
				if (shapes.containsKey(ztc)) {
					final RealMaskRealInterval in = shapes.get(ztc);
					shapes.put(ztc, in.or(m));
				}
				else {
					shapes.put(ztc, m);
				}
			}
		}
		return shapes;
	}

	private void updateMinMax(final double[] min, final double[] max) {
		for (int i = 0; i < min.length; i++) {
			// NB: It is possible that not all the Shapes have the same number of
			// dimensions. This prevents shapes without a Z, T, or C from setting
			// the min or max in that dimension.
			if (i > 1 && (min[i] == -1 || max[i] == -1)) continue;
			if (min[i] < currentMin[i]) currentMin[i] = min[i];
			if (max[i] > currentMax[i]) currentMax[i] = max[i];
		}
	}

	private void populateMinMax(final double[] min, final double[] max) {
		System.arraycopy(currentMin, 0, min, 0, 2);
		System.arraycopy(currentMax, 0, max, 0, 2);

		int pos = 2;
		if (hasZ) {
			min[pos] = currentMin[2];
			max[pos] = currentMax[2];
			pos++;
		}
		if (hasT) {
			min[pos] = currentMin[3];
			max[pos] = currentMax[3];
			pos++;
		}
		if (hasC) {
			min[pos] = currentMin[4];
			max[pos] = currentMax[4];
		}
	}
}
