var colls = db.getCollectionNames()
for(var i = 0; i < colls.length; i++) {
	db.getCollection(colls[i]).drop()
}