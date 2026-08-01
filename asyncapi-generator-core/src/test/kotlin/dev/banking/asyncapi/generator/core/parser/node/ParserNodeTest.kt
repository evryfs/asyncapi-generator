package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.fixtures.ParserNodeFixtures
import dev.banking.asyncapi.generator.core.fixtures.assertMessageContains
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParserNodeTest {

    @Test
    fun `expect reports quoted boolean as yaml string when boolean is expected`() {
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
    fun `expect reports quoted number as yaml string when number is expected`() {
        val node = ParserNodeFixtures.scalar(
            value = "12",
            sourceLine = "minLength: \"12\"",
        )
        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<Number>()
        }
        error.assertMessageContains("expected Number")
        error.assertMessageContains("found String \"12\"")
        error.assertMessageContains("quoted numbers are strings in YAML")
        error.assertMessageContains("minLength: \"12\"")
    }

    @Test
    fun `expect reports unquoted boolean when string is expected`() {
        val node = ParserNodeFixtures.scalar(
            value = true,
            sourceLine = "version: true",
        )
        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<String>()
        }
        error.assertMessageContains("expected String")
        error.assertMessageContains("found Boolean true")
        error.assertMessageContains("quote the value")
        error.assertMessageContains("version: true")
    }

    @Test
    fun `expect accepts explicit null only for nullable types`() {
        val node = ParserNodeFixtures.scalar(
            value = null,
            sourceLine = "description: null",
        )

        assertNull(node.expect<String?>())

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<String>()
        }
        val diagnostic = error.diagnostic as ParserDiagnostic.UnexpectedValueType
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals(typeOf<String>(), diagnostic.expectedType)
        assertNull(diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("test.root.value", diagnostic.path)
        assertEquals("test.root.value", diagnostic.sourceLocation.path)
    }

    @Test
    fun `expect validates nested list element types at their exact path`() {
        val node = ParserNodeFixtures.value(
            value = listOf("first", 2),
            sourceLine = "values: [first, 2]",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<List<String>>()
        }
        val diagnostic = error.diagnostic as ParserDiagnostic.UnexpectedValueType
        assertEquals(typeOf<String>(), diagnostic.expectedType)
        assertEquals(Int::class, diagnostic.actualType)
        assertEquals(2, diagnostic.actualValue)
        assertEquals("test.root.value[1]", diagnostic.path)
        assertEquals("test.root.value[1]", diagnostic.sourceLocation.path)
    }

    @Test
    fun `expect validates nested map value types at their exact path`() {
        val node = ParserNodeFixtures.value(
            value = linkedMapOf("name" to "example", "enabled" to true),
            sourceLine = "value: {name: example, enabled: true}",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<Map<String, String>>()
        }
        val diagnostic = error.diagnostic as ParserDiagnostic.UnexpectedValueType
        assertEquals(typeOf<String>(), diagnostic.expectedType)
        assertEquals(Boolean::class, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals("test.root.value.enabled", diagnostic.path)
    }

    @Test
    fun `expect recursively validates combined collection types at their exact path`() {
        val node = ParserNodeFixtures.value(
            value = listOf(
                linkedMapOf("name" to "first"),
                linkedMapOf("name" to true),
            ),
            sourceLine = "values: [{name: first}, {name: true}]",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<List<Map<String, String>>>()
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(typeOf<String>(), diagnostic.expectedType)
        assertEquals(Boolean::class, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals("test.root.value[1].name", diagnostic.path)
    }

    @Test
    fun `expect supports nullable nested values`() {
        val node = ParserNodeFixtures.value(
            value = listOf("first", null),
            sourceLine = "values: [first, null]",
        )

        assertEquals(listOf("first", null), node.expect<List<String?>>())
    }

    @Test
    fun `expect supports nullable outer collection values`() {
        val nullNode = ParserNodeFixtures.scalar(
            value = null,
            sourceLine = "values: null",
        )
        val listNode = ParserNodeFixtures.value(
            value = listOf("first"),
            sourceLine = "values: [first]",
        )

        assertNull(nullNode.expect<List<String>?>())
        assertEquals(listOf("first"), listNode.expect<List<String>?>())
    }

    @Test
    fun `expect preserves free form values`() {
        val expected = linkedMapOf<String, Any?>(
            "name" to "example",
            "values" to listOf(1, true, null),
        )
        val node = ParserNodeFixtures.value(
            value = expected,
            sourceLine = "value: {name: example, values: [1, true, null]}",
        )

        assertEquals(expected, node.expect<Any>())
    }

    @Test
    fun `expect preserves star projected collection values`() {
        val expectedList = listOf("first", 2, null)
        val expectedMap = linkedMapOf<String, Any?>(
            "name" to "example",
            "values" to expectedList,
        )
        val listNode = ParserNodeFixtures.value(
            value = expectedList,
            sourceLine = "values: [first, 2, null]",
        )
        val mapNode = ParserNodeFixtures.value(
            value = expectedMap,
            sourceLine = "value: {name: example, values: [first, 2, null]}",
        )

        assertEquals(expectedList, listNode.expect<List<*>>())
        assertEquals(expectedMap, mapNode.expect<Map<*, *>>())
    }

    @Test
    fun `expect validates map key types at their exact member path`() {
        val node = ParserNodeFixtures.value(
            value = mapOf("name" to "example"),
            sourceLine = "value: {name: example}",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<Map<Int, String>>()
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(typeOf<Int>(), diagnostic.expectedType)
        assertEquals(String::class, diagnostic.actualType)
        assertEquals("name", diagnostic.actualValue)
        assertEquals("test.root.value.name", diagnostic.path)
        assertEquals("test.root.value.name", diagnostic.sourceLocation.path)
    }

    @Test
    fun `expect reports structural mismatches with a parser diagnostic`() {
        val node = ParserNodeFixtures.value(
            value = listOf("first"),
            sourceLine = "value: [first]",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<Map<String, Any?>>()
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(typeOf<Map<String, Any?>>(), diagnostic.expectedType)
        assertEquals(List::class, diagnostic.actualType)
        assertEquals(listOf("first"), diagnostic.actualValue)
        assertEquals("test.root.value", diagnostic.path)
    }

    @Test
    fun `optional distinguishes an absent member from explicit null`() {
        val node = ParserNodeFixtures.value(
            value = mapOf("description" to null),
            sourceLine = "description: null",
        )

        assertNull(node.optional("missing"))
        val explicitNull = assertNotNull(node.optional("description"))
        assertNull(explicitNull.expect<String?>())
    }

    @Test
    fun `required returns explicit null for deliberate type handling`() {
        val node = ParserNodeFixtures.value(
            value = mapOf("description" to null),
            sourceLine = "description: null",
        )

        assertNull(node.required("description").expect<String?>())
    }

    @Test
    fun `required reports an absent member with structured source information`() {
        val node = ParserNodeFixtures.value(
            value = emptyMap<String, Any?>(),
            sourceLine = "info: {}",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.required("title")
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("title", diagnostic.memberName)
        assertEquals("test.root.value.title", diagnostic.path)
        assertEquals("test.root.value", diagnostic.sourceLocation.path)
    }

    @Test
    fun `member navigation rejects a non-object node`() {
        val node = ParserNodeFixtures.scalar(
            value = "not-an-object",
            sourceLine = "value: not-an-object",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.optional("title")
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(typeOf<Map<String, Any?>>(), diagnostic.expectedType)
        assertEquals(String::class, diagnostic.actualType)
        assertEquals("test.root.value", diagnostic.path)
    }

    @Test
    fun `members and elements preserve individual parser paths`() {
        val objectNode = ParserNodeFixtures.value(
            value = linkedMapOf("first" to true, "second" to false),
            sourceLine = "value: {first: true, second: false}",
        )
        val arrayNode = ParserNodeFixtures.value(
            value = listOf("first", "second"),
            sourceLine = "value: [first, second]",
        )

        assertEquals(
            listOf("test.root.value.first", "test.root.value.second"),
            objectNode.members().map(ParserNode::path),
        )
        assertEquals(
            listOf("test.root.value[0]", "test.root.value[1]"),
            arrayNode.elements().map(ParserNode::path),
        )
    }

    @Test
    fun `members rejects a non-object node with an object expectation`() {
        val node = ParserNodeFixtures.value(
            value = listOf("first"),
            sourceLine = "value: [first]",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.members()
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(typeOf<Map<String, Any?>>(), diagnostic.expectedType)
        assertEquals(List::class, diagnostic.actualType)
        assertEquals("test.root.value", diagnostic.path)
    }

    @Test
    fun `elements rejects a non-array node with an array expectation`() {
        val node = ParserNodeFixtures.value(
            value = mapOf("first" to true),
            sourceLine = "value: {first: true}",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.elements()
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(typeOf<List<Any?>>(), diagnostic.expectedType)
        assertEquals(Map::class, diagnostic.actualType)
        assertEquals("test.root.value", diagnostic.path)
    }
}
