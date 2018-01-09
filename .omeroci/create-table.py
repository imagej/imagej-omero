#! /usr/bin/env python

from omero.gateway import BlitzGateway
from omero.grid import LongColumn, StringColumn
from omero.model import FileAnnotationI, OriginalFileI, ImageAnnotationLinkI, ImageI

# Connect to OMERO
conn = BlitzGateway("root", "omero", host="omero", port=4064)
conn.connect()

# Create columns
col1 = LongColumn('Header 1', '', [])
col2 = LongColumn('Header 2', '', [])
col3 = StringColumn('Header 3', '', 64, [])
columns = [col1, col2, col3]

# Initialize new table
resources = conn.c.sf.sharedResources()
repo_id = resources.repositories().descriptions[0].getId().getValue()
table = resources.newTable(repo_id, "test-table")
table.initialize(columns)

# Add data to table
c1_data = [10, 20, 30]
c2_data = [-1000, 0, 1819]
c3_data = ["hi", "hello", "hey"]
data1 = LongColumn('Header 1', '', c1_data)
data2 = LongColumn('Header 2', '', c2_data)
data3 = StringColumn('Header 3', '', 64, c3_data)
data = [data1, data2, data3]
table.addData(data)
table.close()

# Attach to an Image
orig_file_id = table.getOriginalFile().id.val
print "Original File Id: ", orig_file_id
file_ann = FileAnnotationI()
file_ann.setFile(OriginalFileI(orig_file_id, False))
file_ann = conn.getUpdateService().saveAndReturnObject(file_ann)
link = ImageAnnotationLinkI()
link.setParent(ImageI(1, False))
link.setChild(file_ann)
conn.getUpdateService().saveObject(link)

conn.close()
