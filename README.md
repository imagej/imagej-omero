This project provides interoperability between
[ImageJ](http://developer.imagej.net/) and the
[OMERO server](https://www.openmicroscopy.org/site/support/omero5/).

## ImageJ commands for working with OMERO

There are ImageJ commands for accessing images from a remote OMERO server,
as well as uploading image data from ImageJ to OMERO as a new image.

To try it out, drop the `ij-omero` JAR file, along with its dependencies, into
your ImageJ plugins folder. Launch ImageJ and there will be two new commands:

* File > Import > OMERO...
* File > Export > OMERO...

## Calling ImageJ commands as OMERO scripts

This project enables execution of ImageJ commands on the server side as OMERO
scripts.

The following ImageJ commands are tested and working:

* [HelloWorld](https://github.com/imagej/imagej-tutorials/blob/0bbd12e3/simple-commands/src/main/java/HelloWorld.java):
  a basic example with one string input, and one string output.
* [WidgetDemo](https://github.com/imagej/imagej-tutorials/blob/0bbd12e3/widget-demo/src/main/java/WidgetDemo.java):
  an example exercising many different parameter types, providing a good
  illustration of how type conversion works going back and forth between ImageJ
  and OMERO.
* [ComputeStats](https://github.com/imagej/imagej-tutorials/blob/0bbd12e3/simple-commands/src/main/java/ComputeStats.java):
  an example which takes an image as input and produces numbers.
* [GradientImage](https://github.com/imagej/imagej-tutorials/blob/0bbd12e3/simple-commands/src/main/java/GradientImage.java):
  an example which takes numbers as input and produces an image.

Translation of some complex data types is not yet implemented. In particular,
ImageJ results tables are not translated to and from OMERO.tables structures.

If you wish to give it a test drive, the steps are:

### Set up OMERO

*   [Install OMERO 5](http://www.openmicroscopy.org/site/support/omero5/sysadmins/unix/server-installation.html)
    if you have not already done so.

*   Set your `OMERO_HOME` environment variable to point to your OMERO
    installation.

### Set up Jython

*   Download the latest
    [pre-built standalone version of Jython](http://jython.org/downloads.html).

*   Download the [Jython launch script](bin/jython) and place in the same
    folder as the `jython-standalone` JAR file. Add this folder to your `PATH`.

### Set up ImageJ2

Download the [ImageJ-OMERO installer](bin/imagej-omero), and run it:

```shell
sh imagej-omero
```

The installer performs the following steps:

*   Installs ImageJ2 into `$OMERO_HOME/lib/ImageJ.app`.
*   Installs `ij-omero` and `scifio-omero` into `ImageJ.app/jars`.
*   Installs `simple-commands` and `widget-demo` into `ImageJ.app/plugins`.
*   Installs OMERO script wrappers for all available ImageJ commands into
    `$OMERO_HOME/lib/scripts/imagej`.

### Take it for a spin

*   Fire up OMERO:

    ```shell
    omero admin start
    ```

*   List available scripts:

    ```shell
    omero script list
    ```

*   List parameters of `HelloWorld` command:

    ```shell
    omero script params $(omero script list | grep Hello | sed 's/|.*//')
    ```

*   Execute the `HelloWorld` command:

    ```shell
    omero script launch $(omero script list | grep Hello | sed 's/|.*//')
    ```

*   Repeat with any other desired commands.
    Also try from OMERO.web and OMERO.insight!

## See also

This project makes use of the
[scifio-omero](https://github.com/scifio/scifio-omero) library for reading and
writing OMERO images using the [SCIFIO](http://scif.io/) library.
