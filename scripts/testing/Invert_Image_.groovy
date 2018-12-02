#@ OpService ops
#@ net.imagej.Dataset imageIn
#@output net.imagej.Dataset imageOut

imageOut = ops.copy().img(imageIn)
ops.image().invert(imageOut, imageIn)
