package dev.banking.asyncapi.generator.core.reader

import assertk.assertFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test

class DocumentReadExceptionTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `empty document error includes the source file`() {
        val file = TestResources.file("reader/yaml/empty.yaml")
        val source = DocumentSource(
            id = "empty",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.EmptyDocument>()
            .messageContains(source.file.absolutePath)
    }

    @Test
    fun `duplicate key error includes the key and source file`() {
        val file = TestResources.file("reader/yaml/duplicate-key.yaml")
        val source = DocumentSource(
            id = "duplicate-key",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.DuplicateKey>()

        failure.messageContains("title")
        failure.messageContains(source.file.absolutePath)
    }
}
