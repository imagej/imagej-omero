#@net.imglib2.roi.MaskPredicate inROIOne
#@net.imglib2.roi.MaskPredicate inROITwo
#@OUTPUT net.imglib2.roi.MaskPredicate outROI

outROI = inROI.and(inROITwo)