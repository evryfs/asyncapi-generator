package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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

        val failure = assertFailsWith<DocumentReadException.EmptyDocument> {
            reader.read(source)
        }
        assertTrue(failure.message.orEmpty().contains(source.file.absolutePath))
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

        val failure = assertFailsWith<DocumentReadException.DuplicateKey> {
            reader.read(source)
        }

        assertTrue(failure.message.orEmpty().contains("title"))
        assertTrue(failure.message.orEmpty().contains(source.file.absolutePath))
    }
}
