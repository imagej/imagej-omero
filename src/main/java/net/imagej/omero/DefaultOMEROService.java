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

import io.scif.services.DatasetIOService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imagej.Dataset;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.roi.MaskPredicate;

import org.scijava.Optional;
import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.table.Table;
import org.scijava.table.TableDisplay;
import org.scijava.util.TreeNode;
import org.scijava.util.Types;

/**
 * Default implementation of {@link OMEROService}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultOMEROService extends AbstractService implements
	OMEROService, Optional
{

	// -- Parameters --

	@Parameter
	private LogService log;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ObjectService objectService;

	@Parameter
	private ConvertService convertService;

//-- Fields --

	private final HashMap<OMEROServer, OMEROSession> sessions = new HashMap<>();

	// -- OMEROService methods --

	@Override
	public OMEROSession session(final OMEROServer server,
		final OMEROCredentials credentials) throws OMEROException
	{
		// TODO: Figure out whether we can avoid synchronized every time.
		synchronized (sessions) {
			if (!sessions.containsKey(server)) {
				final OMEROSession session = createSession(server, credentials);
				sessions.put(server, session);
			}
			return session(server);
		}
	}

	@Override
	public OMEROSession session(final OMEROServer server) {
		final OMEROSession session = sessions.get(server);
		// FIXME: If session has been closed, reopen it.
		// Requires support in OMEROSession for reopening.
		if (session != null) return session;
		throw new IllegalStateException("No active session for server " + server);
	}

	@Override
	public OMEROSession createSession(final OMEROServer server,
		final OMEROCredentials credentials) throws OMEROException
	{
		return new OMEROSession(context(), server, credentials);
	}

	@Override
	public omero.grid.Param getJobParam(final ModuleItem<?> item) {
		final omero.grid.Param param = new omero.grid.Param();
		param.optional = !item.isRequired();
		param.prototype = prototype(item.getType());
		param.description = item.getDescription();
		final List<?> choices = item.getChoices();
		if (choices != null && !choices.isEmpty()) {
			param.values = (omero.RList) OMERO.rtype(choices);
		}
		final Object min = item.getMinimumValue();
		if (min != null) param.min = OMERO.rtype(min);
		final Object max = item.getMaximumValue();
		if (max != null) param.max = OMERO.rtype(max);
		return param;
	}

	@Override
	@SuppressWarnings("deprecation")
	public omero.RType prototype(final Class<?> type) {
		// image types
		if (Dataset.class.isAssignableFrom(type) || DatasetView.class
			.isAssignableFrom(type) || ImageDisplay.class.isAssignableFrom(type) ||
			(convertService.supports(type, Dataset.class) && convertService.supports(
				Dataset.class, type)))
		{
			// use an image ID
			return omero.rtypes.rlong(0);
		}

		// table
		if (Table.class.isAssignableFrom(type) || TableDisplay.class
			.isAssignableFrom(type) || (convertService.supports(Table.class, type) &&
				convertService.supports(type, Table.class)))
		{
			// table file ID
			return omero.rtypes.rlong(0);
		}

		// ROI
		// When requesting a TreeNode it is assumed that the number provided is
		// an image ID and you want all the ROIs associated with that image.
		if (TreeNode.class.isAssignableFrom(type) || (convertService.supports(
			TreeNode.class, type) && convertService.supports(type, TreeNode.class)))
			return omero.rtypes.rlong(0);

		if (MaskPredicate.class.isAssignableFrom(type) || (convertService.supports(
			MaskPredicate.class, type) && convertService.supports(type,
				MaskPredicate.class))) return omero.rtypes.rlong(0);

		// primitive types
		final Class<?> saneType = Types.box(type);
		if (Boolean.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rbool(false);
		}
		if (Double.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rdouble(Double.NaN);
		}
		if (Float.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rfloat(Float.NaN);
		}
		if (Integer.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rint(0);
		}
		if (Long.class.isAssignableFrom(saneType)) {
			return omero.rtypes.rlong(0L);
		}

		// data structure types
		if (type.isArray()) {
			return omero.rtypes.rarray();
		}
		if (List.class.isAssignableFrom(type)) {
			return omero.rtypes.rlist();
		}
		if (Map.class.isAssignableFrom(type)) {
			return omero.rtypes.rmap();
		}
		if (Set.class.isAssignableFrom(type)) {
			return omero.rtypes.rset();
		}

		// default case: convert to string
		// works for many types, including but not limited to:
		// - char
		// - java.io.File
		// - java.lang.Character
		// - java.lang.String
		// - java.math.BigDecimal
		// - java.math.BigInteger
		// - org.scijava.util.ColorRGB
		return omero.rtypes.rstring("");
	}

	// -- Disposable methods --

	@Override
	public void dispose() {
		for (final OMEROSession s : sessions.values()) {
			s.close();
		}
		sessions.clear();
	}
}
