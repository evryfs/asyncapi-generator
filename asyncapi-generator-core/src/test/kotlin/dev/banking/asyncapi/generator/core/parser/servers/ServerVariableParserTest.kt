package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
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
    fun `parse server variable with invalid structure throws UnexpectedValue`() {
        val variablesNode = readNode(
            "parser/servers/asyncapi_parser_server_variable_invalid.yaml",
            "components",
            "serverVariableCases",
            "InvalidVariableStructure",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_server_variable_invalid.yaml",
            "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.InvalidVariableStructure.badVariable",
        ) {
            parser.parseMap(variablesNode)
        }
    }

    @Test
    fun `parse server variable with boolean default throws UnexpectedValue`() {
        val variablesNode = readNode(
            "parser/servers/asyncapi_parser_server_variable_invalid.yaml",
            "components",
            "serverVariableCases",
            "BooleanDefault",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected String",
            "found Boolean false",
            "quote the value",
            "asyncapi_parser_server_variable_invalid.yaml",
            "asyncapi_parser_server_variable_invalid.root.components.serverVariableCases.BooleanDefault.badVariable.default",
        ) {
            parser.parseMap(variablesNode)
        }
    }
}
