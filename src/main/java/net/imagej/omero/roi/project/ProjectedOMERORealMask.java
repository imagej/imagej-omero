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

package net.imagej.omero.roi.project;

import net.imagej.axis.TypedAxis;
import net.imagej.omero.roi.OMERORealMask;
import net.imagej.space.AnnotatedSpace;
import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.BoundaryType;

import omero.gateway.model.ShapeData;

/**
 * An {@link OMERORealMask} which has been adapted to a given space.
 *
 * @author Alison Walter
 * @param <S> the {@link ShapeData} being wrapped
 */
public class ProjectedOMERORealMask<S extends ShapeData> extends
	AbstractEuclideanSpace implements OMERORealMask<S>
{

	private final OMERORealMask<S> shape;
	private final AnnotatedSpace<? extends TypedAxis> space;
	private final ThreadLocal<RealPoint> testPoint;

	public ProjectedOMERORealMask(final OMERORealMask<S> shape,
		final AnnotatedSpace<? extends TypedAxis> space)
	{
		super(space.numDimensions());
		this.shape = shape;
		this.space = space;
		testPoint = new ThreadLocal<RealPoint>() {

			@Override
			protected RealPoint initialValue() {
				return new RealPoint(shape.numDimensions());
			}
		};
	}

	public OMERORealMask<S> getSource() {
		return shape;
	}

	@Override
	public boolean test(final RealLocalizable t) {
		final RealPoint current = testPoint.get();
		for (int i = 0; i < n; i++) {
			final TypedAxis axis = space.axis(i);
			if (!shape.testPosition(axis, t.getDoublePosition(i))) return false;
			if (axis.type().getLabel().equals("X")) current.setPosition(t
				.getDoublePosition(i), 0);
			if (axis.type().getLabel().equals("Y")) current.setPosition(t
				.getDoublePosition(i), 1);
		}

		return shape.test(current);
	}

	@Override
	public BoundaryType boundaryType() {
		return shape.boundaryType();
	}

	@Override
	public S getShape() {
		return shape.getShape();
	}
}
