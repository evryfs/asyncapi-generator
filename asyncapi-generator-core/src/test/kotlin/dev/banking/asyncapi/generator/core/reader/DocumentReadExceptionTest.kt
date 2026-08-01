package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DocumentReadExceptionTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `empty document error includes the source file`() {
        val source = ReaderFixtures.yamlSource("empty.yaml")
        val error = assertFailsWith<DocumentReadException.EmptyDocument> {
            reader.read(source)
        }
        assertTrue(error.message.orEmpty().contains(source.file.absolutePath))
    }

    @Test
    fun `duplicate key error includes the key and source file`() {
        val source = ReaderFixtures.yamlSource("duplicate-key.yaml")
        val error = assertFailsWith<DocumentReadException.DuplicateKey> {
            reader.read(source)
        }
        assertTrue(error.message.orEmpty().contains("title"))
        assertTrue(error.message.orEmpty().contains(source.file.absolutePath))
    }

}
