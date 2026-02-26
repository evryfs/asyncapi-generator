def codegenBase = new File(basedir, "target/custom-codegen")

def modelDir = new File(codegenBase, "com/example/codegen")

assert modelDir.exists() : "Expected codegen output directory to exist"
assert modelDir.list().length > 0 : "Expected generated source files in codegen output"
