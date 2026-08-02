package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class OperationReplyParserTest {

    private val context = AsyncApiContext()
    private val parser = OperationReplyParser(context)

    @Test
    fun `parses an inline operation reply with address channel and messages`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val replyNode = ParserNodeFactory.root(document, context)
            .expectObject().required("operations")
            .expectObject().required("receiveLightMeasurement")
            .expectObject().required("reply")

        val reply = assertIs<OperationReplyInterface.OperationReplyInline>(
            parser.parseElement(replyNode),
        ).operationReply

        val address = assertIs<OperationReplyAddressInterface.OperationReplyAddressInline>(reply.address)
            .operationReplyAddress
        assertEquals("\$message.header#/replyTo", address.location)
        assertEquals("#/channels/lightingMeasured", reply.channel?.ref)
        assertEquals(listOf("#/components/messages/lightMeasured"), reply.messages?.map { it.ref })
    }

    @Test
    fun `parses a referenced operation reply with its concrete category`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val replyNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("replies")
            .expectObject().required("referencedReply")

        val reference = assertIs<OperationReplyInterface.OperationReplyReference>(parser.parseElement(replyNode))
            .reference

        assertEquals("#/components/replies/standardReply", reference.ref)
        assertEquals(OPERATION_REPLY, reference.referenceCategoryKey)
    }

    @Test
    fun `parse operation reply reports missing channel ref`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val replyNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyCases")
            .expectObject().required("MissingChannelReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(replyNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("\$ref", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingChannelReference.channel.\$ref",
            diagnostic.path,
        )
        assertEquals(
            "root.components.operationReplyCases.MissingChannelReference.channel",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operation_reply_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation reply reports missing message ref at indexed path`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val replyNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyCases")
            .expectObject().required("MissingMessageReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(replyNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("\$ref", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingMessageReference.messages[0].\$ref",
            diagnostic.path,
        )
        assertEquals(
            "root.components.operationReplyCases.MissingMessageReference.messages[0]",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operation_reply_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation reply with missing address location reports the required member and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val replyNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyCases")
            .expectObject().required("MissingAddressLocation")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(replyNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("location", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals(
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingAddressLocation.address.location",
            diagnostic.path,
        )
        assertEquals(
            "root.components.operationReplyCases.MissingAddressLocation.address",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operation_reply_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation reply with null reference reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val replyNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyCases")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(replyNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.NullReference.\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.operationReplyCases.NullReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operation_reply_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation reply map from an array reports the container type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_reply_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val repliesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationReplyCases")
            .expectObject().required("ArrayInsteadOfMap")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(repliesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(
            listOf(mapOf("channel" to mapOf("\$ref" to "#/channels/myChannel"))),
            diagnostic.actualValue,
        )
        assertEquals(
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.ArrayInsteadOfMap",
            diagnostic.path,
        )
        assertEquals("root.components.operationReplyCases.ArrayInsteadOfMap", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operation_reply_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
