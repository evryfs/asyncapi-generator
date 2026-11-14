package systemtest.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import kotlin.test.assertTrue

/**
 * Reads AsyncAPI documents and schemas written by the generator.
 *
 * Paths are relative to the consumer project, which is the working directory
 * of both Maven Surefire and Gradle test runs.
 */
object Documents {
    private val yaml = ObjectMapper(YAMLFactory())
    private val json = ObjectMapper()

    /** Parses a YAML or JSON file written into the consumer project. */
    fun read(path: String): JsonNode {
        val file = File(path)
        assertTrue(file.isFile, "Expected generated document at ${file.absolutePath}")
        return parse(file.readText(), file.extension)
    }

    /** Parses YAML or JSON content; [extension] selects the parser. */
    fun parse(content: String, extension: String): JsonNode =
        (if (extension == "json") json else yaml).readTree(content)

    /** Every `$ref` value anywhere in [document]. */
    fun references(document: JsonNode): List<String> = document.findValuesAsText("\$ref")

    /** `$ref` values that point outside [document]; a fully bundled document has none. */
    fun externalReferences(document: JsonNode): List<String> = references(document).filterNot { it.startsWith("#") }

    /** Every field name anywhere in [document]. */
    fun fieldNames(document: JsonNode): Set<String> = buildSet {
        fun visit(node: JsonNode) {
            node.properties().forEach { (name, child) ->
                add(name)
                visit(child)
            }
            if (node.isArray) node.forEach(::visit)
        }
        visit(document)
    }
}

/** Resources and classes on the consumer's test runtime classpath. */
object Classpath {
    private val loader: ClassLoader = Classpath::class.java.classLoader

    /** Content of a classpath resource; fails when it is missing. */
    fun resource(path: String): String =
        checkNotNull(loader.getResource(path)) { "Expected classpath resource $path" }.readText()

    fun hasResource(path: String): Boolean = loader.getResource(path) != null

    fun hasClass(name: String): Boolean = runCatching { Class.forName(name, false, loader) }.isSuccess
}
