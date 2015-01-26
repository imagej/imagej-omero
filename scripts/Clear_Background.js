// @BOTH ImagePlus imp
// @ColorRGB(label = "Color") bg

importClass(Packages.ij.IJ)
IJ.setAutoThreshold(imp, "Otsu");
IJ.run(imp, "Create Mask", "");
IJ.wait(200); // HACK: avoid timing bug
IJ.selectWindow("mask");
mask = IJ.getImage();
IJ.run(mask, "Dilate", "");
IJ.run(mask, "Create Selection", "");
IJ.run(imp, "Restore Selection", "");
IJ.run(mask, "Close", "");
IJ.setBackgroundColor(bg.getRed(), bg.getGreen(), bg.getBlue());
IJ.run(imp, "Clear Outside", "");
IJ.run(imp, "Select None", "");
IJ.resetThreshold(imp);
