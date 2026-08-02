package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ParserNodeTest {

    @Test
    fun `child cursors retain the selected parser profile`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content =
                """
                object:
                  member: true
                array:
                  - false
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)
            .withProfile(AsyncApiParserProfile.V3_0)

        assertEquals(AsyncApiParserProfile.V3_0, node.expectObject().required("object").profile)
        assertEquals(
            AsyncApiParserProfile.V3_0,
            node.expectObject().required("object").expectObject().members().single().profile,
        )
        assertEquals(
            AsyncApiParserProfile.V3_0,
            node.expectObject().required("array").expectArray().elements().single().profile,
        )
    }

    @Test
    fun `required reports a structured source-located diagnostic for an absent member`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "present: true",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expectObject().required("missing")
        }

        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("missing", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertNull(diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("asyncapi.root.missing", diagnostic.path)
        assertEquals("asyncapi.yaml", diagnostic.sourceLocation.file.name)
        assertEquals(1, diagnostic.sourceLocation.line)
        assertEquals(1, diagnostic.sourceLocation.column)
        assertContains(error.message.orEmpty(), "Missing required member 'missing'")
        assertContains(error.message.orEmpty(), "asyncapi.yaml (asyncapi.root.missing)")
    }

    @Test
    fun `optional distinguishes an absent member from an explicit null`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "explicit: null",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        assertNull(node.expectObject().optional("absent"))
        val explicitNull = assertIs<ParserNode>(node.expectObject().optional("explicit"))
        assertIs<DocumentNull>(explicitNull.node)
        assertNull(explicitNull.expect<String?>())
    }

    @Test
    fun `expect rejects explicit null for a non-null type at the value path`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "explicit: null",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)
            .expectObject().required("explicit")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<String>()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("asyncapi.root.explicit", diagnostic.path)
        assertEquals("asyncapi.yaml", diagnostic.sourceLocation.file.name)
        assertEquals(1, diagnostic.sourceLocation.line)
        assertEquals(11, diagnostic.sourceLocation.column)
    }

    @Test
    fun `members and elements retain parser paths`() {
        val objectContext = AsyncApiContext()
        val objectSource = DocumentSource(
            id = "object",
            file = File("object.yaml").canonicalFile,
            content =
                """
                first: true
                second: false
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val objectDocument = DocumentReaderRegistry.read(objectSource)
        val objectNode = ParserNodeFactory.root(objectDocument, objectContext)

        val arrayContext = AsyncApiContext()
        val arraySource = DocumentSource(
            id = "array",
            file = File("array.yaml").canonicalFile,
            content =
                """
                - first
                - second
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val arrayDocument = DocumentReaderRegistry.read(arraySource)
        val arrayNode = ParserNodeFactory.root(arrayDocument, arrayContext)

        assertEquals(
            listOf("object.root.first", "object.root.second"),
            objectNode.expectObject().members().map(ParserNode::path),
        )
        assertEquals(
            listOf("array.root[0]", "array.root[1]"),
            arrayNode.expectArray().elements().map(ParserNode::path),
        )
    }

    @Test
    fun `expect object rejects an array with a structured type diagnostic`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "- value",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expectObject().members()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals("asyncapi.root", diagnostic.path)
    }

    @Test
    fun `object view selects members by prefix without changing their paths`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content =
                """
                name: example
                x-owner: team
                x-null: null
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        val extensions = node.expectObject().membersStartingWith("x-")

        assertEquals(listOf("x-owner", "x-null"), extensions.map(ParserNode::name))
        assertEquals(
            listOf("asyncapi.root.x-owner", "asyncapi.root.x-null"),
            extensions.map(ParserNode::path),
        )
    }

    @Test
    fun `expect only members rejects an unknown key and permits specification extensions`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content =
                """
                known: true
                x-owner: team
                unknown: false
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expectObject().expectOnlyMembers("Test Object", setOf("known"))
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedObjectMember>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_OBJECT_MEMBER, diagnostic.category)
        assertEquals("unknown", diagnostic.memberName)
        assertEquals("Test Object member or x- specification extension", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("unknown", diagnostic.actualValue)
        assertEquals("asyncapi.root.unknown", diagnostic.path)
        assertContains(error.message.orEmpty(), "Unexpected member 'unknown'")
    }

    @Test
    fun `expect array rejects an object with a structured type diagnostic`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "value: true",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expectArray().elements()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals("List<Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.OBJECT, diagnostic.actualType)
        assertEquals("asyncapi.root", diagnostic.path)
    }

    @Test
    fun `expect recursively checks nested generic values`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content =
                """
                - label: first
                - label: 7
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<List<Map<String, String>>>()
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(7, diagnostic.actualValue)
        assertEquals("asyncapi.root[1].label", diagnostic.path)
        assertEquals(2, diagnostic.sourceLocation.line)
        assertEquals(10, diagnostic.sourceLocation.column)
    }

    @Test
    fun `expect permits nullable nested values`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "- label: null",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        val value = node.expect<List<Map<String, String?>>>()

        assertEquals(listOf(mapOf("label" to null)), value)
    }

    @Test
    fun `expect any preserves free-form JSON-compatible values`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content =
                """
                array:
                  - 1
                  - true
                  - null
                  - nested: value
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)
        val expected = mapOf(
            "array" to listOf(1, true, null, mapOf("nested" to "value")),
        )

        assertEquals(expected, node.expect<Any?>())
    }

    @Test
    fun `to plain value preserves an explicit null`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "null",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)

        assertNull(node.toPlainValue())
    }

    @Test
    fun `expect retains scalar guidance in its formatted diagnostic`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "deprecated: \"true\"",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)
            .expectObject().required("deprecated")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<Boolean>()
        }

        assertContains(error.message.orEmpty(), "expected Boolean")
        assertContains(error.message.orEmpty(), "found String \"true\"")
        assertContains(error.message.orEmpty(), "quoted booleans are strings in YAML")
        assertContains(error.message.orEmpty(), "deprecated: \"true\"")
    }

    @Test
    fun `expect reports quoted number as yaml string when number is expected`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "minLength: \"12\"",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)
            .expectObject().required("minLength")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<Number>()
        }

        assertContains(error.message.orEmpty(), "expected Number")
        assertContains(error.message.orEmpty(), "found String \"12\"")
        assertContains(error.message.orEmpty(), "quoted numbers are strings in YAML")
        assertContains(error.message.orEmpty(), "minLength: \"12\"")
    }

    @Test
    fun `expect reports unquoted boolean when string is expected`() {
        val context = AsyncApiContext()
        val source = DocumentSource(
            id = "asyncapi",
            file = File("asyncapi.yaml").canonicalFile,
            content = "version: true",
            format = DocumentFormat.YAML,
        )
        val document = DocumentReaderRegistry.read(source)
        val node = ParserNodeFactory.root(document, context)
            .expectObject().required("version")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            node.expect<String>()
        }

        assertContains(error.message.orEmpty(), "expected String")
        assertContains(error.message.orEmpty(), "found Boolean true")
        assertContains(error.message.orEmpty(), "quote the value")
        assertContains(error.message.orEmpty(), "version: true")
    }
}
