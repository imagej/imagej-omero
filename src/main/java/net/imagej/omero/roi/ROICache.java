/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2013 - 2023 Open Microscopy Environment:
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

package net.imagej.omero.roi;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import omero.gateway.model.ROIData;

public class ROICache {

	private final Map<Object, ROIData> savedRois = new IdentityHashMap<>();

	private final Map<Long, ROIData> downloadedROIs = new HashMap<>();

	/**
	 * Returns the most recently retrieved {@link ROIData} object with the given
	 * ID. If no such mapping exists {@code null} is returned.
	 * <p>
	 * When a ROIData object is updated and the new version uploaded to the OMERO
	 * server, previous ROIData objects become "locked" and subsequent attempts to
	 * update and upload new versions of them to OMERO will result in
	 * {@link ome.conditions.OptimisticLockException}. Therefore, pairings of the
	 * id and the update OMERO server ROIData object must be stored.
	 * </p>
	 */
	public ROIData getUpdatedServerROIData(final long roiDataId) {
		return downloadedROIs.get(roiDataId);
	}

	public void updateServerROIData(final long roiDataId, final ROIData shape) {
		downloadedROIs.put(roiDataId, shape);
	}

	public void removeDownloaded(final long roiDataId) {
		downloadedROIs.remove(roiDataId);
	}

	public void removeSaved(final long roiDataId) {
		for (final Object key : savedRois.keySet()) {
			if (savedRois.get(key).getId() == roiDataId) {
				savedRois.remove(key);
			}
		}
	}

	/**
	 * Add a mapping between a ROIs originating from ImageJ and an OMERO
	 * {@link ROIData}.
	 */
	public void addROIMapping(final Object roi, final ROIData shape) {
		savedRois.put(roi, shape);
	}

	/**
	 * Retrieve the {@link ROIData} associated with this key. Returns {@code null}
	 * if there's no mapping.
	 */
	public ROIData getROIMapping(final Object key) {
		return savedRois.get(key);
	}

	/** Returns all the keys for mapped ROIs. */
	public Set<Object> getROIMappingKeys() {
		return Collections.unmodifiableSet(savedRois.keySet());
	}

	/**
	 * Removes the {@code Object} {@link ROIData} mapping associated with the
	 * given key from the stored ROIs.
	 */
	public void removeROIMapping(final Object key) {
		savedRois.remove(key);
	}

	/** Removes all {@code Object} {@link ROIData} mappings. */
	public void clearROIMappings() {
		savedRois.clear();
	}
}
