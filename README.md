This project provides interoperability between
[ImageJ](http://developer.imagej.net/) and the
[OMERO server](https://www.openmicroscopy.org/site/support/omero5/).

## ImageJ commands for working with OMERO

There are ImageJ commands for accessing images from a remote OMERO server,
as well as uploading image data from ImageJ to OMERO as a new image.

To try it out, enable the __OMERO-5.0__
[update site](http://wiki.imagej.net/Update_Sites).

You will then have the following new menu items:

* File > Import > OMERO...
* File > Export > OMERO...

## Calling ImageJ commands as OMERO scripts

You can execute ImageJ modules (commands, scripts, etc.) on the server side as
OMERO scripts.

Translation of some complex data types is not yet implemented. In particular,
ImageJ results tables are not translated to and from OMERO.tables structures,
and ImageJ regions of interest (ROIs) are not translated to/from OMERO ROIs.

If you wish to give it a test drive, the steps are:

### Install prerequisites

*   [Install OMERO 5](http://www.openmicroscopy.org/site/support/omero5/sysadmins/unix/server-installation.html).
*   Install Jython using your package manager, or
    [from the website](https://wiki.python.org/jython/InstallationInstructions).

### Install ImageJ

If you have not yet installed ImageJ on the OMERO server machine, then
download the [ImageJ-OMERO installer](bin/install-imagej), and run it:

```shell
sh install-imagej <path/to/omero>
```

The installer will:

*   Download and install ImageJ into OMERO's `lib/ImageJ.app` folder,
    with the Fiji and OMERO-5.0 update sites enabled.
*   Install OMERO script wrappers for all available ImageJ commands
    into OMERO's `lib/scripts/imagej` folder.

Alternately, if you wish to use an existing ImageJ installation:

1.  Enable the __OMERO-5.0__
    [update site](http://wiki.imagej.net/Update_Sites).
2.  Run `gen-scripts` in ImageJ's `lib` directory.

### Take it for a spin

*   Fire up OMERO:

    ```shell
    omero admin start
    ```

*   List available scripts:

    ```shell
    omero script list
    ```

*   List parameters of "System Information" command:

    ```shell
    omero script params $(omero script list | grep 'System Info' | sed 's/|.*//')
    ```

*   Execute the "System Information" command:

    ```shell
    omero script launch $(omero script list | grep 'System Info' | sed 's/|.*//')
    ```

*   Repeat with any other desired commands.
    Also try from OMERO.web and OMERO.insight!

### Uninstalling

If you wish to remove ImageJ support from OMERO:

```shell
OMERO_DIR="/path/to/omero"
rm -rf "$OMERO_DIR/lib/scripts/imagej" "$OMERO_DIR/lib/ImageJ.app"
```

## See also

This project makes use of the
[scifio-omero](https://github.com/scifio/scifio-omero) library for reading and
writing OMERO images using the [SCIFIO](http://scif.io/) library.
