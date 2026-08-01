package dev.banking.asyncapi.generator.core.parser.channels

import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChannelParserTest : ParserTestSupport() {

    private val parser = ChannelParser(asyncApiContext)

    @Test
    fun `parse lightingMeasured channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("lightingMeasured" in result)
        val lightingMeasured = (result["lightingMeasured"] as ChannelInterface.ChannelInline).channel
        val expectedLightingMeasured = lightingMeasured()
        assertThat(lightingMeasured)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedLightingMeasured)
    }

    @Test
    fun `parse lightTurnOn channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("lightTurnOn" in result)
        val lightTurnOn = (result["lightTurnOn"] as ChannelInterface.ChannelInline).channel
        val expectedLightTurnOn = lightTurnOn()
        assertThat(lightTurnOn)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedLightTurnOn)
    }

    @Test
    fun `parse lightTurnOff channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("lightTurnOff" in result)
        val lightTurnOff = (result["lightTurnOff"] as ChannelInterface.ChannelInline).channel
        val expectedLightTurnOff = lightTurnOff()
        assertThat(lightTurnOff)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedLightTurnOff)
    }

    @Test
    fun `parse lightsDim channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("lightsDim" in result)
        val lightsDim = (result["lightsDim"] as ChannelInterface.ChannelInline).channel
        val expectedLightsDim = lightsDim()
        assertThat(lightsDim)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedLightsDim)
    }

    @Test
    fun `parse lightStatus channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("lightStatus" in result)
        val lightStatus = (result["lightStatus"] as ChannelInterface.ChannelInline).channel
        val expectedLightStatus = lightStatus()
        assertThat(lightStatus)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedLightStatus)
    }

    @Test
    fun `parse maintenanceRequest channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("maintenanceRequest" in result)
        val maintenanceRequest = (result["maintenanceRequest"] as ChannelInterface.ChannelInline).channel
        val expectedMaintenanceRequest = maintenanceRequest()
        assertThat(maintenanceRequest)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedMaintenanceRequest)
    }

    @Test
    fun `parse cityLights channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("cityLights" in result)
        val cityLights = (result["cityLights"] as ChannelInterface.ChannelInline).channel
        val expectedCityLights = cityLights()
        assertThat(cityLights)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedCityLights)
    }

    @Test
    fun `parse powerStatus channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)
        assertTrue("powerStatus" in result)
        val powerStatus = (result["powerStatus"] as ChannelInterface.ChannelInline).channel
        val expectedPowerStatus = powerStatus()
        assertThat(powerStatus)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedPowerStatus)
    }

    @Test
    fun `parse referenced channel`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_valid.yaml", "channels")
        val result = parser.parseMap(channelsNode)

        val reference = (result["referencedChannel"] as ChannelInterface.ChannelReference).reference
        assertThat(reference.ref).isEqualTo("#/channels/lightingMeasured")
    }

    @Test
    fun `parse channel with invalid messages structure reports its expected type and source`() {
        val channelsNode = readNode("parser/channels/asyncapi_parser_channel_invalid.yaml", "channels")
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_channel_invalid.root.channels.InvalidMessages.messages",
            sourcePath = "root.channels.InvalidMessages.messages",
            sourceFile = "asyncapi_parser_channel_invalid.yaml",
        ) {
            parser.parseMap(channelsNode)
        }
    }

    @Test
    fun `parse channel with list shaped messages reports its expected type and source`() {
        val channelNode = readNode(
            "parser/channels/asyncapi_parser_channel_invalid.yaml",
            "channelCases",
            "ListMessages",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("\$ref" to "#/components/messages/Message")),
            path = "asyncapi_parser_channel_invalid.root.channelCases.ListMessages.invalidChannel.messages",
            sourcePath = "root.channelCases.ListMessages.invalidChannel.messages",
            sourceFile = "asyncapi_parser_channel_invalid.yaml",
        ) {
            parser.parseMap(channelNode)
        }
    }

    @Test
    fun `parse channel with boolean address reports its expected type and source`() {
        val channelsNode = readNode(
            "parser/channels/asyncapi_parser_channel_invalid.yaml",
            "channelCases",
            "InvalidAddress",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = false,
            path = "asyncapi_parser_channel_invalid.root.channelCases.InvalidAddress.invalidChannel.address",
            sourcePath = "root.channelCases.InvalidAddress.invalidChannel.address",
            sourceFile = "asyncapi_parser_channel_invalid.yaml",
        ) {
            parser.parseMap(channelsNode)
        }
    }

    @Test
    fun `parse channel with null reference reports its expected type and source`() {
        val channelsNode = readNode(
            "parser/channels/asyncapi_parser_channel_invalid.yaml",
            "channelCases",
            "NullReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_channel_invalid.root.channelCases.NullReference.invalidChannel.\$ref",
            sourcePath = "root.channelCases.NullReference.invalidChannel.\$ref",
            sourceFile = "asyncapi_parser_channel_invalid.yaml",
        ) {
            parser.parseMap(channelsNode)
        }
    }

    @Test
    fun `parse scalar channel reports the entry type and source`() {
        val channelsNode = readNode(
            "parser/channels/asyncapi_parser_channel_invalid.yaml",
            "channelCases",
            "InvalidChannelStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_channel_invalid.root.channelCases.InvalidChannelStructure.invalidChannel",
            sourcePath = "root.channelCases.InvalidChannelStructure.invalidChannel",
            sourceFile = "asyncapi_parser_channel_invalid.yaml",
        ) {
            parser.parseMap(channelsNode)
        }
    }

    @Test
    fun `parse channel map from an array reports the container type and source`() {
        val channelsNode = readNode(
            "parser/channels/asyncapi_parser_channel_invalid.yaml",
            "channelCases",
            "ArrayInsteadOfMap",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("address" to "valid-address")),
            path = "asyncapi_parser_channel_invalid.root.channelCases.ArrayInsteadOfMap",
            sourcePath = "root.channelCases.ArrayInsteadOfMap",
            sourceFile = "asyncapi_parser_channel_invalid.yaml",
        ) {
            parser.parseMap(channelsNode)
        }
    }
}
