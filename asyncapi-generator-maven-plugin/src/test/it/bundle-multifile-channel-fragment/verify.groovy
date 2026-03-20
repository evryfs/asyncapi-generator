def bundled = new File(basedir, "target/bundled.yaml")
assert bundled.exists() : "Expected bundled.yaml to exist"
assert bundled.length() > 0 : "Expected bundled.yaml to be non-empty"

def content = bundled.getText("UTF-8")
assert content.contains("testChannel:") : "Expected external channel key to be present"
assert content.contains("address: \"example.topic\"") : "Expected external channel content to be inlined"
assert content.contains("description: The unique identifier of the user") : "Expected nested external schema fragment to be inlined"
assert !content.contains("channels.yaml#") : "Did not expect external channel ref in bundled output"
assert !content.contains("properties.yaml#") : "Did not expect external schema ref in bundled output"
assert !content.contains("defaultSet:") : "Did not expect internal technical fields in bundled output"

def generatedDir = new File(basedir, "target/generated-sources/asyncapi")
assert !generatedDir.exists() : "Did not expect any generated code when no packages are set"
