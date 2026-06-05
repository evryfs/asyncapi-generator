package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class MessageParserTest : ParserTestSupport() {

    private val parser = MessageParser(asyncApiContext)

    @Test
    fun parseMessages_validate_data_class() {
        val messagesNode = readNode(
            "parser/messages/asyncapi_parser_message_valid.yaml",
            "components",
            "messages",
        )
        val result = parser.parseMap(messagesNode)

        assertTrue("lightMeasured" in result)
        assertTrue("turnOnOff" in result)
        assertTrue("referencedMessage" in result)

        val lightMeasured = (result["lightMeasured"] as MessageInterface.MessageInline).message
        val expectedLightMeasured = lightMeasured()
        assertThat(lightMeasured)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedLightMeasured)

        val turnOnOff = (result["turnOnOff"] as MessageInterface.MessageInline).message
        val expectedTurnOnOff = turnOnOff()
        assertThat(turnOnOff)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedTurnOnOff)

        val referencedMessage = (result["referencedMessage"] as MessageInterface.MessageReference).reference
        val expectedReferencedMessage = referencedMessage()
        assertThat(referencedMessage)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedReferencedMessage)
    }

    @Test
    fun `parse valid message objects`() {
        val messagesNode = readNode(
            "parser/messages/asyncapi_parser_message_edge_cases.yaml",
            "components",
            "messages",
        )
        val messages = parser.parseMap(messagesNode)

        val refPayload = (messages["RefPayload"] as MessageInterface.MessageInline).message
        assertThat(refPayload)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(refPayloadMessage())

        val refCorrelation = (messages["RefCorrelationId"] as MessageInterface.MessageInline).message
        assertThat(refCorrelation)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(refCorrelationIdMessage())

    }

    @Test
    fun `parse message with inline trait`() {
        val messagesNode = readNode(
            "parser/messages/asyncapi_parser_message_edge_cases.yaml",
            "components",
            "messages",
        )
        val messages = parser.parseMap(messagesNode)

        val emptyPayload = (messages["EmptyPayloadMessage"] as MessageInterface.MessageInline).message
        assertThat(emptyPayload)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(emptyPayloadMessage())

        val inlineTrait = (messages["InlineTraitMessage"] as MessageInterface.MessageInline).message
        assertThat(inlineTrait)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(inlineTraitMessage())
    }

    @Test
    fun `parse message with invalid field type throws InvalidValue`() {
        val messagesNode = readNode(
            "parser/messages/asyncapi_parser_message_invalid_type.yaml",
            "components",
            "messages",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected String",
            "12345",
            "quote the value",
            "asyncapi_parser_message_invalid_type.yaml",
            "asyncapi_parser_message_invalid_type.root.components.messages.InvalidNameMessage.name",
        ) {
            parser.parseMap(messagesNode)
        }
    }
}
