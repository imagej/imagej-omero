This repository contains code for interoperability between
[SCIFIO](http://scif.io/) and the
[OMERO server](https://www.openmicroscopy.org/site/support/omero4/).

In particular, it provides a SCIFIO `Format` implementation which offers
transparent read and write access to image pixels on an OMERO server.

See also [ImageJ-OMERO](https://github.com/imagej/imagej-omero).

## Usage and benefits

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
