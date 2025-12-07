package com.tietoevry.banking.asyncapi.generator.core.parser.servers

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ServerParserTest : AbstractParserTest() {

    private val parser = ServerParser(asyncApiContext)

    @Test
    fun parseServers_validate_data_classes() {
        val root = readYaml("src/test/resources/parser/servers/asyncapi_parser_servers_valid.yaml")
        val result = parser.parseMap(root.mandatory("servers"))

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
        val root = readYaml("src/test/resources/parser/servers/asyncapi_parser_server_invalid.yaml")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseMap(root.mandatory("servers"))
        }
    }
}
