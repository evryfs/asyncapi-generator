package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OperationReplyParserTest : AbstractParserTest() {

    private val parser = OperationReplyParser(asyncApiContext)

    @Test
    fun `parse operation reply with address`() {
        val root = readYaml("src/test/resources/parser/operations/asyncapi_parser_operations_valid.yaml")
        val replyNode = root
            .mandatory("operations")
            .mandatory("receiveLightMeasurement")
            .mandatory("reply")

        val replyInterface = parser.parseElement(replyNode)
        assertNotNull(replyInterface, "Reply should be present")
        assertTrue(replyInterface is OperationReplyInterface.OperationReplyInline)

        val reply = replyInterface.operationReply

        // Assertions for the reply (extracted from receiveLightMeasurement's reply section)
        assertNotNull(reply.address, "Reply address should be present")
        assertTrue(reply.address is OperationReplyAddressInterface.OperationReplyAddressInline)
        assertEquals($$"'$message.header#/replyTo", reply.address.operationReplyAddress.location)

        assertNotNull(reply.channel, "Reply channel should be present")
        assertEquals("'#/channels/lightingMeasured", reply.channel.ref)
    }
}
