def base = new File(basedir, "target/generated-sources/asyncapi")

// define where the generated files should be for each contract, based on the configuration in the pom.xml
def aClientDir = new File(base, "com/example/a/client")
def aSchemaDir = new File(base, "com/example/a/schema")
def bClientDir = new File(base, "com/example/b/client")
def bSchemaDir = new File(base, "com/example/b/schema")
def aModelDir = new File(base, "com/example/a/model")
def bModelDir = new File(base, "com/example/b/model")

// assert that the expected directories exist or do not exist based on the configuration in the pom.xml
assert aModelDir.exists() : "Expected model directory for contract A"
assert bModelDir.exists() : "Expected model directory for contract B"
assert aClientDir.exists() : "Expected client directory for contract A (global client.type)"
assert aSchemaDir.exists() : "Expected schema directory for contract A (global schema.type)"
assert !bClientDir.exists() : "Did not expect client directory for contract B (overridden client.type)"
assert !bSchemaDir.exists() : "Did not expect schema directory for contract B (overridden schema.type)"
