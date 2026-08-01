package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.value
import dev.banking.asyncapi.generator.core.fixtures.writeTestFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentReaderRegistryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `reads yaml files through the registry`() {
        val file = ReaderFixtures.yamlFile("registry-asyncapi.yaml")
        val document = DocumentReaderRegistry.read(file)
        assertEquals(DocumentFormat.YAML, document.source.format)
        assertEquals("registry-asyncapi", document.source.id)
        assertEquals("3.0.0", document.root.value("asyncapi"))
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
        assertEquals(DocumentFormat.JSON, document.source.format)
        assertEquals("registry-asyncapi", document.source.id)
        assertEquals("3.0.0", document.root.value("asyncapi"))
    }

    @Test
    fun `fails clearly for unsupported file extensions`() {
        val file = tempDir.writeTestFile("contract.txt", "asyncapi: '3.0.0'")
        assertFailsWith<DocumentReadException.UnsupportedFormat> {
            DocumentReaderRegistry.read(file)
        }
    }
}
