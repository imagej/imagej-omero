/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2015 Open Microscopy Environment:
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

package net.imagej.omero.commands;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.omero.OMEROCommand;
import net.imagej.omero.OMEROLocation;
import net.imagej.omero.OMEROService;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RealRandomAccessibleRealInterval;
import net.imglib2.roi.Mask;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.logic.BoolType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;

/** An ImageJ command for downloading ROIs from an OMERO server. */
@Plugin(type = Command.class, label = "Import ROI from OMERO", menu = { @Menu(
	label = MenuConstants.FILE_LABEL, weight = MenuConstants.FILE_WEIGHT,
	mnemonic = MenuConstants.FILE_MNEMONIC), @Menu(label = "Import", weight = 6),
	@Menu(label = "OMERO ROIs...", weight = 100, mnemonic = 'o') })
public class OpenROIsFromOMERO extends OMEROCommand {

	@Parameter
	private LogService log;

	@Parameter
	private OMEROService omeroService;

	@Parameter
	private long imageID;

	@Override
	public void run() {
		try {
			final OMEROLocation credentials = new OMEROLocation(getServer(),
				getPort(), getUser(), getPassword());
			final List<MaskPredicate<?>> ijROIs = omeroService.downloadROIs(
				credentials, imageID);
			for (final MaskPredicate<?> ijROI : ijROIs) {
				if (ijROI instanceof RealMask) display((RealMask) ijROI);
				else if (ijROI instanceof Mask) display((Mask) ijROI);
				else throw new IllegalArgumentException("Uh oh ...");
			}
		}
		catch (final ServerError exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final PermissionDeniedException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final CannotCreateSessionException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final DSOutOfServiceException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final ExecutionException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final DSAccessException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error talking to OMERO: " + exc.getMessage());
		}
		catch (final URISyntaxException exc) {
			log.error(exc);
			exc.printStackTrace();
			cancel("Error creating URI for Session: " + exc.getMessage());
		}
	}

	private void display(final RealMask rm) {
		if (rm instanceof RealMaskRealInterval) {
			final RealRandomAccessibleRealInterval<BoolType> rrari = Masks
				.toRealRandomAccessibleRealInterval((RealMaskRealInterval) rm);
			BdvFunctions.show(rrari, Intervals.smallestContainingInterval(rrari), "",
				Bdv.options());
		}
		else {
			BdvFunctions.show(Masks.toRealRandomAccessible(rm), createInterval(rm
				.numDimensions()), "", Bdv.options());
		}
	}

	private void display(final Mask m) {
		if (m instanceof MaskInterval) {
			BdvFunctions.show(Masks.toRandomAccessibleInterval((MaskInterval) m), "",
				Bdv.options());
		}
		else {
			BdvFunctions.show(Views.interval(Masks.toRandomAccessible(m),
				createInterval(m.numDimensions())), "", Bdv.options());
		}
	}

	private Interval createInterval(final int numDims) {
		final long[] min = new long[numDims];
		final long[] max = new long[numDims];
		for (int i = 0; i < numDims; i++) {
			min[i] = 0;
			max[i] = 500;
		}
		return new FinalInterval(min, max);
	}

}
