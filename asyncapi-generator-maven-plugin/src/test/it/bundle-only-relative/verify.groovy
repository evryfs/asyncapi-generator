def bundled = new File(basedir, "bundled/relative-bundled.yaml")
assert bundled.exists() : "Expected bundled/relative-bundled.yaml to exist"
assert bundled.length() > 0 : "Expected bundled/relative-bundled.yaml to be non-empty"

def generatedDir = new File(basedir, "target/generated-sources/asyncapi")
assert !generatedDir.exists() : "Did not expect any generated code when no packages are set"
