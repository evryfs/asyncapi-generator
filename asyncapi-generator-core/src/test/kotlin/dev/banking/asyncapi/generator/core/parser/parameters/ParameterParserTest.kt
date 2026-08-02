package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ParameterParserTest {

    private val context = AsyncApiContext()
    private val parser = ParameterParser(context)

    @Test
    fun `parses every parameter field`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val parametersNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channels")
            .expectObject().required("lightStatus")
            .expectObject().required("parameters")

        val parameters = parser.parseMap(parametersNode)

        val city = assertIs<ParameterInterface.ParameterInline>(parameters["city"]).parameter
        assertEquals("The city where the streetlights are located.", city.description)
        assertEquals("\$message.payload#/city", city.location)
        assertEquals(listOf("helsinki", "oslo", "stockholm"), city.enum)
        assertEquals("helsinki", city.default)
        assertEquals(listOf("helsinki", "oslo"), city.examples)
    }

    @Test
    fun `parse parameter reports invalid inline structure`() {
        val file = TestResources.file("parser/parameters/asyncapi_parser_parameter_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val parametersNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("parameterCases")
            .expectObject().required("InvalidParameterStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(parametersNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_parameter_invalid.root.components.parameterCases.InvalidParameterStructure.badParameter",
            diagnostic.path,
        )
        assertEquals("root.components.parameterCases.InvalidParameterStructure.badParameter", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_parameter_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse parameter reports boolean location`() {
        val file = TestResources.file("parser/parameters/asyncapi_parser_parameter_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val parametersNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("parameterCases")
            .expectObject().required("BooleanLocation")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(parametersNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_parameter_invalid.root.components.parameterCases.BooleanLocation.badParameter.location",
            diagnostic.path,
        )
        assertEquals("root.components.parameterCases.BooleanLocation.badParameter.location", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_parameter_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse parameter reports non-string ref before inline parsing`() {
        val file = TestResources.file("parser/parameters/asyncapi_parser_parameter_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val parametersNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("parameterCases")
            .expectObject().required("NumericReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(parametersNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(42, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_parameter_invalid.root.components.parameterCases.NumericReference.badParameter.\$ref",
            diagnostic.path,
        )
        assertEquals("root.components.parameterCases.NumericReference.badParameter.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_parameter_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse parameter reports invalid enum element at its index`() {
        val file = TestResources.file("parser/parameters/asyncapi_parser_parameter_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val parametersNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("parameterCases")
            .expectObject().required("InvalidEnumElement")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(parametersNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(7, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_parameter_invalid.root.components.parameterCases.InvalidEnumElement.badParameter.enum[1]",
            diagnostic.path,
        )
        assertEquals("root.components.parameterCases.InvalidEnumElement.badParameter.enum[1]", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_parameter_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse parameter reports null example at its index`() {
        val file = TestResources.file("parser/parameters/asyncapi_parser_parameter_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val parametersNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("parameterCases")
            .expectObject().required("InvalidExamplesElement")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(parametersNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_parameter_invalid.root.components.parameterCases.InvalidExamplesElement.badParameter.examples[1]",
            diagnostic.path,
        )
        assertEquals(
            "root.components.parameterCases.InvalidExamplesElement.badParameter.examples[1]",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_parameter_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
