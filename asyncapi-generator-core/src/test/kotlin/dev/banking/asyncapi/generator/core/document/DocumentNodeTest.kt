package dev.banking.asyncapi.generator.core.document

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.junit.jupiter.api.Test
import java.io.File

class DocumentNodeTest {

    @Test
    fun `object preserves member order and key and value locations`() {
        val firstValue = DocumentString("first", location("root.first", line = 1, column = 8))
        val secondValue = DocumentString("second", location("root.second", line = 2, column = 9))
        val node = DocumentObject(
            members = linkedMapOf(
                "first" to DocumentMember(location("root.first", line = 1, column = 1), firstValue),
                "second" to DocumentMember(location("root.second", line = 2, column = 1), secondValue),
            ),
            location = location("root", line = 1, column = 1),
        )

        assertThat(node.members.keys.toList()).isEqualTo(listOf("first", "second"))
        assertThat(node.member("first")?.keyLocation?.column).isEqualTo(1)
        assertThat(node["first"]?.location)
            .isNotNull()
            .isSameInstanceAs(firstValue.location)
    }

    @Test
    fun `object distinguishes an absent member from an explicit null`() {
        val node = DocumentObject(
            members = mapOf(
                "description" to DocumentMember(
                    keyLocation = location("root.description", line = 1, column = 1),
                    value = DocumentNull(location("root.description", line = 1, column = 14)),
                ),
            ),
            location = location("root", line = 1, column = 1),
        )

        assertThat(node.members.keys).contains("description")
        assertThat(node["description"])
            .isNotNull()
            .isInstanceOf<DocumentNull>()
        assertThat(node["missing"]).isNull()
    }

    @Test
    fun `object defensively copies and exposes an unmodifiable member map`() {
        val source = linkedMapOf(
            "name" to DocumentMember(
                keyLocation = location("root.name", line = 1, column = 1),
                value = DocumentString("example", location("root.name", line = 1, column = 7)),
            ),
        )
        val node = DocumentObject(source, location("root", line = 1, column = 1))

        source.clear()

        assertThat(node["name"])
            .isNotNull()
            .isInstanceOf<DocumentString>()
            .prop(DocumentString::value)
            .isEqualTo("example")
        assertFailure {
            @Suppress("UNCHECKED_CAST")
            (node.members as MutableMap<String, DocumentMember>).clear()
        }.isInstanceOf<UnsupportedOperationException>()
    }

    @Test
    fun `array defensively copies and exposes an unmodifiable element list`() {
        val source = mutableListOf<DocumentNode>(
            DocumentBoolean(true, location("root[0]", line = 1, column = 2)),
        )
        val node = DocumentArray(source, location("root", line = 1, column = 1))

        source.clear()

        assertThat(node[0])
            .isInstanceOf<DocumentBoolean>()
            .prop(DocumentBoolean::value)
            .isTrue()
        assertFailure {
            @Suppress("UNCHECKED_CAST")
            (node.elements as MutableList<DocumentNode>).clear()
        }.isInstanceOf<UnsupportedOperationException>()
    }

    private fun location(
        path: String,
        line: Int,
        column: Int,
    ): SourceLocation =
        SourceLocation(
            sourceId = "fixture",
            file = File("fixture.yaml"),
            path = path,
            line = line,
            column = column,
        )
}
