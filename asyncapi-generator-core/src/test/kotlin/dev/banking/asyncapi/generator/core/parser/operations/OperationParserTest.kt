package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class OperationParserTest : ParserTestSupport() {

    private val parser = OperationParser(asyncApiContext)

    @Test
    fun parseOperations_validate_data_class() {
        val operationsNode = readNode("parser/operations/asyncapi_parser_operations_valid.yaml", "operations")
        val result = parser.parseMap(operationsNode)

        assertTrue("receiveLightMeasurement" in result)
        assertTrue("turnOn" in result)

        val receiveLightMeasurement =
            (result["receiveLightMeasurement"] as OperationInterface.OperationInline).operation
        val expectedReceiveLightMeasurement = receiveLightMeasurement()
        assertThat(receiveLightMeasurement)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedReceiveLightMeasurement)

        val turnOn = (result["turnOn"] as OperationInterface.OperationInline).operation
        val expectedTurnOn = turnOn()
        assertThat(turnOn)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedTurnOn)
    }

    @Test
    fun `parse operation missing action throws RequiredObject`() {
        val operationsNode = readNode("parser/operations/asyncapi_parser_operations_invalid.yaml", "operations")
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory 'action'",
            "asyncapi_parser_operations_invalid.yaml",
            "asyncapi_parser_operations_invalid.root.operations.MissingAction.action",
        ) {
            parser.parseMap(operationsNode)
        }
    }

    @Test
    fun `validation fails for operation with inline message definition`() {
        val operationsNode = readNode(
            "parser/operations/asyncapi_validator_operations_inline_message_error.yaml",
            "operations",
        )
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory '\$ref'",
            "asyncapi_validator_operations_inline_message_error.yaml",
            "asyncapi_validator_operations_inline_message_error.root.operations.testOperation.messages[0].\$ref",
        ) {
            parser.parseMap(operationsNode)
        }
    }
}
