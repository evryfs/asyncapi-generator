package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.assertMemberLocation
import dev.banking.asyncapi.generator.core.fixtures.assertNodeLocation
import dev.banking.asyncapi.generator.core.fixtures.childObject
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DocumentLocationTest {

    @Test
    fun `yaml reader retains source locations on nodes and object members`() {
        val document = YamlDocumentReader().read(ReaderFixtures.yamlSource("source-map.yaml"))

        assertDocumentLocations(document)
        assertValueFollowsKey(document.root, "asyncapi")
    }

    @Test
    fun `json reader retains source locations on nodes and object members`() {
        val document = JsonDocumentReader().read(ReaderFixtures.jsonSource("source-map.json"))

        assertDocumentLocations(document)
        assertValueFollowsKey(document.root, "asyncapi")
    }

    private fun assertDocumentLocations(document: InputDocument) {
        val info = document.root.childObject("info")
        val tags = assertIs<DocumentArray>(info["tags"])
        val lineOffset = if (document.source.format == DocumentFormat.JSON) 1 else 0

        document.assertNodeLocation(document.root, "root", 1)
        document.assertMemberLocation(document.root, "asyncapi", "root.asyncapi", 1 + lineOffset)
        document.assertMemberLocation(document.root, "info", "root.info", 2 + lineOffset)
        document.assertMemberLocation(info, "title", "root.info.title", 3 + lineOffset)
        document.assertMemberLocation(info, "tags", "root.info.tags", 4 + lineOffset)
        document.assertNodeLocation(tags[0], "root.info.tags[0]", 5 + lineOffset)
    }

    private fun assertValueFollowsKey(
        root: DocumentObject,
        memberName: String,
    ) {
        val member = requireNotNull(root.member(memberName))
        assertTrue(member.value.location.column > member.keyLocation.column)
    }
}
