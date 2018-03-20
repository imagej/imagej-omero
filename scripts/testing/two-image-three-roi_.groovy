#@net.imagej.Dataset imageOne
#@net.imagej.Dataset imageTwo
#@OUTPUT net.imagej.omero.rois.DataNode dataNodeOne
#@OUTPUT net.imagej.omero.rois.DataNode dataNodeTwo
#@OUTPUT net.imagej.omero.rois.DataNode dataNodeThree

import net.imagej.omero.rois.DefaultDataNode
import net.imglib2.roi.geom.GeomMasks

e = GeomMasks.closedWritableEllipsoid([100, 80] as double[], [20, 10] as double[])
b = GeomMasks.openWritableBox([95, 80] as double[], [150, 107] as double[])
s = GeomMasks.openWritableSphere([70, 20] as double[], 13)

dataNodeOne = new DefaultDataNode(e, null, null)
dataNodeTwo = new DefaultDataNode(b, null, null)
dataNodeThree = new DefaultDataNode(s, null, null)