package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
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
    fun `parse parameter reports invalid inline structure`() {
        val parametersNode = readNode(
            "parser/parameters/asyncapi_parser_parameter_invalid.yaml",
            "components",
            "parameterCases",
            "InvalidParameterStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_parameter_invalid.root.components.parameterCases.InvalidParameterStructure.badParameter",
            sourcePath = "root.components.parameterCases.InvalidParameterStructure.badParameter",
            sourceFile = "asyncapi_parser_parameter_invalid.yaml",
        ) {
            parser.parseMap(parametersNode)
        }
    }

    @Test
    fun `parse parameter reports boolean location`() {
        val parametersNode = readNode(
            "parser/parameters/asyncapi_parser_parameter_invalid.yaml",
            "components",
            "parameterCases",
            "BooleanLocation",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = true,
            path = "asyncapi_parser_parameter_invalid.root.components.parameterCases.BooleanLocation.badParameter.location",
            sourcePath = "root.components.parameterCases.BooleanLocation.badParameter.location",
            sourceFile = "asyncapi_parser_parameter_invalid.yaml",
        ) {
            parser.parseMap(parametersNode)
        }
    }

    @Test
    fun `parse parameter reports non-string ref before inline parsing`() {
        val parametersNode = readNode(
            "parser/parameters/asyncapi_parser_parameter_invalid.yaml",
            "components",
            "parameterCases",
            "NumericReference",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 42,
            path = "asyncapi_parser_parameter_invalid.root.components.parameterCases.NumericReference.badParameter.\$ref",
            sourcePath = "root.components.parameterCases.NumericReference.badParameter.\$ref",
            sourceFile = "asyncapi_parser_parameter_invalid.yaml",
        ) {
            parser.parseMap(parametersNode)
        }
    }

    @Test
    fun `parse parameter reports invalid enum element at its index`() {
        val parametersNode = readNode(
            "parser/parameters/asyncapi_parser_parameter_invalid.yaml",
            "components",
            "parameterCases",
            "InvalidEnumElement",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 7,
            path = "asyncapi_parser_parameter_invalid.root.components.parameterCases.InvalidEnumElement.badParameter.enum[1]",
            sourcePath = "root.components.parameterCases.InvalidEnumElement.badParameter.enum[1]",
            sourceFile = "asyncapi_parser_parameter_invalid.yaml",
        ) {
            parser.parseMap(parametersNode)
        }
    }

    @Test
    fun `parse parameter reports null example at its index`() {
        val parametersNode = readNode(
            "parser/parameters/asyncapi_parser_parameter_invalid.yaml",
            "components",
            "parameterCases",
            "InvalidExamplesElement",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_parameter_invalid.root.components.parameterCases.InvalidExamplesElement.badParameter.examples[1]",
            sourcePath = "root.components.parameterCases.InvalidExamplesElement.badParameter.examples[1]",
            sourceFile = "asyncapi_parser_parameter_invalid.yaml",
        ) {
            parser.parseMap(parametersNode)
        }
    }
}
