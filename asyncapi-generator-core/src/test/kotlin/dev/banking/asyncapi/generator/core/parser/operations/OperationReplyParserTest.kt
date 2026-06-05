package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OperationReplyParserTest : ParserTestSupport() {

    private val parser = OperationReplyParser(asyncApiContext)

    @Test
    fun `parse operation reply with address`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operations_valid.yaml",
            "operations",
            "receiveLightMeasurement",
            "reply",
        )

        val replyInterface = parser.parseElement(replyNode)
        assertNotNull(replyInterface, "Reply should be present")
        assertTrue(replyInterface is OperationReplyInterface.OperationReplyInline)

        val reply = replyInterface.operationReply

        // Assertions for the reply (extracted from receiveLightMeasurement's reply section)
        assertNotNull(reply.address, "Reply address should be present")
        assertTrue(reply.address is OperationReplyAddressInterface.OperationReplyAddressInline)
        assertEquals($$"$message.header#/replyTo", reply.address.operationReplyAddress.location)

        assertNotNull(reply.channel, "Reply channel should be present")
        assertEquals("#/channels/lightingMeasured", reply.channel.ref)

        assertNotNull(reply.messages, "Reply messages should be present")
        assertEquals(listOf("#/components/messages/lightMeasured"), reply.messages.map { it.ref })
    }

    @Test
    fun `parse operation reply with missing channel ref throws Mandatory`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "MissingChannelReference",
        )
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory '\$ref'",
            "asyncapi_parser_operation_reply_invalid.yaml",
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingChannelReference.channel.\$ref",
        ) {
            parser.parseElement(replyNode)
        }
    }

    @Test
    fun `parse operation reply with missing message ref throws Mandatory with indexed path`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "MissingMessageReference",
        )
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory '\$ref'",
            "asyncapi_parser_operation_reply_invalid.yaml",
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingMessageReference.messages[0].\$ref",
        ) {
            parser.parseElement(replyNode)
        }
    }

    @Test
    fun `parse operation reply with missing address location throws Mandatory`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "MissingAddressLocation",
        )
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory 'location'",
            "asyncapi_parser_operation_reply_invalid.yaml",
            "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingAddressLocation.address.location",
        ) {
            parser.parseElement(replyNode)
        }
    }
}
