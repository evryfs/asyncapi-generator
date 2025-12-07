package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class OperationParserTest : AbstractParserTest() {

    private val parser = OperationParser(asyncApiContext)

    @Test
    fun parseOperations_validate_data_class() {
        val root = readYaml("src/test/resources/parser/operations/asyncapi_parser_operations_valid.yaml")
        val result = parser.parseMap(root.mandatory("operations"))

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
        val root = readYaml("src/test/resources/parser/operations/asyncapi_parser_operations_invalid.yaml")
        assertFailsWith<AsyncApiParseException.Mandatory> {
            parser.parseMap(root.mandatory("operations"))
        }
    }

    @Test
    fun `validation fails for operation with inline message definition`() {
        val root = readYaml("src/test/resources/parser/operations/asyncapi_validator_operations_inline_message_error.yaml")
        assertFailsWith<AsyncApiParseException.Mandatory> {
            parser.parseMap(root.mandatory("operations"))
        }
    }
}
