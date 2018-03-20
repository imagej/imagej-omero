#@OMEROService omero
#@String username (label="username")
#@String password (label="password", style=password)
#@String hostname (label="hostname")
#@Integer port (label="port")
#@Long imageID (label="image ID")

import net.imglib2.roi.geom.GeomMasks
import net.imagej.omero.OMEROLocation
import net.imagej.omero.rois.DefaultDataNode

location = new OMEROLocation(hostname, port, username, password)
ellipse = GeomMasks.closedWritableEllipsoid( [20, 23] as double[], [15, 20] as double[])
list = [new DefaultDataNode(ellipse.negate(), null, null)]

ids = omero.uploadROIs(location, list, imageID)

ids.each {
	println "${it}"
}
