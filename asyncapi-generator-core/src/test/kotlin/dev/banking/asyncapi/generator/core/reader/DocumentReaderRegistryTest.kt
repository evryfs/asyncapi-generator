package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.value
import dev.banking.asyncapi.generator.core.fixtures.writeTestFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DocumentReaderRegistryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `reads yaml files through the registry`() {
        val file = ReaderFixtures.yamlFile("registry-asyncapi.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = assertIs<DocumentObject>(document.root)
        assertEquals(DocumentFormat.YAML, document.source.format)
        assertEquals("registry-asyncapi", document.source.id)
        assertEquals("3.0.0", root.value("asyncapi"))
    }

    @Test
    fun `reads yml files through the registry`() {
        val file = ReaderFixtures.yamlFile("registry-contract.yml")
        val document = DocumentReaderRegistry.read(file)
        assertEquals(DocumentFormat.YAML, document.source.format)
        assertEquals("registry-contract", document.source.id)
    }

    @Test
    fun `reads json files through the registry`() {
        val file = ReaderFixtures.jsonFile("registry-asyncapi.json")
        val document = DocumentReaderRegistry.read(file)
        val root = assertIs<DocumentObject>(document.root)
        assertEquals(DocumentFormat.JSON, document.source.format)
        assertEquals("registry-asyncapi", document.source.id)
        assertEquals("3.0.0", root.value("asyncapi"))
    }

    @Test
    fun `fails clearly for unsupported file extensions`() {
        val file = tempDir.writeTestFile("contract.txt", "asyncapi: '3.0.0'")
        assertFailsWith<DocumentReadException.UnsupportedFormat> {
            DocumentReaderRegistry.read(file)
        }
    }

    @Test
    fun `normalizes missing file access failures`() {
        val file = tempDir.resolve("missing.yaml").toFile()

        val error = assertFailsWith<DocumentReadException.UnreadableDocument> {
            DocumentReaderRegistry.read(file)
        }

        assertEquals(file.absolutePath, error.message.orEmpty().substringAfterLast(": "))
    }

    @Test
    fun `rejects malformed UTF-8 as a document read failure`() {
        val file = tempDir.resolve("malformed-utf8.yaml").toFile()
        file.writeBytes(byteArrayOf(0xC3.toByte(), 0x28))

        val error = assertFailsWith<DocumentReadException.MalformedDocument> {
            DocumentReaderRegistry.read(file)
        }

        assertEquals(file.absolutePath, error.message.orEmpty().substringAfterLast(": "))
    }
}
