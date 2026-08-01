package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
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
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_example_invalid.yaml",
            "components",
            "messageExampleCases",
            "InvalidExampleStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.InvalidExampleStructure[0]",
            sourcePath = "root.components.messageExampleCases.InvalidExampleStructure[0]",
            sourceFile = "asyncapi_parser_message_example_invalid.yaml",
        ) {
            parser.parseList(examplesNode)
        }
    }

    @Test
    fun `parse message example with invalid headers reports its expected type and source`() {
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_example_invalid.yaml",
            "components",
            "messageExampleCases",
            "InvalidHeaders",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.InvalidHeaders[0].headers",
            sourcePath = "root.components.messageExampleCases.InvalidHeaders[0].headers",
            sourceFile = "asyncapi_parser_message_example_invalid.yaml",
        ) {
            parser.parseList(examplesNode)
        }
    }

    @Test
    fun `parse message example with boolean name reports its expected type and source`() {
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_example_invalid.yaml",
            "components",
            "messageExampleCases",
            "BooleanName",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = true,
            path = "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.BooleanName[0].name",
            sourcePath = "root.components.messageExampleCases.BooleanName[0].name",
            sourceFile = "asyncapi_parser_message_example_invalid.yaml",
        ) {
            parser.parseList(examplesNode)
        }
    }

    @Test
    fun `parse message example list from an object reports the container type and source`() {
        val examplesNode = readNode(
            "parser/messages/asyncapi_parser_message_example_invalid.yaml",
            "components",
            "messageExampleCases",
            "ObjectInsteadOfList",
        )
        assertUnexpectedValueType(
            expectedType = "List<Any?>",
            actualType = ParserValueType.OBJECT,
            actualValue = mapOf("invalidExample" to mapOf("name" to "example")),
            path = "asyncapi_parser_message_example_invalid.root.components.messageExampleCases.ObjectInsteadOfList",
            sourcePath = "root.components.messageExampleCases.ObjectInsteadOfList",
            sourceFile = "asyncapi_parser_message_example_invalid.yaml",
        ) {
            parser.parseList(examplesNode)
        }
    }
}
