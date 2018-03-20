#@net.imagej.Dataset image
#@int z
#@int time
#@int channel
#@OUTPUT net.imglib2.roi.MaskPredicate mp

import net.imglib2.roi.geom.GeomMasks
import net.imagej.omero.rois.project.OMEROZTCProjectedRealMaskRealInterval

b = GeomMasks.openWritableBox([30, 30] as double[], [100, 100] as double[])
mp = new OMEROZTCProjectedRealMaskRealInterval(b, z, time, channel)