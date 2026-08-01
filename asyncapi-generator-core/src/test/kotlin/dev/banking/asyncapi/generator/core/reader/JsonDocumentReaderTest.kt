package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentNumber
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonDocumentReaderTest {

    private val reader = JsonDocumentReader()

    @Test
    fun `reads semantic scalar values`() {
        val document = reader.read(ReaderFixtures.jsonSource("semantic-scalars.json"))
        val root = assertIs<DocumentObject>(document.root)
        val info = assertIs<DocumentObject>(root["info"])
        val components = assertIs<DocumentObject>(root["components"])
        val schemas = assertIs<DocumentObject>(components["schemas"])
        val example = assertIs<DocumentObject>(schemas["Example"])

        assertEquals("3.0.0", assertIs<DocumentString>(root["asyncapi"]).value)
        assertEquals("Demo API", assertIs<DocumentString>(info["title"]).value)
        assertEquals("folded text", assertIs<DocumentString>(info["summary"]).value)
        assertEquals("literal\ntext", assertIs<DocumentString>(info["description"]).value)
        assertEquals(true, assertIs<DocumentBoolean>(example["enabled"]).value)
        assertEquals("true", assertIs<DocumentString>(example["quotedEnabled"]).value)
        assertEquals(12, assertIs<DocumentNumber>(example["count"]).value)
        assertEquals("12", assertIs<DocumentString>(example["quotedCount"]).value)
        assertEquals(12.5, assertIs<DocumentNumber>(example["price"]).value)
        assertIs<DocumentNull>(example["nullable"])
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
        assertEquals("asyncapi", assertIs<DocumentString>(root[0]).value)
        assertEquals("info", assertIs<DocumentString>(root[1]).value)
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
    fun `normalizes nesting limit failures`() {
        val constrainedReader =
            JsonDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxNestingDepth = 2),
            )
        val source =
            ReaderFixtures.jsonSource("invalid-root.json").copy(
                content = """{"root":{"child":{"leaf":true}}}""",
            )

        val error = assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            constrainedReader.read(source)
        }

        assertTrue(error.message.orEmpty().contains(source.file.absolutePath))
    }

    @Test
    fun `records source locations for root object fields and array items`() {
        val source = ReaderFixtures.jsonSource("source-map.json")
        val document = reader.read(source)
        val root = assertIs<DocumentObject>(document.root)
        val info = assertIs<DocumentObject>(root["info"])
        val tags = assertIs<DocumentArray>(info["tags"])

        assertEquals(source.id, root.location.sourceId)
        assertEquals(source.file, root.location.file)
        assertEquals("root", root.location.path)
        assertEquals(1, root.location.line)
        assertTrue(root.location.column >= 1)

        val asyncapiMember = assertNotNull(root.member("asyncapi"))
        assertEquals(source.id, asyncapiMember.keyLocation.sourceId)
        assertEquals(source.file, asyncapiMember.keyLocation.file)
        assertEquals("root.asyncapi", asyncapiMember.keyLocation.path)
        assertEquals(2, asyncapiMember.keyLocation.line)
        assertTrue(asyncapiMember.keyLocation.column >= 1)

        val infoMember = assertNotNull(root.member("info"))
        assertEquals(source.id, infoMember.keyLocation.sourceId)
        assertEquals(source.file, infoMember.keyLocation.file)
        assertEquals("root.info", infoMember.keyLocation.path)
        assertEquals(3, infoMember.keyLocation.line)
        assertTrue(infoMember.keyLocation.column >= 1)

        val titleMember = assertNotNull(info.member("title"))
        assertEquals(source.id, titleMember.keyLocation.sourceId)
        assertEquals(source.file, titleMember.keyLocation.file)
        assertEquals("root.info.title", titleMember.keyLocation.path)
        assertEquals(4, titleMember.keyLocation.line)
        assertTrue(titleMember.keyLocation.column >= 1)

        val tagsMember = assertNotNull(info.member("tags"))
        assertEquals(source.id, tagsMember.keyLocation.sourceId)
        assertEquals(source.file, tagsMember.keyLocation.file)
        assertEquals("root.info.tags", tagsMember.keyLocation.path)
        assertEquals(5, tagsMember.keyLocation.line)
        assertTrue(tagsMember.keyLocation.column >= 1)

        val firstTag = tags[0]
        assertEquals(source.id, firstTag.location.sourceId)
        assertEquals(source.file, firstTag.location.file)
        assertEquals("root.info.tags[0]", firstTag.location.path)
        assertEquals(6, firstTag.location.line)
        assertTrue(firstTag.location.column >= 1)
    }
}
