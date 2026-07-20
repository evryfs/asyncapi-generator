def codegenBase = new File(basedir, "target/generated-sources/asyncapi")

// define where the generated files should be for each contract, based on the configuration in the pom.xml
def aClientDir = new File(codegenBase, "com/example/a/client")
def aSchemaDir = new File(codegenBase, "com/example/a/schema")
def bClientDir = new File(codegenBase, "com/example/b/client")
def bSchemaDir = new File(codegenBase, "com/example/b/schema")
def aModelDir = new File(codegenBase, "com/example/a/model")
def bModelDir = new File(codegenBase, "com/example/b/model")

// assert that the expected directories exist or do not exist based on the configuration in the pom.xml
assert aModelDir.exists() : "Expected model directory for contract A"
assert bModelDir.exists() : "Expected model directory for contract B"
assert aClientDir.exists() : "Expected client directory for contract A (execution client configuration)"
assert aSchemaDir.exists() : "Expected schema directory for contract A (global avroProjection schema)"
assert !bClientDir.exists() : "Did not expect client directory for contract B (disabled springKafka client)"
assert !bSchemaDir.exists() : "Did not expect schema directory for contract B (disabled avroProjection schema)"
