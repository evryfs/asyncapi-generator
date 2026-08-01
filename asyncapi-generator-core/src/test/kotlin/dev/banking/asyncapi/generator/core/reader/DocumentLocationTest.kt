package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.assertMemberLocation
import dev.banking.asyncapi.generator.core.fixtures.assertNodeLocation
import dev.banking.asyncapi.generator.core.fixtures.childObject
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class DocumentLocationTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `records source locations for root object fields and array items`() {
        val source = ReaderFixtures.yamlSource("source-map.yaml")
        val document = reader.read(source)
        val root = assertIs<DocumentObject>(document.root)
        val info = root.childObject("info")
        val tags = info["tags"] as DocumentArray
        document.assertNodeLocation(root, "root", 1)
        document.assertMemberLocation(root, "asyncapi", "root.asyncapi", 1)
        document.assertMemberLocation(root, "info", "root.info", 2)
        document.assertMemberLocation(info, "title", "root.info.title", 3)
        document.assertMemberLocation(info, "tags", "root.info.tags", 4)
        document.assertNodeLocation(tags[0], "root.info.tags[0]", 5)
    }
}
