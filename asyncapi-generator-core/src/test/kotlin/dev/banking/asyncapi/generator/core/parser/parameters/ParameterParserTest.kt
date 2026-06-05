package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParameterParserTest : ParserTestSupport() {

    private val parser = ParameterParser(asyncApiContext)

    @Test
    fun `parse parameters`() {
        val parametersNode = readNode(
            "parser/channels/asyncapi_parser_channel_valid.yaml",
            "channels",
            "lightStatus",
            "parameters",
        )

        val parameters = parser.parseMap(parametersNode)

        assertTrue(parameters["city"] is ParameterInterface.ParameterInline)
        val city = (parameters["city"] as ParameterInterface.ParameterInline).parameter
        assertEquals("The city where the streetlights are located.", city.description)
        assertEquals($$"$message.payload#/city", city.location)
        assertEquals(listOf("helsinki", "oslo", "stockholm"), city.enum)
        assertEquals("helsinki", city.default)
        assertEquals(listOf("helsinki", "oslo"), city.examples)
    }

    @Test
    fun `parse parameter with invalid structure throws UnexpectedValue`() {
        val parametersNode = readNode(
            "parser/parameters/asyncapi_parser_parameter_invalid.yaml",
            "components",
            "parameterCases",
            "InvalidParameterStructure",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_parameter_invalid.yaml",
            "asyncapi_parser_parameter_invalid.root.components.parameterCases.InvalidParameterStructure.badParameter",
        ) {
            parser.parseMap(parametersNode)
        }
    }

    @Test
    fun `parse parameter with boolean location throws UnexpectedValue`() {
        val parametersNode = readNode(
            "parser/parameters/asyncapi_parser_parameter_invalid.yaml",
            "components",
            "parameterCases",
            "BooleanLocation",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected String",
            "found Boolean true",
            "quote the value",
            "asyncapi_parser_parameter_invalid.yaml",
            "asyncapi_parser_parameter_invalid.root.components.parameterCases.BooleanLocation.badParameter.location",
        ) {
            parser.parseMap(parametersNode)
        }
    }
}
