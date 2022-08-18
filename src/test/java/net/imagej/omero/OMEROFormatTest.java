/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2022 Open Microscopy Environment:
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.scif.Checker;
import io.scif.FormatException;
import io.scif.SCIFIO;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.io.location.FileLocation;

/**
 * Tests {@link OMEROFormat}.
 *
 * @author Curtis Rueden
 */
public class OMEROFormatTest {

	private SCIFIO scifio;

	@Before
	public void setUp() {
		scifio = new SCIFIO();
	}

	@After
	public void tearDown() {
		if (scifio != null) scifio.context().dispose();
	}

	/** Tests {@link OMEROFormat#getFormatName()}. */
	@Test
	public void testGetFormatName() {
		final OMEROFormat omeroFormat = getFormat();
		assertEquals("OMERO", omeroFormat.getFormatName());
	}

	/** Tests the {@link OMEROFormat.Checker}. */
	@Test
	public void testChecker() throws FormatException {
		final OMEROFormat omeroFormat = getFormat();
		final Checker checker = omeroFormat.createChecker();
		assertTrue(checker.isFormat(new OMEROLocation(new OMEROServer("example.com",
			false), "/path/to/image")));
		assertFalse(checker.isFormat(new FileLocation("asdf")));
		assertFalse(checker.isFormat(new FileLocation("asdf.omero")));
		assertFalse(checker.isFormat(new FileLocation("omero:asdf")));
		assertEquals("omero", omeroFormat.getSuffixes()[0]);
	}

	// -- Helper methods --

	private OMEROFormat getFormat() {
		return scifio.format().getFormatFromClass(OMEROFormat.class);
	}
}
