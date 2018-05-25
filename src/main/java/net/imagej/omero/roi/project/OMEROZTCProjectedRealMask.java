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

package net.imagej.omero.roi.project;

import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.KnownConstant;
import net.imglib2.roi.RealMask;

/**
 * Represents a {@link RealMask} positioned at a specific Z, T, and C. Z, T, and
 * C must be positive numbers or negative one. As in OMERO, negative one denotes
 * the entire axis.
 *
 * @author Alison Walter
 */
public class OMEROZTCProjectedRealMask extends AbstractEuclideanSpace implements
	RealMask
{

	private final RealMask source;
	private final int z;
	private final int time;
	private final int channel;

	public OMEROZTCProjectedRealMask(final RealMask source, final int z,
		final int time, final int channel)
	{
		super(5);
		if (z < -1 || time < -1 || channel < -1) throw new IllegalArgumentException(
			"Invalid position (z, time, channel): (" + z + ", " + time + ", " +
				channel + ")");
		this.source = source;
		this.z = z;
		this.time = time;
		this.channel = channel;
	}

	public int getZPosition() {
		return z;
	}

	public int getTimePosition() {
		return time;
	}

	public int getChannelPosition() {
		return channel;
	}

	public RealMask getSource() {
		return source;
	}

	@Override
	public boolean test(final RealLocalizable t) {
		return source.test(t) && checkZTC(t.getDoublePosition(2), t
			.getDoublePosition(3), t.getDoublePosition(4));
	}

	@Override
	public BoundaryType boundaryType() {
		return source.boundaryType();
	}

	@Override
	public KnownConstant knownConstant() {
		return source.knownConstant();
	}

	// -- Helper methods --

	private boolean checkZTC(final double testZ, final double testT,
		final double testC)
	{
		return (z == -1 || z == testZ) && (time == -1 || time == testT) &&
			(channel == -1 || channel == testC);
	}
}
