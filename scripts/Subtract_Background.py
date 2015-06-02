# @ImagePlus inputImage
# @double sigma
# @OUTPUT ImagePlus outputImage
from ij import IJ
from ij.plugin import Duplicator, ImageCalculator
backgroundImage = Duplicator().run(inputImage)
IJ.run(backgroundImage, "Gaussian Blur...", "sigma=" + str(sigma))
outputImage = ImageCalculator().run("Subtract create 32-bit", inputImage, backgroundImage)
IJ.run(outputImage, "8-bit", "")
