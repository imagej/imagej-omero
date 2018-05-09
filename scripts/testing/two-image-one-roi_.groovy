#@net.imagej.Dataset imageOne
#@net.imagej.Dataset imageTwo
#@OUTPUT net.imagej.omero.rois.DataNode dataNode

import net.imagej.omero.rois.DefaultDataNode
import net.imglib2.roi.geom.GeomMasks

e = GeomMasks.closedWritableEllipsoid([100, 80] as double[], [20, 10] as double[])

dataNode = new DefaultDataNode(e, null, null)