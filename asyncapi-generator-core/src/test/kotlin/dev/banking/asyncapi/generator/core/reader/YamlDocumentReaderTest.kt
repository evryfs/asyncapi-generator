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

        val failure = assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }
        assertEquals(source.file, failure.file)
        assertTrue(failure.location?.line ?: 0 > 0)
        assertTrue(failure.location?.column ?: 0 > 0)
        assertTrue(failure.cause is org.yaml.snakeyaml.error.MarkedYAMLException)
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

        val failure = assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }
        assertEquals(source.file, failure.file)
        assertEquals(1, failure.location?.line)
        assertTrue(failure.location?.column ?: 0 > 0)
        assertTrue(failure.cause is org.yaml.snakeyaml.reader.ReaderException)
    }

    @Test
    fun `rejects additional yaml documents`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "multiple-documents",
            file = file,
            content = "value: first\n---\nvalue: second\n",
            format = DocumentFormat.YAML,
        )

        val failure = assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }

        assertEquals(2, failure.location?.line)
        assertTrue(failure.cause is org.yaml.snakeyaml.composer.ComposerException)
    }

    @Test
    fun `rejects custom tags on every yaml node shape`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val taggedDocuments =
            listOf(
                "value: !custom content",
                "value: !custom [one, two]",
                "value: !custom {nested: content}",
            )

        taggedDocuments.forEach { content ->
            val source = DocumentSource(
                id = "custom-tag",
                file = file,
                content = content,
                format = DocumentFormat.YAML,
            )

            val failure = assertFailsWith<DocumentReadException.MalformedDocument> {
                reader.read(source)
            }

            assertEquals(source.file, failure.file)
            assertEquals(1, failure.location?.line)
            assertTrue(failure.cause?.message.orEmpty().contains("Unsupported YAML"))
        }
    }

    @Test
    fun `preserves yaml words and dates as strings`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "yaml-words",
            file = file,
            content =
                """
                yesValue: yes
                noValue: no
                onValue: on
                offValue: off
                dateValue: 2026-08-03
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        val root = assertIs<DocumentObject>(reader.read(source).root)

        assertEquals("yes", assertIs<DocumentString>(root["yesValue"]).value)
        assertEquals("no", assertIs<DocumentString>(root["noValue"]).value)
        assertEquals("on", assertIs<DocumentString>(root["onValue"]).value)
        assertEquals("off", assertIs<DocumentString>(root["offValue"]).value)
        assertEquals("2026-08-03", assertIs<DocumentString>(root["dateValue"]).value)
    }

    @Test
    fun `rejects yaml-only and non-finite numeric values`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val yamlOnlyNumbers =
            listOf("+1", "01", "0x10", "0o10", ".5", "1.", "1_000", ".inf", "-.Inf", ".nan")

        yamlOnlyNumbers.forEach { value ->
            val source = DocumentSource(
                id = "yaml-number",
                file = file,
                content = "value: $value",
                format = DocumentFormat.YAML,
            )

            val failure = assertFailsWith<DocumentReadException.MalformedDocument> {
                reader.read(source)
            }

            assertEquals("root.value", failure.location?.path)
            assertTrue(failure.cause?.message.orEmpty().contains("JSON-compatible number"))
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
        assertEquals(source.file, failure.file)
        assertEquals("root", failure.location?.path)
        assertEquals(1, failure.location?.line)
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

            assertEquals(source.file, failure.file)
            assertEquals("root.$key", failure.location?.path)
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
        assertEquals(DocumentResourceLimit.NESTING_DEPTH, failure.limit)
        assertEquals(2L, failure.maximum)
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

        val failure = assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            constrainedReader.read(source)
        }
        assertEquals(DocumentResourceLimit.COLLECTION_ALIASES, failure.limit)
        assertEquals(1L, failure.maximum)
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
