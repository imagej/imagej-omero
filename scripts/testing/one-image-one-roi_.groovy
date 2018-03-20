#@net.imagej.Dataset image
#@OUTPUT net.imagej.omero.rois.DataNode dn

import net.imagej.omero.rois.DefaultDataNode
import net.imglib2.roi.geom.GeomMasks

b = GeomMasks.openWritableBox([30, 30] as double[], [100, 100] as double[])
dn = new DefaultDataNode(b, null, null)
