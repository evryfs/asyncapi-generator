package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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
    fun `parse message with invalid field type reports its expected type and source`() {
        val messagesNode = readNode(
            "parser/messages/asyncapi_parser_message_invalid_type.yaml",
            "components",
            "messageCases",
            "InvalidName",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 12345,
            path = "asyncapi_parser_message_invalid_type.root.components.messageCases.InvalidName.invalidMessage.name",
            sourcePath = "root.components.messageCases.InvalidName.invalidMessage.name",
            sourceFile = "asyncapi_parser_message_invalid_type.yaml",
        ) {
            parser.parseMap(messagesNode)
        }
    }

    @Test
    fun `parse message with null reference reports its expected type and source`() {
        val messagesNode = readNode(
            "parser/messages/asyncapi_parser_message_invalid_type.yaml",
            "components",
            "messageCases",
            "NullReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_message_invalid_type.root.components.messageCases.NullReference.invalidMessage.\$ref",
            sourcePath = "root.components.messageCases.NullReference.invalidMessage.\$ref",
            sourceFile = "asyncapi_parser_message_invalid_type.yaml",
        ) {
            parser.parseMap(messagesNode)
        }
    }

    @Test
    fun `parse message map from an array reports the container type and source`() {
        val messagesNode = readNode(
            "parser/messages/asyncapi_parser_message_invalid_type.yaml",
            "components",
            "messageCases",
            "ArrayInsteadOfMap",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("name" to "validMessage")),
            path = "asyncapi_parser_message_invalid_type.root.components.messageCases.ArrayInsteadOfMap",
            sourcePath = "root.components.messageCases.ArrayInsteadOfMap",
            sourceFile = "asyncapi_parser_message_invalid_type.yaml",
        ) {
            parser.parseMap(messagesNode)
        }
    }

    @Test
    fun `parse message rejects unknown members and permits specification extensions`() {
        val invalidNode = readNode(
            "parser/messages/asyncapi_parser_message_invalid_type.yaml",
            "components",
            "messageCases",
            "UnknownMember",
        )

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(invalidNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedObjectMember>(error.diagnostic)
        assertEquals(ParserDiagnosticCategory.UNEXPECTED_OBJECT_MEMBER, diagnostic.category)
        assertEquals("ImportedMessage", diagnostic.memberName)
        assertEquals(
            "asyncapi_parser_message_invalid_type.root.components.messageCases.UnknownMember.invalidMessage.ImportedMessage",
            diagnostic.path,
        )
        assertEquals("asyncapi_parser_message_invalid_type.yaml", diagnostic.sourceLocation.file.name)

        val extensionNode = readNode(
            "parser/messages/asyncapi_parser_message_invalid_type.yaml",
            "components",
            "messageCases",
            "SpecificationExtension",
        )
        val message = assertIs<MessageInterface.MessageInline>(
            parser.parseMap(extensionNode).getValue("validMessage"),
        ).message
        assertEquals("valid", message.name)
    }
}
