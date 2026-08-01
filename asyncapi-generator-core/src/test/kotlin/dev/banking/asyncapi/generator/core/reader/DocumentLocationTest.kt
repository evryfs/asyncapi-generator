package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentLocationTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `records source locations for root object fields and array items`() {
        val source = ReaderFixtures.yamlSource("source-map.yaml")
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
        assertEquals(1, asyncapiMember.keyLocation.line)
        assertTrue(asyncapiMember.keyLocation.column >= 1)

        val infoMember = assertNotNull(root.member("info"))
        assertEquals(source.id, infoMember.keyLocation.sourceId)
        assertEquals(source.file, infoMember.keyLocation.file)
        assertEquals("root.info", infoMember.keyLocation.path)
        assertEquals(2, infoMember.keyLocation.line)
        assertTrue(infoMember.keyLocation.column >= 1)

        val titleMember = assertNotNull(info.member("title"))
        assertEquals(source.id, titleMember.keyLocation.sourceId)
        assertEquals(source.file, titleMember.keyLocation.file)
        assertEquals("root.info.title", titleMember.keyLocation.path)
        assertEquals(3, titleMember.keyLocation.line)
        assertTrue(titleMember.keyLocation.column >= 1)

        val tagsMember = assertNotNull(info.member("tags"))
        assertEquals(source.id, tagsMember.keyLocation.sourceId)
        assertEquals(source.file, tagsMember.keyLocation.file)
        assertEquals("root.info.tags", tagsMember.keyLocation.path)
        assertEquals(4, tagsMember.keyLocation.line)
        assertTrue(tagsMember.keyLocation.column >= 1)

        val firstTag = tags[0]
        assertEquals(source.id, firstTag.location.sourceId)
        assertEquals(source.file, firstTag.location.file)
        assertEquals("root.info.tags[0]", firstTag.location.path)
        assertEquals(5, firstTag.location.line)
        assertTrue(firstTag.location.column >= 1)
    }
}
