#@OMEROService omero
#@String username (label="username")
#@String password (label="password", style=password)
#@String hostname (label="hostname")
#@Integer port (label="port")
#@Long imageID (label="image ID")

import net.imglib2.roi.geom.GeomMasks
import net.imagej.omero.OMEROLocation
import net.imglib2.roi.Masks;
import net.imglib2.util.Intervals;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;


location = new OMEROLocation(hostname, port, username, password)
dns = omero.downloadROIs(location, imageID)


dns.each{ dn ->
	dn.children().each { element ->
		rrari = Masks.toRealRandomAccessibleRealInterval(element.getData())
		BdvFunctions.show(rrari, Intervals.smallestContainingInterval(rrari), "", BdvOptions.options().is2D());
	}
}