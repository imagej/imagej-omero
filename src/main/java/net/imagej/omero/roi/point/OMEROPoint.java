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

package net.imagej.omero.roi.point;

import net.imagej.omero.roi.OMERORealMaskRealInterval;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.PointMask;
import net.imglib2.roi.geom.real.WritablePointMask;

import omero.gateway.model.PointData;

/**
 * A {@link PointMask} which wraps an OMERO point roi.
 *
 * @author Alison Walter
 */
public interface OMEROPoint extends OMERORealMaskRealInterval<PointData>,
	WritablePointMask
{

	@Override
	default boolean test(final RealLocalizable l) {
		if (l.getDoublePosition(0) == getShape().getX() && l.getDoublePosition(
			1) == getShape().getY()) return true;
		return false;
	}

	@Override
	default double realMin(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) return getShape().getX();
		return getShape().getY();
	}

	@Override
	default double realMax(final int d) {
		return realMin(d);
	}

	@Override
	default void localize(final float[] position) {
		position[0] = (float) getShape().getX();
		position[1] = (float) getShape().getY();
	}

	@Override
	default void localize(final double[] position) {
		position[0] = getShape().getX();
		position[1] = getShape().getY();
	}

	@Override
	default float getFloatPosition(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		return d == 0 ? (float) getShape().getX() : (float) getShape().getY();
	}

	@Override
	default double getDoublePosition(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		return d == 0 ? getShape().getX() : getShape().getY();
	}

	@Override
	default void move(final float distance, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(getShape().getX() + distance);
		else getShape().setY(getShape().getY() + distance);
	}

	@Override
	default void move(final double distance, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(getShape().getX() + distance);
		else getShape().setY(getShape().getY() + distance);
	}

	@Override
	default void move(final RealLocalizable localizable) {
		getShape().setX(getShape().getX() + localizable.getDoublePosition(0));
		getShape().setY(getShape().getY() + localizable.getDoublePosition(1));
	}

	@Override
	default void move(final float[] distance) {
		getShape().setX(getShape().getX() + distance[0]);
		getShape().setY(getShape().getY() + distance[1]);
	}

	@Override
	default void move(final double[] distance) {
		getShape().setX(getShape().getX() + distance[0]);
		getShape().setY(getShape().getY() + distance[1]);
	}

	@Override
	default void setPosition(final RealLocalizable localizable) {
		getShape().setX(localizable.getDoublePosition(0));
		getShape().setY(localizable.getDoublePosition(1));
	}

	@Override
	default void setPosition(final float[] position) {
		getShape().setX(position[0]);
		getShape().setY(position[1]);
	}

	@Override
	default void setPosition(final double[] position) {
		getShape().setX(position[0]);
		getShape().setY(position[1]);
	}

	@Override
	default void setPosition(final float position, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(position);
		else getShape().setY(position);

	}

	@Override
	default void setPosition(final double position, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(position);
		else getShape().setY(position);
	}

	@Override
	default void fwd(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(getShape().getX() + 1);
		else getShape().setY(getShape().getY() + 1);
	}

	@Override
	default void bck(final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(getShape().getX() - 1);
		else getShape().setY(getShape().getY() - 1);
	}

	@Override
	default void move(final int distance, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(getShape().getX() + distance);
		else getShape().setY(getShape().getY() + distance);
	}

	@Override
	default void move(final long distance, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(getShape().getX() + distance);
		else getShape().setY(getShape().getY() + distance);
	}

	@Override
	default void move(final Localizable localizable) {
		getShape().setX(getShape().getX() + localizable.getLongPosition(0));
		getShape().setY(getShape().getY() + localizable.getLongPosition(1));
	}

	@Override
	default void move(final int[] distance) {
		getShape().setX(getShape().getX() + distance[0]);
		getShape().setY(getShape().getY() + distance[1]);
	}

	@Override
	default void move(final long[] distance) {
		getShape().setX(getShape().getX() + distance[0]);
		getShape().setY(getShape().getY() + distance[1]);
	}

	@Override
	default void setPosition(final Localizable localizable) {
		getShape().setX(localizable.getLongPosition(0));
		getShape().setY(localizable.getLongPosition(1));
	}

	@Override
	default void setPosition(final int[] position) {
		getShape().setX(position[0]);
		getShape().setY(position[1]);
	}

	@Override
	default void setPosition(final long[] position) {
		getShape().setX(position[0]);
		getShape().setY(position[1]);
	}

	@Override
	default void setPosition(final int position, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(position);
		else getShape().setY(position);
	}

	@Override
	default void setPosition(final long position, final int d) {
		if (d < 0 || d > 1) throw new IllegalArgumentException(
			"Invalid dimension: " + d);
		if (d == 0) getShape().setX(position);
		else getShape().setY(position);
	}

}
