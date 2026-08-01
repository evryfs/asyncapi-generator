package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.assertMemberLocation
import dev.banking.asyncapi.generator.core.fixtures.assertNodeLocation
import dev.banking.asyncapi.generator.core.fixtures.childObject
import dev.banking.asyncapi.generator.core.fixtures.semanticValue
import dev.banking.asyncapi.generator.core.fixtures.value
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonDocumentReaderTest {

    private val reader = JsonDocumentReader()

    @Test
    fun `reads semantic scalar values`() {
        val document = reader.read(ReaderFixtures.jsonSource("semantic-scalars.json"))
        val root = assertIs<DocumentObject>(document.root)
        val info = root.childObject("info")
        val example = root.childObject("components")
            .childObject("schemas")
            .childObject("Example")
        assertEquals("3.0.0", root.value("asyncapi"))
        assertEquals("Demo API", info.value("title"))
        assertEquals("folded text", info.value("summary"))
        assertEquals("literal\ntext", info.value("description"))
        assertEquals(true, example.value("enabled"))
        assertEquals("true", example.value("quotedEnabled"))
        assertEquals(12, example.value("count"))
        assertEquals("12", example.value("quotedCount"))
        assertEquals(12.5, example.value("price"))
        assertEquals(null, example.value("nullable"))
    }

    @Test
    fun `fails when json is malformed`() {
        val source = ReaderFixtures.jsonSource("malformed.json")
        assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }
    }

    @Test
    fun `reads an array root without applying AsyncAPI rules`() {
        val source = ReaderFixtures.jsonSource("invalid-root.json")
        val document = reader.read(source)

        val root = assertIs<DocumentArray>(document.root)
        assertEquals(listOf("asyncapi", "info"), root.elements.map { it.semanticValue() })
        assertEquals("root", root.location.path)
        assertEquals(1, root.location.line)
    }

    @Test
    fun `fails when document is empty`() {
        val source = ReaderFixtures.jsonSource("empty.json")
        assertFailsWith<DocumentReadException.EmptyDocument> {
            reader.read(source)
        }
    }

    @Test
    fun `fails when object contains duplicate keys`() {
        val source = ReaderFixtures.jsonSource("duplicate-key.json")
        val error = assertFailsWith<DocumentReadException.DuplicateKey> {
            reader.read(source)
        }
        assertTrue(error.message.orEmpty().contains("title"))
        assertTrue(error.message.orEmpty().contains(source.file.absolutePath))
    }

    @Test
    fun `records source locations for root object fields and array items`() {
        val document = reader.read(ReaderFixtures.jsonSource("source-map.json"))
        val root = assertIs<DocumentObject>(document.root)
        val info = root.childObject("info")
        val tags = info["tags"] as DocumentArray
        document.assertNodeLocation(root, "root", 1)
        document.assertMemberLocation(root, "asyncapi", "root.asyncapi", 2)
        document.assertMemberLocation(root, "info", "root.info", 3)
        document.assertMemberLocation(info, "title", "root.info.title", 4)
        document.assertMemberLocation(info, "tags", "root.info.tags", 5)
        document.assertNodeLocation(tags[0], "root.info.tags[0]", 6)
    }
}
