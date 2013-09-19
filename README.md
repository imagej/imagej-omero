This project provides interoperability between
[ImageJ2](http://developer.imagej.net/) and the
[OMERO server](https://www.openmicroscopy.org/site/support/omero4/).

## ImageJ commands for working with OMERO pixels

There are ImageJ commands for accessing pixels from a remote OMERO server,
as well as uploading image data from ImageJ to OMERO as a new image.

To try it out, drop the `ij-omero` JAR file, along with its dependencies, into
your ImageJ plugins folder. Launch ImageJ and there will be two new commands:

* File > Import > OMERO...
* File > Export > OMERO...

## Calling ImageJ commands as OMERO scripts

This project enables execution of ImageJ commands on the server side as OMERO
scripts.

The following ImageJ tutorial commands are tested and working:

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

1. Build OMERO from [joshmoore](https://github.com/joshmoore)'s
   [jy-scripts](https://github.com/joshmoore/openmicroscopy/compare/jy-scripts)
   branch.

2. Download [ImageJ2](http://developer.imagej.net/downloads) and unpack into
   `$OMERO_HOME/dist/lib` (it will create a subfolder called `ImageJ.app`).

3. Download the
   [ij-omero](http://jenkins.imagej.net/job/ImageJ-OMERO/lastSuccessfulBuild/artifact/target/ij-omero-0.1.0-SNAPSHOT.jar)
   interoperability library into `$OMERO_HOME/dist/lib/ImageJ.app/jars`.

4. Download
   [scifio-omero-0.2.1.jar](http://maven.imagej.net/content/repositories/releases/io/scif/scifio-omero/0.2.1/scifio-omero-0.2.1.jar)
   into `$OMERO_HOME/dist/lib/ImageJ.app/jars`.

5. Download the
   [simple-commands](http://jenkins.imagej.net/job/ImageJ-tutorials/lastSuccessfulBuild/artifact/simple-commands/target/simple-commands-1.0.0-SNAPSHOT.jar)
   and/or
   [widget-demo](http://jenkins.imagej.net/job/ImageJ-tutorials/lastSuccessfulBuild/artifact/widget-demo/target/widget-demo-1.0.0-SNAPSHOT.jar)
   tutorial plugins into `$OMERO_HOME/dist/lib/ImageJ.app/plugins`.

6. Download the latest [pre-built standalone version of Jython](http://jython.org/downloads.html).

7. Create a `jython` launch script _**on your path**_:

    ```shell
    #!/bin/sh
    export OMERO_HOME="$HOME/code/ome/openmicroscopy/dist"
    export JYTHON_LIB="$HOME/bin/jython-standalone-2.5.3.jar"
    export IMAGEJ_JARS="$OMERO_HOME/lib/ImageJ.app/jars/*"
    export IMAGEJ_PLUGINS="$OMERO_HOME/lib/ImageJ.app/plugins/*"
    export JYTHON_CLASSPATH="$JYTHON_LIB:$IMAGEJ_JARS:$IMAGEJ_PLUGINS:$CLASSPATH"
    java -cp "$JYTHON_CLASSPATH" org.python.util.jython $@
    ```

8. To generate the scripts, launch `jython`:

    ```shell
    (export CLASSPATH="$OMERO_HOME/lib/server/*" && jython)
    ```

   And then execute:

    ```python
    import os, imagej.omero.ScriptGenerator as sg
    scriptDir = os.getenv('OMERO_HOME') + '/lib/scripts/imagej'
    os.mkdir(scriptDir)
    sg.main(scriptDir)
    ```

9. `omero admin start` (if you haven't already)

10. `omero script list`

    You should see all the ImageJ modules as available options.

11. `omero script params $(omero script list | grep HelloWorld | sed 's/|.*//')`

12. `omero script launch $(omero script list | grep HelloWorld | sed 's/|.*//')`

## See also

This project makes use of the
[scifio-omero](https://github.com/scifio/scifio-omero) library for reading and
writing OMERO pixels using the [SCIFIO](http://scif.io/) library.
