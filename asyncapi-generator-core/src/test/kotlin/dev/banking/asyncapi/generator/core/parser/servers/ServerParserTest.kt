package dev.banking.asyncapi.generator.core.parser.servers

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
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
    fun `parse server with invalid variables structure throws UnexpectedValue`() {
        val serversNode = readNode("parser/servers/asyncapi_parser_server_invalid.yaml", "servers")
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map/List",
            "asyncapi_parser_server_invalid.yaml",
            "asyncapi_parser_server_invalid.root.servers.InvalidVariables.variables",
        ) {
            parser.parseMap(serversNode)
        }
    }
}
