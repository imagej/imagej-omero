This project provides interoperability between
[ImageJ2](http://developer.imagej.net/) and the
[OMERO server](https://www.openmicroscopy.org/site/support/omero4/).

## ImageJ commands for working with OMERO

There are ImageJ commands for accessing pixels from a remote OMERO server,
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

The code is currently very experimental. If you wish to give it a test drive, the steps are:

### Set up OMERO

*   Build OMERO from [joshmoore](https://github.com/joshmoore)'s
    [jy-scripts](https://github.com/joshmoore/openmicroscopy/compare/jy-scripts)
    branch:

    ```shell
    git clone git://github.com/joshmoore/openmicroscopy
    cd openmicroscopy
    ./build.py
    ```

*   Set your `OMERO_HOME` environment variable to point to the `dist` folder
    of your OMERO build.

### Set up Jython

*   Download the latest
    [pre-built standalone version of Jython](http://jython.org/downloads.html).

*   Download the [Jython launch script](bin/jython) and place in the same
    folder as `jython-standalone-2.5.3.jar`.

### Set up ImageJ2

*   Download [ImageJ2](http://developer.imagej.net/downloads) and unpack into
    `$OMERO_HOME/lib` (it will create a subfolder called `ImageJ.app`).

*   Download the
    [ij-omero](http://jenkins.imagej.net/job/ImageJ-OMERO/lastSuccessfulBuild/artifact/target/ij-omero-0.1.0-SNAPSHOT.jar)
    interoperability library into `$OMERO_HOME/lib/ImageJ.app/jars`.

*   Download the
    [scifio-omero](http://maven.imagej.net/content/repositories/releases/io/scif/scifio-omero/0.2.1/scifio-omero-0.2.1.jar)
    helper library into `$OMERO_HOME/lib/ImageJ.app/jars`.

*   Download the
    [simple-commands](http://jenkins.imagej.net/job/ImageJ-tutorials/lastSuccessfulBuild/artifact/simple-commands/target/simple-commands-1.0.0-SNAPSHOT.jar)
    and/or
    [widget-demo](http://jenkins.imagej.net/job/ImageJ-tutorials/lastSuccessfulBuild/artifact/widget-demo/target/widget-demo-1.0.0-SNAPSHOT.jar)
    tutorial plugins into `$OMERO_HOME/lib/ImageJ.app/plugins`.

*   Download the [ImageJ script generator](bin/genScripts.jy), and run it:

    ```shell
    jython genScripts.jy
    ```

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
    omero script params $(omero script list | grep 'Hello,_World' | sed 's/|.*//')
    ```

*   Execute the `HelloWorld` command:

    ```shell
    omero script launch $(omero script list | grep 'Hello,_World' | sed 's/|.*//')
    ```

*   Repeat with any other desired commands.
    Also try from OMERO.web and OMERO.insight!

## See also

This project makes use of the
[scifio-omero](https://github.com/scifio/scifio-omero) library for reading and
writing OMERO pixels using the [SCIFIO](http://scif.io/) library.
