#@net.imagej.Dataset imageOne
#@OUTPUT java.util.List l

import net.imagej.omero.rois.DefaultDataNode
import net.imglib2.roi.geom.GeomMasks

e = GeomMasks.closedWritableEllipsoid([100, 100] as double[], [30, 15] as double[])
b = GeomMasks.closedWritableBox([20, 20] as double[], [40, 90] as double[])
s = GeomMasks.closedWritableSphere([120, 70] as double[], 20)

dnOne = new DefaultDataNode(e, null, null)
dnTwo = new DefaultDataNode(b, null, null)
dnThree = new DefaultDataNode(s, null, null)

l = [dnOne, dnTwo, dnThree]