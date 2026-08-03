package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentReaderRegistryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `reads yaml files through the registry`() {
        val file = TestResources.file("reader/yaml/registry-asyncapi.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = assertIs<DocumentObject>(document.root)

        assertEquals(DocumentFormat.YAML, document.source.format)
        assertEquals("registry-asyncapi", document.source.id)
        assertEquals("3.0.0", assertIs<DocumentString>(root["asyncapi"]).value)
    }

    @Test
    fun `reads yml files through the registry`() {
        val file = TestResources.file("reader/yaml/registry-contract.yml")
        val document = DocumentReaderRegistry.read(file)
        assertEquals(DocumentFormat.YAML, document.source.format)
        assertEquals("registry-contract", document.source.id)
    }

    @Test
    fun `reads json files through the registry`() {
        val file = TestResources.file("reader/json/registry-asyncapi.json")
        val document = DocumentReaderRegistry.read(file)
        val root = assertIs<DocumentObject>(document.root)

        assertEquals(DocumentFormat.JSON, document.source.format)
        assertEquals("registry-asyncapi", document.source.id)
        assertEquals("3.0.0", assertIs<DocumentString>(root["asyncapi"]).value)
    }

    @Test
    fun `fails clearly for unsupported file extensions`() {
        val file = tempDir.resolve("contract.txt").toFile()
        file.writeText("asyncapi: '3.0.0'")

        val failure = assertFailsWith<DocumentReadException.UnsupportedFormat> {
            DocumentReaderRegistry.read(file)
        }
        assertEquals(file, failure.file)
        assertEquals("txt", failure.format)
    }

    @Test
    fun `normalizes missing file access failures`() {
        val file = tempDir.resolve("missing.yaml").toFile()

        val failure = assertFailsWith<DocumentReadException.UnreadableDocument> {
            DocumentReaderRegistry.read(file)
        }
        assertEquals(file, failure.file)
        assertNotNull(failure.cause)
        assertTrue(failure.message.orEmpty().endsWith(file.absolutePath))
    }

    @Test
    fun `rejects malformed UTF-8 as a document read failure`() {
        val file = tempDir.resolve("malformed-utf8.yaml").toFile()
        file.writeBytes(byteArrayOf(0xC3.toByte(), 0x28))

        val failure = assertFailsWith<DocumentReadException.MalformedDocument> {
            DocumentReaderRegistry.read(file)
        }
        assertEquals(file, failure.file)
        assertEquals(null, failure.location)
        assertNotNull(failure.cause)
        assertTrue(failure.message.orEmpty().endsWith(file.absolutePath))
    }
}
