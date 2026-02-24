def base = new File(basedir, "target/generated-sources/asyncapi")

// Verify that the expected directories were generated based on the per-execution configuration
def aClientDir = new File(base, "com/example/a/client")
def aSchemaDir = new File(base, "com/example/a/schema")
def bClientDir = new File(base, "com/example/b/client")
def bSchemaDir = new File(base, "com/example/b/schema")
def aModelDir = new File(base, "com/example/a/model")
def bModelDir = new File(base, "com/example/b/model")

// Assertions to verify the presence or absence of directories based on the configuration
assert aModelDir.exists() : "Expected model directory for contract A"
assert bModelDir.exists() : "Expected model directory for contract B"
assert aClientDir.exists() : "Expected client directory for contract A (per-execution client.type)"
assert !aSchemaDir.exists() : "Did not expect schema directory for contract A (no schema.type)"
assert !bClientDir.exists() : "Did not expect client directory for contract B (no client.type)"
assert bSchemaDir.exists() : "Expected schema directory for contract B (per-execution schema.type)"
