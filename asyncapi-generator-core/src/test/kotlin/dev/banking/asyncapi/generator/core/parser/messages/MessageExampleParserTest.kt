package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessageExampleParserTest : ParserTestSupport() {

    private val parser = MessageExampleParser(asyncApiContext)

    @Test
    fun `parse message examples`() {
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_valid.yaml",
            "components",
            "messages",
            "lightMeasured",
            "examples",
        )

        val examples = parser.parseList(examplesNode)

        assertEquals(2, examples.size)
        val example = examples.first()
        assertEquals("lightMeasurementExample", example.name)
        assertEquals("Example of light measurement payload", example.summary)
        assertNotNull(example.headers)
        assertNotNull(example.payload)
    }

    @Test
    fun `parse message example with invalid structure throws UnexpectedValue`() {
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_example_invalid.yaml",
            "components",
            "messageExampleCases",
            "InvalidExampleStructure",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_message_example_invalid.yaml",
            "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.InvalidExampleStructure[0]",
        ) {
            parser.parseList(examplesNode)
        }
    }

    @Test
    fun `parse message example with invalid headers throws UnexpectedValue`() {
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_example_invalid.yaml",
            "components",
            "messageExampleCases",
            "InvalidHeaders",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_message_example_invalid.yaml",
            "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.InvalidHeaders[0].headers",
        ) {
            parser.parseList(examplesNode)
        }
    }

    @Test
    fun `parse message example with boolean name throws UnexpectedValue`() {
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_example_invalid.yaml",
            "components",
            "messageExampleCases",
            "BooleanName",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected String",
            "found Boolean true",
            "quote the value",
            "asyncapi_parser_message_example_invalid.yaml",
            "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.BooleanName[0].name",
        ) {
            parser.parseList(examplesNode)
        }
    }
}
