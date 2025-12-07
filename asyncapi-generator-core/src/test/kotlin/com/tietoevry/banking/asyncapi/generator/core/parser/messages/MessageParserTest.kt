package com.tietoevry.banking.asyncapi.generator.core.parser.messages

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MessageParserTest : AbstractParserTest() {

    private val parser = MessageParser(asyncApiContext)

    @Test
    fun parseMessages_validate_data_class() {
        val root = readYaml("src/test/resources/parser/messages/asyncapi_parser_message_valid.yaml")
        val result = parser.parseMap(root.mandatory("components").mandatory("messages"))

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
        val root = readYaml("src/test/resources/parser/messages/asyncapi_parser_message_edge_cases.yaml")
        val messages = parser.parseMap(root.mandatory("components").mandatory("messages"))

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
        val root = readYaml("src/test/resources/parser/messages/asyncapi_parser_message_edge_cases.yaml")
        val messages = parser.parseMap(root.mandatory("components").mandatory("messages"))

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
        val root = readYaml("src/test/resources/parser/messages/asyncapi_parser_message_invalid_type.yaml")
        val messagesNode = root.mandatory("components").mandatory("messages")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseMap(messagesNode)
        }
    }
}
