def codegenBase = new File(basedir, "target/generated-sources/asyncapi")

// Verify that the expected directories were generated based on the per-execution configuration
def aClientDir = new File(codegenBase, "com/example/a/client")
def aSchemaDir = new File(codegenBase, "com/example/a/schema")
def bClientDir = new File(codegenBase, "com/example/b/client")
def bSchemaDir = new File(codegenBase, "com/example/b/schema")
def aModelDir = new File(codegenBase, "com/example/a/model")
def bModelDir = new File(codegenBase, "com/example/b/model")

// Assertions to verify the presence or absence of directories based on the configuration
assert aModelDir.exists() : "Expected model directory for contract A"
assert bModelDir.exists() : "Expected model directory for contract B"
assert aClientDir.exists() : "Expected client directory for contract A (per-execution springKafka client)"
assert !aSchemaDir.exists() : "Did not expect schema directory for contract A (no avroProjection schema)"
assert !bClientDir.exists() : "Did not expect client directory for contract B (no springKafka client)"
assert bSchemaDir.exists() : "Expected schema directory for contract B (per-execution avroProjection schema)"
