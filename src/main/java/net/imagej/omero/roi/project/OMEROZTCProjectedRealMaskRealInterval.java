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

import net.imglib2.roi.RealMaskRealInterval;

/**
 * Represents a {@link RealMaskRealInterval} positioned at a specific Z, T, and
 * C. Z, T, and C must be positive numbers or negative one. As in OMERO,
 * negative one denotes the entire axis.
 *
 * @author Alison Walter
 */
public class OMEROZTCProjectedRealMaskRealInterval extends
	OMEROZTCProjectedRealMask implements RealMaskRealInterval
{

	private final RealMaskRealInterval source;

	public OMEROZTCProjectedRealMaskRealInterval(
		final RealMaskRealInterval source, final int z, final int time,
		final int channel)
	{
		super(source, z, time, channel);
		this.source = source;
	}

	@Override
	public RealMaskRealInterval getSource() {
		return source;
	}

	@Override
	public double realMin(final int d) {
		if (d == 2) return getZPosition();
		if (d == 3) return getTimePosition();
		if (d == 4) return getChannelPosition();
		return source.realMin(d);
	}

	@Override
	public double realMax(final int d) {
		if (d == 2) return getZPosition();
		if (d == 3) return getTimePosition();
		if (d == 4) return getChannelPosition();
		return source.realMax(d);
	}
}
