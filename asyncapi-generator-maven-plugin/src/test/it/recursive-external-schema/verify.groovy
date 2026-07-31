def generatedSources = new File(basedir, "target/generated-sources/asyncapi")
def kotlinModel = new File(generatedSources, "com/example/recursive/kotlin/TreeNode.kt")
def javaModel = new File(generatedSources, "com/example/recursive/java/TreeNode.java")

assert kotlinModel.isFile() : "Expected the recursive Kotlin model to be generated"
assert kotlinModel.text.contains("val children: List<TreeNode>? = null")
assert javaModel.isFile() : "Expected the recursive Java model to be generated"
assert javaModel.text.contains("List<TreeNode> children")

def classes = new File(basedir, "target/classes")
assert new File(classes, "com/example/recursive/kotlin/TreeNode.class").isFile() :
    "Expected the recursive Kotlin model to compile"
assert new File(classes, "com/example/recursive/java/TreeNode.class").isFile() :
    "Expected the recursive Java model to compile"

["bundled-kotlin.yaml", "bundled-java.yaml"].each { fileName ->
    def bundled = new File(basedir, "target/${fileName}")
    assert bundled.isFile() : "Expected ${fileName} to be generated"
    assert bundled.text.contains("#/components/schemas/TreeNode")
    assert bundled.text.contains("TreeNode:")
    assert !bundled.text.contains("schemas.yaml") : "Expected ${fileName} to contain no external schema reference"
}
