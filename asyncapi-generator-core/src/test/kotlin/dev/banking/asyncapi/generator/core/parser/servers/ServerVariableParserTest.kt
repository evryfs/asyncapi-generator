package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ServerVariableParserTest {

    private val context = AsyncApiContext()
    private val parser = ServerVariableParser(context)

    @Test
    fun `parse server variables`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_servers_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("servers")
            .expectObject().required("scram-connections")
            .expectObject().required("variables")

        val variables = parser.parseMap(variablesNode)

        val port = assertIs<ServerVariableInterface.ServerVariableInline>(variables["port"]).serverVariable
        assertEquals(listOf("18092", "28092"), port.enum)
        assertEquals("18092", port.default)
        assertEquals("The port used for Kafka connections", port.description)
        assertEquals(listOf("18092", "28092"), port.examples)
    }

    @Test
    fun `parses a referenced server variable with its concrete category`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_servers_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("serverVariables")

        val variables = parser.parseMap(variablesNode)

        val reference = assertIs<ServerVariableInterface.ServerVariableReference>(variables["referencedPort"])
            .reference
        assertEquals("#/components/serverVariables/sharedPort", reference.ref)
        assertEquals(SERVER_VARIABLE, reference.referenceCategoryKey)
    }

    @Test
    fun `parse server variable with invalid structure reports its expected type and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_variable_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("serverVariableCases")
            .expectObject().required("InvalidVariableStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(variablesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.InvalidVariableStructure.badVariable",
            diagnostic.path,
        )
        assertEquals(
            "root.components.serverVariableCases.InvalidVariableStructure.badVariable",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_server_variable_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse server variable with boolean default reports its expected type and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_variable_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("serverVariableCases")
            .expectObject().required("BooleanDefault")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(variablesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(false, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.BooleanDefault.badVariable.default",
            diagnostic.path,
        )
        assertEquals(
            "root.components.serverVariableCases.BooleanDefault.badVariable.default",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_server_variable_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse server variable with numeric reference reports its expected type and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_variable_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("serverVariableCases")
            .expectObject().required("NumericReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(variablesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(42, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.NumericReference.badVariable.\$ref",
            diagnostic.path,
        )
        assertEquals(
            "root.components.serverVariableCases.NumericReference.badVariable.\$ref",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_server_variable_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse server variable with numeric enum entry reports the nested value and source`() {
        val file = TestResources.file("parser/servers/asyncapi_parser_server_variable_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val variablesNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("serverVariableCases")
            .expectObject().required("InvalidEnumElement")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(variablesNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(7, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.InvalidEnumElement.badVariable.enum[1]",
            diagnostic.path,
        )
        assertEquals(
            "root.components.serverVariableCases.InvalidEnumElement.badVariable.enum[1]",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_server_variable_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
