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

	public void updateServerROIData(final long roiDataId, ROIData shape) {
		downloadedROIs.put(roiDataId, shape);
	}

	public void removeDownloaded(long roiDataId) {
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
