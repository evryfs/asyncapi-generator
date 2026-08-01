package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
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
    fun `parse referenced operation`() {
        val operationsNode = readNode("parser/operations/asyncapi_parser_operations_valid.yaml", "operations")
        val result = parser.parseMap(operationsNode)

        val reference = (result["referencedOperation"] as OperationInterface.OperationReference).reference
        assertThat(reference.ref).isEqualTo("#/operations/receiveLightMeasurement")
    }

    @Test
    fun `parse operation missing action reports the required member and source`() {
        val operationsNode = readNode("parser/operations/asyncapi_parser_operations_invalid.yaml", "operations")
        assertMissingRequiredMember(
            memberName = "action",
            path = "asyncapi_parser_operations_invalid.root.operations.MissingAction.action",
            sourcePath = "root.operations.MissingAction",
            sourceFile = "asyncapi_parser_operations_invalid.yaml",
        ) {
            parser.parseMap(operationsNode)
        }
    }

    @Test
    fun `parse operation with boolean action reports its expected type and source`() {
        val operationsNode = readNode(
            "parser/operations/asyncapi_parser_operations_invalid.yaml",
            "operationCases",
            "BooleanAction",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = false,
            path = "asyncapi_parser_operations_invalid.root.operationCases.BooleanAction.invalidOperation.action",
            sourcePath = "root.operationCases.BooleanAction.invalidOperation.action",
            sourceFile = "asyncapi_parser_operations_invalid.yaml",
        ) {
            parser.parseMap(operationsNode)
        }
    }

    @Test
    fun `parse operation with null reference reports its expected type and source`() {
        val operationsNode = readNode(
            "parser/operations/asyncapi_parser_operations_invalid.yaml",
            "operationCases",
            "NullReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_operations_invalid.root.operationCases.NullReference.invalidOperation.\$ref",
            sourcePath = "root.operationCases.NullReference.invalidOperation.\$ref",
            sourceFile = "asyncapi_parser_operations_invalid.yaml",
        ) {
            parser.parseMap(operationsNode)
        }
    }

    @Test
    fun `parse operation map from an array reports the container type and source`() {
        val operationsNode = readNode(
            "parser/operations/asyncapi_parser_operations_invalid.yaml",
            "operationCases",
            "ArrayInsteadOfMap",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("action" to "send")),
            path = "asyncapi_parser_operations_invalid.root.operationCases.ArrayInsteadOfMap",
            sourcePath = "root.operationCases.ArrayInsteadOfMap",
            sourceFile = "asyncapi_parser_operations_invalid.yaml",
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
        assertMissingRequiredMember(
            memberName = $$"$ref",
            path = "asyncapi_validator_operations_inline_message_error.root.operations.testOperation.messages[0].\$ref",
            sourcePath = "root.operations.testOperation.messages[0]",
            sourceFile = "asyncapi_validator_operations_inline_message_error.yaml",
        ) {
            parser.parseMap(operationsNode)
        }
    }
}
