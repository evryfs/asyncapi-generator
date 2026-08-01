package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.fixtures.ParserNodeFixtures
import dev.banking.asyncapi.generator.core.fixtures.assertMessageContains
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.reader.DocumentNull
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ParserNodeTest {

    @Test
    fun `required reports a structured source-located diagnostic for an absent member`() {
        val node = ParserNodeFixtures.node(
            value = mapOf("present" to true),
            sourceLine = "present: true",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.required("missing")
        }

        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("missing", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertNull(diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("test.root.missing", diagnostic.path)
        assertEquals("asyncapi.yaml", diagnostic.sourceLocation.file.name)
        assertEquals(1, diagnostic.sourceLocation.line)
        assertEquals(1, diagnostic.sourceLocation.column)
        error.assertMessageContains("Missing required member 'missing'")
        error.assertMessageContains("asyncapi.yaml (test.root.missing)")
    }

    @Test
    fun `optional distinguishes an absent member from an explicit null`() {
        val node = ParserNodeFixtures.node(
            value = mapOf("explicit" to null),
            sourceLine = "explicit: null",
        )

        assertNull(node.optional("absent"))
        val explicitNull = assertIs<ParserNode>(node.optional("explicit"))
        assertIs<DocumentNull>(explicitNull.node)
        assertNull(explicitNull.expect<String?>())
    }

    @Test
    fun `expect rejects explicit null for a non-null type at the value path`() {
        val node = ParserNodeFixtures.node(
            value = mapOf("explicit" to null),
            sourceLine = "explicit: null",
        ).required("explicit")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<String>()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("test.root.explicit", diagnostic.path)
        assertEquals("asyncapi.yaml", diagnostic.sourceLocation.file.name)
        assertEquals(1, diagnostic.sourceLocation.line)
        assertEquals(1, diagnostic.sourceLocation.column)
    }

    @Test
    fun `members and elements retain parser paths`() {
        val objectNode = ParserNodeFixtures.node(
            value = linkedMapOf("first" to true, "second" to false),
            sourceLine = "first: true",
        )
        val arrayNode = ParserNodeFixtures.node(
            value = listOf("first", "second"),
            sourceLine = "- first",
        )

        assertEquals(
            listOf("test.root.first", "test.root.second"),
            objectNode.members().map(ParserNode::path),
        )
        assertEquals(
            listOf("test.root[0]", "test.root[1]"),
            arrayNode.elements().map(ParserNode::path),
        )
    }

    @Test
    fun `members rejects an array with a structured type diagnostic`() {
        val node = ParserNodeFixtures.node(
            value = listOf("value"),
            sourceLine = "- value",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.members()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals("test.root", diagnostic.path)
    }

    @Test
    fun `elements rejects an object with a structured type diagnostic`() {
        val node = ParserNodeFixtures.node(
            value = mapOf("value" to true),
            sourceLine = "value: true",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.elements()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals("List<Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.OBJECT, diagnostic.actualType)
        assertEquals("test.root", diagnostic.path)
    }

    @Test
    fun `expect recursively checks nested generic values`() {
        val node = ParserNodeFixtures.node(
            value = listOf(
                mapOf("label" to "first"),
                mapOf("label" to 7),
            ),
            sourceLine = "- label: first",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<List<Map<String, String>>>()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(7, diagnostic.actualValue)
        assertEquals("test.root[1].label", diagnostic.path)
    }

    @Test
    fun `expect permits nullable nested values`() {
        val node = ParserNodeFixtures.node(
            value = listOf(mapOf("label" to null)),
            sourceLine = "- label: null",
        )

        val value = node.expect<List<Map<String, String?>>>()

        assertEquals(listOf(mapOf("label" to null)), value)
    }

    @Test
    fun `expect any preserves free-form JSON-compatible values`() {
        val expected = mapOf(
            "array" to listOf(1, true, null, mapOf("nested" to "value")),
        )
        val node = ParserNodeFixtures.node(
            value = expected,
            sourceLine = "array: [1, true, null]",
        )

        assertEquals(expected, node.expect<Any?>())
    }

    @Test
    fun `to plain value preserves an explicit null`() {
        val node = ParserNodeFixtures.scalar(
            value = null,
            sourceLine = "value: null",
        )

        assertNull(node.toPlainValue())
    }

    @Test
    fun `expect retains scalar guidance in its formatted diagnostic`() {
        val node = ParserNodeFixtures.scalar(
            value = "true",
            sourceLine = "deprecated: \"true\"",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<Boolean>()
        }

        error.assertMessageContains("expected Boolean")
        error.assertMessageContains("found String \"true\"")
        error.assertMessageContains("quoted booleans are strings in YAML")
        error.assertMessageContains("deprecated: \"true\"")
    }

    @Test
    fun `coerce reports quoted boolean as yaml string when boolean is expected`() {
        val node = ParserNodeFixtures.scalar(
            value = "true",
            sourceLine = "deprecated: \"true\"",
        )
        val error = assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            node.coerce<Boolean>()
        }
        error.assertMessageContains("expected Boolean")
        error.assertMessageContains("found String \"true\"")
        error.assertMessageContains("quoted booleans are strings in YAML")
        error.assertMessageContains("deprecated: \"true\"")
    }

    @Test
    fun `coerce reports quoted number as yaml string when number is expected`() {
        val node = ParserNodeFixtures.scalar(
            value = "12",
            sourceLine = "minLength: \"12\"",
        )
        val error = assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            node.coerce<Number>()
        }
        error.assertMessageContains("expected Number")
        error.assertMessageContains("found String \"12\"")
        error.assertMessageContains("quoted numbers are strings in YAML")
        error.assertMessageContains("minLength: \"12\"")
    }

    @Test
    fun `coerce reports unquoted boolean when string is expected`() {
        val node = ParserNodeFixtures.scalar(
            value = true,
            sourceLine = "version: true",
        )
        val error = assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            node.coerce<String>()
        }
        error.assertMessageContains("expected String")
        error.assertMessageContains("found Boolean true")
        error.assertMessageContains("quote the value")
        error.assertMessageContains("version: true")
    }
}
