#@ ImagePlus imp
#@ OUTPUT ImagePlus output

import ij.IJ

imp2 = imp.duplicate();
IJ.setAutoThreshold(imp2, "Default");
IJ.run(imp2, "Convert to Mask", "");
IJ.run(imp2, "Analyze Particles...", "  show=Overlay");
table = imp2.getOverlay().measure(imp);
imp2.setProperty("tables", [table])
output = imp2
