package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerVariableParserTest : ParserTestSupport() {

    private val parser = ServerVariableParser(asyncApiContext)

    @Test
    fun `parse server variables`() {
        val variablesNode = readNode(
            "parser/servers/asyncapi_parser_servers_valid.yaml",
            "servers",
            "scram-connections",
            "variables",
        )

        val variables = parser.parseMap(variablesNode)

        assertTrue(variables["port"] is ServerVariableInterface.ServerVariableInline)
        val port = (variables["port"] as ServerVariableInterface.ServerVariableInline).serverVariable
        assertEquals(listOf("18092", "28092"), port.enum)
        assertEquals("18092", port.default)
        assertEquals("The port used for Kafka connections", port.description)
    }

    @Test
    fun `parse server variable with invalid structure reports its expected type and source`() {
        val variablesNode = readNode(
            "parser/servers/asyncapi_parser_server_variable_invalid.yaml",
            "components",
            "serverVariableCases",
            "InvalidVariableStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.InvalidVariableStructure.badVariable",
            sourcePath = "root.components.serverVariableCases.InvalidVariableStructure.badVariable",
            sourceFile = "asyncapi_parser_server_variable_invalid.yaml",
        ) {
            parser.parseMap(variablesNode)
        }
    }

    @Test
    fun `parse server variable with boolean default reports its expected type and source`() {
        val variablesNode = readNode(
            "parser/servers/asyncapi_parser_server_variable_invalid.yaml",
            "components",
            "serverVariableCases",
            "BooleanDefault",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = false,
            path = "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.BooleanDefault.badVariable.default",
            sourcePath = "root.components.serverVariableCases.BooleanDefault.badVariable.default",
            sourceFile = "asyncapi_parser_server_variable_invalid.yaml",
        ) {
            parser.parseMap(variablesNode)
        }
    }

    @Test
    fun `parse server variable with numeric reference reports its expected type and source`() {
        val variablesNode = readNode(
            "parser/servers/asyncapi_parser_server_variable_invalid.yaml",
            "components",
            "serverVariableCases",
            "NumericReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 42,
            path = "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.NumericReference.badVariable.\$ref",
            sourcePath = "root.components.serverVariableCases.NumericReference.badVariable.\$ref",
            sourceFile = "asyncapi_parser_server_variable_invalid.yaml",
        ) {
            parser.parseMap(variablesNode)
        }
    }

    @Test
    fun `parse server variable with numeric enum entry reports the nested value and source`() {
        val variablesNode = readNode(
            "parser/servers/asyncapi_parser_server_variable_invalid.yaml",
            "components",
            "serverVariableCases",
            "InvalidEnumElement",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 7,
            path = "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.InvalidEnumElement.badVariable.enum[1]",
            sourcePath = "root.components.serverVariableCases.InvalidEnumElement.badVariable.enum[1]",
            sourceFile = "asyncapi_parser_server_variable_invalid.yaml",
        ) {
            parser.parseMap(variablesNode)
        }
    }
}
