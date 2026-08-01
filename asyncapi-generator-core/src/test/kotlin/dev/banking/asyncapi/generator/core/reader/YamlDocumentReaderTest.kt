package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.childObject
import dev.banking.asyncapi.generator.core.fixtures.semanticValue
import dev.banking.asyncapi.generator.core.fixtures.value
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YamlDocumentReaderTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `reads semantic scalar values without yaml style markers`() {
        val document = reader.read(ReaderFixtures.yamlSource("semantic-scalars.yaml"))
        val root = assertIs<DocumentObject>(document.root)
        val info = root.childObject("info")
        val example = root.childObject("components")
            .childObject("schemas")
            .childObject("Example")
        assertEquals("3.0.0", root.value("asyncapi"))
        assertEquals("Demo API", info.value("title"))
        assertTrue((info.value("summary") as String).startsWith("folded text"))
        assertTrue((info.value("description") as String).startsWith("literal\ntext"))
        assertEquals(true, example.value("enabled"))
        assertEquals("true", example.value("quotedEnabled"))
        assertEquals(12, example.value("count"))
        assertEquals("12", example.value("quotedCount"))
        assertEquals(12.5, example.value("price"))
        assertEquals(null, example.value("nullable"))
    }

    @Test
    fun `fails when yaml is malformed`() {
        val source = ReaderFixtures.yamlSource("malformed.yaml")
        assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }
    }

    @Test
    fun `normalizes invalid yaml characters as malformed input`() {
        val source =
            ReaderFixtures.yamlSource("invalid-root.yaml").copy(
                content = "value: \u0001",
            )

        assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }
    }

    @Test
    fun `reads an array root without applying AsyncAPI rules`() {
        val source = ReaderFixtures.yamlSource("invalid-root.yaml")
        val document = reader.read(source)

        val root = assertIs<DocumentArray>(document.root)
        assertEquals(listOf("asyncapi", "info"), root.elements.map { it.semanticValue() })
        assertEquals("root", root.location.path)
        assertEquals(1, root.location.line)
    }

    @Test
    fun `fails when mapping key is not scalar`() {
        val source = ReaderFixtures.yamlSource("non-scalar-key.yaml")
        val error = assertFailsWith<DocumentReadException.InvalidMappingKey> {
            reader.read(source)
        }

        assertTrue(error.message.orEmpty().contains("expected string key"))
    }

    @Test
    fun `fails when mapping key is a non-string scalar`() {
        listOf("true", "42", "null").forEach { key ->
            val source =
                ReaderFixtures.yamlSource("invalid-root.yaml").copy(
                    content = "  $key: value",
                )

            val error = assertFailsWith<DocumentReadException.InvalidMappingKey> {
                reader.read(source)
            }

            assertTrue(error.message.orEmpty().contains(source.file.absolutePath))
            assertTrue(error.message.orEmpty().contains("line 1, column 3"))
            assertTrue(error.message.orEmpty().contains("expected string key"))
        }
    }

    @Test
    fun `accepts quoted keys that resemble non-string scalars`() {
        val source =
            ReaderFixtures.yamlSource("invalid-root.yaml").copy(
                content =
                    """
                    "true": boolean-shaped
                    "42": number-shaped
                    "null": null-shaped
                    """.trimIndent(),
            )

        val root = assertIs<DocumentObject>(reader.read(source).root)

        assertEquals("boolean-shaped", root.value("true"))
        assertEquals("number-shaped", root.value("42"))
        assertEquals("null-shaped", root.value("null"))
    }

    @Test
    fun `fails instead of exposing a yaml merge key as a literal member`() {
        val source =
            ReaderFixtures.yamlSource("invalid-root.yaml").copy(
                content =
                    """
                    base: &base
                      enabled: true
                    merged:
                      <<: *base
                    """.trimIndent(),
            )

        val error = assertFailsWith<DocumentReadException.InvalidMappingKey> {
            reader.read(source)
        }

        assertTrue(error.message.orEmpty().contains("line 4, column 3"))
        assertTrue(error.message.orEmpty().contains("expected string key"))
    }

    @Test
    fun `normalizes nesting limit failures`() {
        val constrainedReader =
            YamlDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxNestingDepth = 2),
            )
        val source =
            ReaderFixtures.yamlSource("invalid-root.yaml").copy(
                content =
                    """
                    root:
                      child:
                        leaf: true
                    """.trimIndent(),
            )

        val error = assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            constrainedReader.read(source)
        }

        assertTrue(error.message.orEmpty().contains(source.file.absolutePath))
    }

    @Test
    fun `limits expanded collection aliases`() {
        val constrainedReader =
            YamlDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxAliasesForCollections = 1),
            )
        val source =
            ReaderFixtures.yamlSource("invalid-root.yaml").copy(
                content =
                    """
                    base: &base
                      value: true
                    first: *base
                    second: *base
                    """.trimIndent(),
            )

        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            constrainedReader.read(source)
        }
    }

    @Test
    fun `returns linked map preserving input order`() {
        val document = reader.read(ReaderFixtures.yamlSource("order-preservation.yaml"))
        val root = assertIs<DocumentObject>(document.root)
        assertEquals(listOf("asyncapi", "info", "channels"), root.members.keys.toList())
    }
}
