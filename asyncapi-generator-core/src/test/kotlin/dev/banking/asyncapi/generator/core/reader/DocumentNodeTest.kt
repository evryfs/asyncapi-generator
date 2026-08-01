package dev.banking.asyncapi.generator.core.reader

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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

        assertEquals(listOf("first", "second"), node.members.keys.toList())
        assertEquals(1, node.member("first")?.keyLocation?.column)
        assertSame(firstValue.location, node["first"]?.location)
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

        assertTrue("description" in node.members)
        assertIs<DocumentNull>(node["description"])
        assertNull(node["missing"])
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

        assertEquals("example", assertIs<DocumentString>(node["name"]).value)
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (node.members as MutableMap<String, DocumentMember>).clear()
        }
    }

    @Test
    fun `array defensively copies and exposes an unmodifiable element list`() {
        val source = mutableListOf<DocumentNode>(
            DocumentBoolean(true, location("root[0]", line = 1, column = 2)),
        )
        val node = DocumentArray(source, location("root", line = 1, column = 1))

        source.clear()

        assertEquals(true, assertIs<DocumentBoolean>(node[0]).value)
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (node.elements as MutableList<DocumentNode>).clear()
        }
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
