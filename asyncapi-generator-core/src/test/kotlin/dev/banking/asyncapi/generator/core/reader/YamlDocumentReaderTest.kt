package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentNumber
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YamlDocumentReaderTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `reads semantic scalar values without yaml style markers`() {
        val file = TestResources.file("reader/yaml/semantic-scalars.yaml")
        val source = DocumentSource(
            id = "semantic-scalars",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
        val root = assertIs<DocumentObject>(document.root)
        val info = assertIs<DocumentObject>(root["info"])
        val components = assertIs<DocumentObject>(root["components"])
        val schemas = assertIs<DocumentObject>(components["schemas"])
        val example = assertIs<DocumentObject>(schemas["Example"])

        assertEquals("3.0.0", assertIs<DocumentString>(root["asyncapi"]).value)
        assertEquals("Demo API", assertIs<DocumentString>(info["title"]).value)
        assertTrue(assertIs<DocumentString>(info["summary"]).value.startsWith("folded text"))
        assertTrue(assertIs<DocumentString>(info["description"]).value.startsWith("literal\ntext"))
        assertEquals(true, assertIs<DocumentBoolean>(example["enabled"]).value)
        assertEquals("true", assertIs<DocumentString>(example["quotedEnabled"]).value)
        assertEquals(12, assertIs<DocumentNumber>(example["count"]).value)
        assertEquals("12", assertIs<DocumentString>(example["quotedCount"]).value)
        assertEquals(12.5, assertIs<DocumentNumber>(example["price"]).value)
        assertIs<DocumentNull>(example["nullable"])
    }

    @Test
    fun `fails when yaml is malformed`() {
        val file = TestResources.file("reader/yaml/malformed.yaml")
        val source = DocumentSource(
            id = "malformed",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }
    }

    @Test
    fun `normalizes invalid yaml characters as malformed input`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content = "value: \u0001",
            format = DocumentFormat.YAML,
        )

        assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }
    }

    @Test
    fun `reads an array root without applying AsyncAPI rules`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)

        val root = assertIs<DocumentArray>(document.root)
        assertEquals("asyncapi", assertIs<DocumentString>(root[0]).value)
        assertEquals("info", assertIs<DocumentString>(root[1]).value)
        assertEquals("root", root.location.path)
        assertEquals(1, root.location.line)
    }

    @Test
    fun `fails when mapping key is not scalar`() {
        val file = TestResources.file("reader/yaml/non-scalar-key.yaml")
        val source = DocumentSource(
            id = "non-scalar-key",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailsWith<DocumentReadException.InvalidMappingKey> {
            reader.read(source)
        }
        assertTrue(failure.message.orEmpty().contains("expected string key"))
    }

    @Test
    fun `fails when mapping key is a non-string scalar`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        listOf("true", "42", "null").forEach { key ->
            val source = DocumentSource(
                id = "invalid-root",
                file = file,
                content = "  $key: value",
                format = DocumentFormat.YAML,
            )

            val failure = assertFailsWith<DocumentReadException.InvalidMappingKey> {
                reader.read(source)
            }

            assertTrue(failure.message.orEmpty().contains(source.file.absolutePath))
            assertTrue(failure.message.orEmpty().contains("line 1, column 3"))
            assertTrue(failure.message.orEmpty().contains("expected string key"))
        }
    }

    @Test
    fun `accepts quoted keys that resemble non-string scalars`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                "true": boolean-shaped
                "42": number-shaped
                "null": null-shaped
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        val root = assertIs<DocumentObject>(reader.read(source).root)
        assertEquals("boolean-shaped", assertIs<DocumentString>(root["true"]).value)
        assertEquals("number-shaped", assertIs<DocumentString>(root["42"]).value)
        assertEquals("null-shaped", assertIs<DocumentString>(root["null"]).value)
    }

    @Test
    fun `fails instead of exposing a yaml merge key as a literal member`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                base: &base
                  enabled: true
                merged:
                  <<: *base
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailsWith<DocumentReadException.InvalidMappingKey> {
            reader.read(source)
        }

        assertTrue(failure.message.orEmpty().contains("line 4, column 3"))
        assertTrue(failure.message.orEmpty().contains("expected string key"))
    }

    @Test
    fun `normalizes nesting limit failures`() {
        val constrainedReader =
            YamlDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxNestingDepth = 2),
            )
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                root:
                  child:
                    leaf: true
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            constrainedReader.read(source)
        }
        assertTrue(failure.message.orEmpty().contains(source.file.absolutePath))
    }

    @Test
    fun `limits expanded collection aliases`() {
        val constrainedReader =
            YamlDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxAliasesForCollections = 1),
            )
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                base: &base
                  value: true
                first: *base
                second: *base
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            constrainedReader.read(source)
        }
    }

    @Test
    fun `returns linked map preserving input order`() {
        val file = TestResources.file("reader/yaml/order-preservation.yaml")
        val source = DocumentSource(
            id = "order-preservation",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val root = assertIs<DocumentObject>(reader.read(source).root)

        assertEquals(listOf("asyncapi", "info", "channels"), root.members.keys.toList())
    }
}
