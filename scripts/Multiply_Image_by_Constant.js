// @int value
// @BOTH ImagePlus imp

imp.getProcessor().multiply(value);
imp.updateAndDraw();
