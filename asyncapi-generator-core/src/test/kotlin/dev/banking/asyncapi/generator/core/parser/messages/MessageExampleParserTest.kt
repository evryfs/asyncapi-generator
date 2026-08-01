package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MessageExampleParserTest {

    private val context = AsyncApiContext()
    private val parser = MessageExampleParser(context)

    @Test
    fun `parse message examples`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val examplesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messages")
            .expectObject().required("lightMeasured")
            .expectObject().required("examples")

        val examples = parser.parseList(examplesNode)

        assertEquals(2, examples.size)
        val example = examples.first()
        assertEquals("lightMeasurementExample", example.name)
        assertEquals("Example of light measurement payload", example.summary)
        assertNotNull(example.headers)
        assertEquals(
            mapOf(
                "lumens" to 1200,
                "sentAt" to "2024-09-12T12:00:00Z",
            ),
            example.payload,
        )
    }

    @Test
    fun `parse message example with invalid structure reports its expected type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_example_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val examplesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageExampleCases")
            .expectObject().required("InvalidExampleStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(examplesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.InvalidExampleStructure[0]",
            diagnostic.path,
        )
        assertEquals(
            "root.components.messageExampleCases.InvalidExampleStructure[0]",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_message_example_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse message example with invalid headers reports its expected type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_example_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val examplesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageExampleCases")
            .expectObject().required("InvalidHeaders")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(examplesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.InvalidHeaders[0].headers",
            diagnostic.path,
        )
        assertEquals("root.components.messageExampleCases.InvalidHeaders[0].headers", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_message_example_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse message example with boolean name reports its expected type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_example_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val examplesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageExampleCases")
            .expectObject().required("BooleanName")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(examplesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.BooleanName[0].name",
            diagnostic.path,
        )
        assertEquals("root.components.messageExampleCases.BooleanName[0].name", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_message_example_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse message example list from an object reports the container type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_example_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val examplesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageExampleCases")
            .expectObject().required("ObjectInsteadOfList")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(examplesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("List<Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.OBJECT, diagnostic.actualType)
        assertEquals(
            mapOf("invalidExample" to mapOf("name" to "example")),
            diagnostic.actualValue,
        )
        assertEquals(
            "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.ObjectInsteadOfList",
            diagnostic.path,
        )
        assertEquals("root.components.messageExampleCases.ObjectInsteadOfList", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_message_example_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
