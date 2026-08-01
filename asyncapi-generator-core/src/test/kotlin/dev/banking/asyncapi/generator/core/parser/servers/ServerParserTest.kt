package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerParserTest : ParserTestSupport() {

    private val parser = ServerParser(asyncApiContext)

    @Test
    fun parseServers_validate_data_classes() {
        val serversNode = readNode("parser/servers/asyncapi_parser_servers_valid.yaml", "servers")
        val result = parser.parseMap(serversNode)

        assertTrue("scram-connections" in result)
        assertTrue("mtls-connections" in result)
        assertTrue("staging" in result)

        val scramConnections = (result["scram-connections"] as ServerInterface.ServerInline).server
        val expectedScramConnections = scramConnections()
        assertThat(scramConnections)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedScramConnections)

        val mtlsConnections = (result["mtls-connections"] as ServerInterface.ServerInline).server
        val expectedMtlsConnections = mtlsConnections()
        assertThat(mtlsConnections)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedMtlsConnections)

        val staging = (result["staging"] as ServerInterface.ServerReference).reference
        val expectedStaging = stagingReference()
        assertThat(staging)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedStaging)
    }

    @Test
    fun `parse server with invalid variables structure reports its expected type and source`() {
        val serversNode = readNode("parser/servers/asyncapi_parser_server_invalid.yaml", "servers")
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_server_invalid.root.servers.InvalidVariables.variables",
            sourcePath = "root.servers.InvalidVariables.variables",
            sourceFile = "asyncapi_parser_server_invalid.yaml",
        ) {
            parser.parseMap(serversNode)
        }
    }

    @Test
    fun `parse server with null reference reports the reference type and source`() {
        val serversNode = readNode(
            "parser/servers/asyncapi_parser_server_invalid.yaml",
            "serverCases",
            "NullReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_server_invalid.root.serverCases.NullReference.invalidReference.\$ref",
            sourcePath = "root.serverCases.NullReference.invalidReference.\$ref",
            sourceFile = "asyncapi_parser_server_invalid.yaml",
        ) {
            parser.parseMap(serversNode)
        }
    }

    @Test
    fun `parse server missing host reports the required member and source`() {
        val serversNode = readNode(
            "parser/servers/asyncapi_parser_server_invalid.yaml",
            "serverCases",
            "MissingHost",
        )
        assertMissingRequiredMember(
            memberName = "host",
            path = "asyncapi_parser_server_invalid.root.serverCases.MissingHost.missingHost.host",
            sourcePath = "root.serverCases.MissingHost.missingHost",
            sourceFile = "asyncapi_parser_server_invalid.yaml",
        ) {
            parser.parseMap(serversNode)
        }
    }

    @Test
    fun `parse server map from an array reports the container type and source`() {
        val serversNode = readNode(
            "parser/servers/asyncapi_parser_server_invalid.yaml",
            "serverCases",
            "ArrayInsteadOfMap",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("host" to "localhost", "protocol" to "kafka")),
            path = "asyncapi_parser_server_invalid.root.serverCases.ArrayInsteadOfMap",
            sourcePath = "root.serverCases.ArrayInsteadOfMap",
            sourceFile = "asyncapi_parser_server_invalid.yaml",
        ) {
            parser.parseMap(serversNode)
        }
    }
}
