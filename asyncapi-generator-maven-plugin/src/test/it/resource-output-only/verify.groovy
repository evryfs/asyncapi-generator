def resourceBase = new File(basedir, "target/custom-resources")

def schemaDir = new File(resourceBase, "com/example/avro")

assert schemaDir.exists() : "Expected resource output directory to exist"
assert schemaDir.list().length > 0 : "Expected Avro schema files in resource output"
