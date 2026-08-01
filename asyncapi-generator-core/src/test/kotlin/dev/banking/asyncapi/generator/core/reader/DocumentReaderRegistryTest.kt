package dev.banking.asyncapi.generator.core.reader

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DocumentReaderRegistryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `reads yaml files through the registry`() {
        val file = TestResources.file("reader/yaml/registry-asyncapi.yaml")
        val document = DocumentReaderRegistry.read(file)
        val root = assertThat(document.root).isInstanceOf<DocumentObject>()

        assertThat(document.source.format).isEqualTo(DocumentFormat.YAML)
        assertThat(document.source.id).isEqualTo("registry-asyncapi")
        root.prop("asyncapi") { it["asyncapi"] }
            .isNotNull()
            .isInstanceOf<DocumentString>()
            .prop(DocumentString::value)
            .isEqualTo("3.0.0")
    }

    @Test
    fun `reads yml files through the registry`() {
        val file = TestResources.file("reader/yaml/registry-contract.yml")
        val document = DocumentReaderRegistry.read(file)
        assertThat(document.source.format).isEqualTo(DocumentFormat.YAML)
        assertThat(document.source.id).isEqualTo("registry-contract")
    }

    @Test
    fun `reads json files through the registry`() {
        val file = TestResources.file("reader/json/registry-asyncapi.json")
        val document = DocumentReaderRegistry.read(file)
        val root = assertThat(document.root).isInstanceOf<DocumentObject>()

        assertThat(document.source.format).isEqualTo(DocumentFormat.JSON)
        assertThat(document.source.id).isEqualTo("registry-asyncapi")
        root.prop("asyncapi") { it["asyncapi"] }
            .isNotNull()
            .isInstanceOf<DocumentString>()
            .prop(DocumentString::value)
            .isEqualTo("3.0.0")
    }

    @Test
    fun `fails clearly for unsupported file extensions`() {
        val file = tempDir.resolve("contract.txt").toFile()
        file.writeText("asyncapi: '3.0.0'")

        assertFailure {
            DocumentReaderRegistry.read(file)
        }.isInstanceOf<DocumentReadException.UnsupportedFormat>()
    }

    @Test
    fun `normalizes missing file access failures`() {
        val file = tempDir.resolve("missing.yaml").toFile()

        assertFailure {
            DocumentReaderRegistry.read(file)
        }.isInstanceOf<DocumentReadException.UnreadableDocument>()
            .prop(Throwable::message)
            .isNotNull()
            .endsWith(file.absolutePath)
    }

    @Test
    fun `rejects malformed UTF-8 as a document read failure`() {
        val file = tempDir.resolve("malformed-utf8.yaml").toFile()
        file.writeBytes(byteArrayOf(0xC3.toByte(), 0x28))

        assertFailure {
            DocumentReaderRegistry.read(file)
        }.isInstanceOf<DocumentReadException.MalformedDocument>()
            .prop(Throwable::message)
            .isNotNull()
            .endsWith(file.absolutePath)
    }
}
