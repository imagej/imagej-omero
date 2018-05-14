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

package net.imagej.omero;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;

import net.imagej.table.Table;

import org.scijava.log.LogService;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;

/**
 * A {@code List<Table<?, ?>>} whose elements are downloaded lazily from an
 * OMERO server.
 *
 * @author Alison Walter
 */
public class LazyTableList implements List<Table<?, ?>> {

	private List<Table<?, ?>> tables;
	private final long imageID;
	private final OMEROLocation location;
	private final OMEROService omero;
	private final LogService log;

	public LazyTableList(final long imageID, final OMEROLocation location,
		final OMEROService omero, final LogService log)
	{
		this.imageID = imageID;
		this.location = location;
		this.omero = omero;
		this.log = log;
	}

	@Override
	public int size() {
		return getTables().size();
	}

	@Override
	public boolean isEmpty() {
		return getTables().isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		return getTables().contains(o);
	}

	@Override
	public Iterator<Table<?, ?>> iterator() {
		return getTables().iterator();
	}

	@Override
	public Object[] toArray() {
		return getTables().toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return getTables().toArray(a);
	}

	@Override
	public boolean add(final Table<?, ?> e) {
		return getTables().add(e);
	}

	@Override
	public boolean remove(final Object o) {
		return getTables().remove(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return getTables().containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends Table<?, ?>> c) {
		return getTables().addAll(c);
	}

	@Override
	public boolean addAll(final int index,
		final Collection<? extends Table<?, ?>> c)
	{
		return getTables().addAll(index, c);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return getTables().removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return getTables().retainAll(c);
	}

	@Override
	public void clear() {
		getTables().clear();
	}

	@Override
	public Table<?, ?> get(final int index) {
		return getTables().get(index);
	}

	@Override
	public Table<?, ?> set(final int index, final Table<?, ?> element) {
		return getTables().set(index, element);
	}

	@Override
	public void add(final int index, final Table<?, ?> element) {
		getTables().add(index, element);
	}

	@Override
	public Table<?, ?> remove(final int index) {
		return getTables().remove(index);
	}

	@Override
	public int indexOf(final Object o) {
		return getTables().indexOf(o);
	}

	@Override
	public int lastIndexOf(final Object o) {
		return getTables().lastIndexOf(o);
	}

	@Override
	public ListIterator<Table<?, ?>> listIterator() {
		return getTables().listIterator();
	}

	@Override
	public ListIterator<Table<?, ?>> listIterator(final int index) {
		return getTables().listIterator(index);
	}

	@Override
	public List<Table<?, ?>> subList(final int fromIndex, final int toIndex) {
		return getTables().subList(fromIndex, toIndex);
	}

	// -- Helper methods --

	private List<Table<?, ?>> getTables() {
		if (tables == null) retrieveTables();
		return tables;
	}

	private synchronized void retrieveTables() {
		if (tables != null) return;
		List<Table<?, ?>> t = null;
		try {
			t = omero.downloadTables(location, imageID);
		}
		catch (ServerError | PermissionDeniedException
				| CannotCreateSessionException | ExecutionException
				| DSOutOfServiceException | DSAccessException exc)
		{
			log.error("Error retrieving tables", exc);
		}
		tables = t;
	}
}
