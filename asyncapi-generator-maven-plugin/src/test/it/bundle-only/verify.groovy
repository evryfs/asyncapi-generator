def bundled = new File(basedir, "target/bundled.yaml")
assert bundled.exists() : "Expected bundled.yaml to exist"
assert bundled.length() > 0 : "Expected bundled.yaml to be non-empty"

def generatedDir = new File(basedir, "target/generated-sources/asyncapi")
assert !generatedDir.exists() : "Did not expect any generated code when no packages are set"
