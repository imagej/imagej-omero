[![](https://travis-ci.org/imagej/imagej-omero.svg?branch=master)](https://travis-ci.org/imagej/imagej-omero)
[![Join the chat at https://gitter.im/imagej/imagej-omero](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/imagej/imagej-omero?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This project provides interoperability between
[ImageJ](http://imagej.net/) and the [OMERO server](http://imagej.net/OMERO).

## ImageJ commands for working with OMERO

There are ImageJ commands for accessing images from a remote OMERO server,
as well as uploading image data from ImageJ to OMERO as a new image.

To try it out, enable the appropriate
[update site](http://imagej.net/Update_Sites):

* __OMERO-5.1__ If your OMERO server is version 5.1.x.
* __OMERO-5.0__ If your OMERO server is version 5.0.x.

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

### Prerequisites

*   [OMERO 5](http://www.openmicroscopy.org/site/support/omero5/sysadmins/unix/server-installation.html)
*   Python 2.7 or later

### Installation

Download the
[ImageJ-OMERO installer](https://raw.githubusercontent.com/imagej/imagej-omero/master/bin/install-imagej),
and run it:

```shell
sh install-imagej <path/to/omero>
```

The installer will:

*   Download and install ImageJ into OMERO's `lib/ImageJ.app` folder,
    with the __Fiji__ and __OMERO-5.1__ update sites enabled.
*   Install OMERO script wrappers for all available ImageJ commands
    into OMERO's `lib/scripts/imagej` folder.

### Using an existing ImageJ installation

If you already have ImageJ installed on the OMERO server machine,
you can use that, rather than installing a new copy of ImageJ:

1.  Enable the __OMERO-5.1__ [update site](http://imagej.net/Update_Sites).
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
    omero script params $(omero script list | grep 'System_Info' | sed 's/|.*//')
    ```

*   Execute the "System Information" command:

    ```shell
    omero script launch $(omero script list | grep 'System_Info' | sed 's/|.*//')
    ```

*   Repeat with any other desired commands.
    Also try from OMERO.web and OMERO.insight!

### Uninstalling

If you wish to remove ImageJ support from OMERO:

```shell
OMERO_PREFIX="/path/to/omero"
rm -rf "$OMERO_PREFIX/lib/scripts/imagej" "$OMERO_PREFIX/lib/ImageJ.app"
```

## Under the hood: a SCIFIO format for OMERO data

This component provides a [SCIFIO](http://imagej.net/SCIFIO) `Format`
implementation which offers transparent read and write access to image pixels
on an OMERO server.

With this format implementation, SCIFIO's `ImgOpener` class can be used to
"open" (i.e., download on demand) an ImgLib2 `ImgPlus` directly from an OMERO
server. The `ImgPlus` will be backed by a `SCIFIOCellImg`, which is backed by
an `OMEROFormat.Reader`, which is backed by an `omero.client` connection.

The `ImgPlus` can then be wrapped as an ImageJ2 `Dataset`, enabling ImageJ2
commands to operate upon it directly.

When changes are made to the local `ImgPlus`'s pixels, those changes happen in
memory, to the `Img`'s "cells"; i.e., paged blocks. As new cells are requested
which push memory consumption beyond desired limits, old dirty cells are cached
out to disk. These cached cells, when present, are used in preference to data
from the original source. In this way, it is possible to iterate over a massive
remote dataset and apply image processing filters, with all changes recorded to
the disk cache, as long as there is sufficient disk space.

Finally, once processing is complete, SCIFIO's `ImgSaver` class can be used to
"save" (i.e., upload) the `ImgPlus` back to OMERO as a new pixels object.
