package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MessageTraitParserTest {

    private val context = AsyncApiContext()
    private val parser = MessageTraitParser(context)

    @Test
    fun `parse message trait list`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_edge_cases.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messages")
            .expectObject().required("InlineTraitMessage")
            .expectObject().required("traits")

        val traits = parser.parseList(traitsNode)

        val trait = assertIs<MessageTraitInterface.InlineMessageTrait>(traits.single()).trait
        val headers = assertIs<SchemaInterface.SchemaInline>(trait.headers).schema
        assertEquals("string", headers.type)
    }

    @Test
    fun `parse message trait with invalid structure reports its expected type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageTraitCases")
            .expectObject().required("InvalidTraitStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.InvalidTraitStructure.badTrait",
            diagnostic.path,
        )
        assertEquals(
            "root.components.messageTraitCases.InvalidTraitStructure.badTrait",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_message_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse message trait with boolean content type reports its expected type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageTraitCases")
            .expectObject().required("BooleanContentType")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.BooleanContentType.badTrait.contentType",
            diagnostic.path,
        )
        assertEquals(
            "root.components.messageTraitCases.BooleanContentType.badTrait.contentType",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_message_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse message trait with invalid example structure reports its expected type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageTraitCases")
            .expectObject().required("InvalidExampleStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.InvalidExampleStructure.badTrait.examples[0]",
            diagnostic.path,
        )
        assertEquals(
            "root.components.messageTraitCases.InvalidExampleStructure.badTrait.examples[0]",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_message_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse message trait with numeric reference reports its expected type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageTraitCases")
            .expectObject().required("NumericReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(42, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.NumericReference.badTrait.\$ref",
            diagnostic.path,
        )
        assertEquals(
            "root.components.messageTraitCases.NumericReference.badTrait.\$ref",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_message_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse message trait list from an object reports the container type and source`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageTraitCases")
            .expectObject().required("ObjectInsteadOfList")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("List<Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.OBJECT, diagnostic.actualType)
        assertEquals(
            mapOf("badTrait" to mapOf("contentType" to "application/json")),
            diagnostic.actualValue,
        )
        assertEquals(
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.ObjectInsteadOfList",
            diagnostic.path,
        )
        assertEquals("root.components.messageTraitCases.ObjectInsteadOfList", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_message_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
