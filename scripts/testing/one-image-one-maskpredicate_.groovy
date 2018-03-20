#@net.imagej.Dataset image
#@OUTPUT net.imglib2.roi.MaskPredicate mp

import net.imglib2.roi.geom.GeomMasks

mp = GeomMasks.openWritableBox([30, 30] as double[], [100, 100] as double[])