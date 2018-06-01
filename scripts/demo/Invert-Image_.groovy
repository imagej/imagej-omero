#@OpService ops
#@net.imagej.Dataset imageIn
#@OUTPUT net.imagej.Dataset(attachToOMERODatasetIDs="2") imageOut

imageOut = ops.copy().img(imageIn)
ops.image().invert(imageOut, imageIn)