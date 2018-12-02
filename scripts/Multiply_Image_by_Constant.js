#@ int value
#@both ImagePlus imp

imp.getProcessor().multiply(value);
imp.updateAndDraw();
