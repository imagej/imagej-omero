#@net.imagej.Dataset image
#@OUTPUT net.imagej.table.Table table
#@OUTPUT java.util.List l

import net.imagej.table.DefaultIntTable
import net.imglib2.roi.geom.GeomMasks

int[][] data = [[1, 2, 3], [-10, -20, -80], [1707, -2000, 63], [0, 15, -53]]
table = new DefaultIntTable(3, 4)

for(int i = 0; i < table.getColumnCount(); i++) {
	table.setColumnHeader(i, "Header " + i)
}

for(int r = 0; r < table.getRowCount(); r++) {
	for(int c = 0; c < table.getColumnCount(); c++) {
		table.setValue(c, r, data[r][c])
	}
}

e = GeomMasks.closedWritableEllipsoid([100, 100] as double[], [30, 15] as double[])
b = GeomMasks.closedWritableBox([20, 20] as double[], [40, 90] as double[])
s = GeomMasks.closedWritableSphere([120, 70] as double[], 20)

l = [e, b, s]