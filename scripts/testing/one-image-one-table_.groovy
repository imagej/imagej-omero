#@net.imagej.Dataset image
#@OUTPUT net.imagej.table.Table table

import net.imagej.table.DefaultIntTable

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