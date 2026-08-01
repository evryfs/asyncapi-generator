package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import java.io.File

/**
 * Fixture access for document-reader tests.
 *
 * Reader tests use these helpers to load YAML and JSON documents as either
 * [DocumentSource] instances or real files while keeping resource layout details
 * in one place.
 */
internal object ReaderFixtures {

    fun yamlSource(name: String): DocumentSource {
        val file = yamlFile(name)
        return DocumentSource(
            id = file.nameWithoutExtension,
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
    }

    fun yamlFile(name: String): File =
        TestResources.file("reader/yaml/$name")

    fun jsonSource(name: String): DocumentSource {
        val file = jsonFile(name)
        return DocumentSource(
            id = file.nameWithoutExtension,
            file = file,
            content = file.readText(),
            format = DocumentFormat.JSON,
        )
    }

    fun jsonFile(name: String): File =
        TestResources.file("reader/json/$name")
}
