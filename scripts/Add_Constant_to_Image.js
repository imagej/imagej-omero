#@ int value
#@both ImagePlus imp

imp.getProcessor().add(value);
imp.updateAndDraw();
