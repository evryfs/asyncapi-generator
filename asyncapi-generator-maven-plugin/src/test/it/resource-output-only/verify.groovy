def resourceBase = new File(basedir, "target/custom-resources")
def schemaDir = new File(resourceBase, "com/example/avro")
def generatedSchema = new File(schemaDir, "SampleMessage.avsc")
def classpathSchema = new File(
    basedir,
    "target/classes/com/example/avro/SampleMessage.avsc",
)

assert schemaDir.exists() : "Expected resource output directory to exist"
assert generatedSchema.isFile() : "Expected an Avro schema in the configured output directory"
assert classpathSchema.isFile() : "Expected the generated Avro schema on the runtime classpath"
