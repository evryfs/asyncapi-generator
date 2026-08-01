package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
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
    fun `parse operation reply reports missing channel ref`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "MissingChannelReference",
        )
        assertMissingRequiredMember(
            memberName = $$"$ref",
            path = "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingChannelReference.channel.\$ref",
            sourcePath = "root.components.operationReplyCases.MissingChannelReference.channel",
            sourceFile = "asyncapi_parser_operation_reply_invalid.yaml",
        ) {
            parser.parseElement(replyNode)
        }
    }

    @Test
    fun `parse operation reply reports missing message ref at indexed path`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "MissingMessageReference",
        )
        assertMissingRequiredMember(
            memberName = $$"$ref",
            path = "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingMessageReference.messages[0].\$ref",
            sourcePath = "root.components.operationReplyCases.MissingMessageReference.messages[0]",
            sourceFile = "asyncapi_parser_operation_reply_invalid.yaml",
        ) {
            parser.parseElement(replyNode)
        }
    }

    @Test
    fun `parse operation reply with missing address location reports the required member and source`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "MissingAddressLocation",
        )
        assertMissingRequiredMember(
            memberName = "location",
            path = "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.MissingAddressLocation.address.location",
            sourcePath = "root.components.operationReplyCases.MissingAddressLocation.address",
            sourceFile = "asyncapi_parser_operation_reply_invalid.yaml",
        ) {
            parser.parseElement(replyNode)
        }
    }

    @Test
    fun `parse operation reply with null reference reports its expected type and source`() {
        val replyNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "NullReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.NullReference.\$ref",
            sourcePath = "root.components.operationReplyCases.NullReference.\$ref",
            sourceFile = "asyncapi_parser_operation_reply_invalid.yaml",
        ) {
            parser.parseElement(replyNode)
        }
    }

    @Test
    fun `parse operation reply map from an array reports the container type and source`() {
        val repliesNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_invalid.yaml",
            "components",
            "operationReplyCases",
            "ArrayInsteadOfMap",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("channel" to mapOf("\$ref" to "#/channels/myChannel"))),
            path = "asyncapi_parser_operation_reply_invalid.root.components.operationReplyCases.ArrayInsteadOfMap",
            sourcePath = "root.components.operationReplyCases.ArrayInsteadOfMap",
            sourceFile = "asyncapi_parser_operation_reply_invalid.yaml",
        ) {
            parser.parseMap(repliesNode)
        }
    }
}
