// @File(label="Multi series file") file
// @String(label="Fusion method", choices={"Linear Blending", "Average", "Median", "Max. Intensity", "Min. Intensity", "Intensity from random input tile", "Do not fuse images (only write TileConfiguration)"}) fusion_method
// @double(label="Regression threshold") regression_threshold
// @double(label="Max/avg displacement threshold") max_avg_displacement_threshold
// @double(label="Absolute displacement threshold") absolute_displacement_threshold
// @boolean(label="Add tiles as ROIs") add_tiles_as_rois
// @boolean(label="Compute overlap (otherwise trust coordinates in the file") compute_overlap
// @boolean(label="Ignore Calibration") ignore_calibration
// @int(label="Increase overlap [%]", min=0, max=100) increase_overlap
// @boolean(label="Invert X coordinates") invert_x
// @boolean(label="Invert Y coordinates") invert_y
// @boolean(label="Ignore Z stage position") ignore_z_stage
// @boolean(label="Subpixel accuracy") subpixel_accuracy
// @boolean(label="Downsample tiles") downsample_tiles
// @boolean(label="Display fusion") display_fusion
// @boolean(label="Use virtual input images") use_virtual_input_images
// @String(label="Computation parameters", choices={"Save memory (but be slower)", "Save computation time (but use more RAM)"}) computation_parameters
// @OUTPUT ImagePlus fused_image

arg = "type=[Positions from file] order=[Defined by image metadata]" +
	" browse=[" + file.getAbsolutePath() + "]" +
	" multi_series_file=[" + file.getAbsolutePath() + "]" +
	" fusion_method=[" + fusion_method + "]" +
	" regression_threshold=" regression_threshold +
	" max/avg_displacement_threshold=" + max_avg_displacement_threshold +
	" absolute_displacement_threshold=" + absolute_displacement_threshold;

if (add_tiles_as_rois)        arg += " add_tiles_as_rois";
if (compute_overlap)          arg += " compute_overlap";
if (ignore_calibration)       arg += " ignore_calibration";
if (increase_overlap=)        arg += " increase_overlap=";
if (invert_x)                 arg += " invert_x";
if (invert_y)                 arg += " invert_y";
if (ignore_z_stage)           arg += " ignore_z_stage";
if (subpixel_accuracy)        arg += " subpixel_accuracy";
if (downsample_tiles)         arg += " downsample_tiles";
if (display_fusion)           arg += " display_fusion";
if (use_virtual_input_images) arg += " use_virtual_input_images";

arg += " computation_parameters=[" + computation_parameters + "]" +
	" image_output=[Fuse and display]");

run("Grid/Collection stitching", arg);

fused_image = IJ.getImage();
