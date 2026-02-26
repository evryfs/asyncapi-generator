package dev.banking.asyncapi.generator.maven.plugin

import org.apache.maven.plugin.Mojo
import java.io.File

object MavenTestHelper {

    fun outputPath(path: String): File =
        File(path).apply { mkdirs() }

    fun inputPath(path: String): File =
        File("src/test/resources/$path").also {
            require(it.exists()) { "Missing YAML test file: ${it.absolutePath}" }
        }

    fun Mojo.project(value: Any) {
        writeField("project", value)
    }

    fun Mojo.inputFile(value: Any) {
        writeField("inputFile", value)
    }

    fun Mojo.outputFile(value: Any?) {
        writeField("outputFile", value)
    }

    fun Mojo.codegenOutputDirectory(value: Any) {
        writeField("codegenOutputDirectory", value)
    }

    fun Mojo.resourceOutputDirectory(value: Any) {
        writeField("resourceOutputDirectory", value)
    }

    fun Mojo.modelPackage(value: Any) {
        writeField("modelPackage", value)
    }

    fun Mojo.clientPackage(value: Any?) {
        writeField("clientPackage", value)
    }

    fun Mojo.schemaPackage(value: Any?) {
        writeField("schemaPackage", value)
    }

    fun Mojo.generatorName(value: String) {
        writeField("generatorName", value)
    }

    fun Mojo.configOptions(value: Map<String, String>) {
        writeField("configOptions", value)
    }

    private fun Mojo.writeField(name: String, value: Any?) {
        val field = this.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }
}
