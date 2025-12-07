package com.tietoevry.banking.asyncapi.generator.core.parser.channels

import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ChannelParserTest : AbstractParserTest() {

    private val parser = ChannelParser(asyncApiContext)

    @Test
    fun `parse lightingMeasured channel`() {
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
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
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
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
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
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
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
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
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
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
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
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
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
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
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_valid.yaml")
        val result = parser.parseMap(root.mandatory("channels"))
        assertTrue("powerStatus" in result)
        val powerStatus = (result["powerStatus"] as ChannelInterface.ChannelInline).channel
        val expectedPowerStatus = powerStatus()
        assertThat(powerStatus)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedPowerStatus)
    }

    @Test
    fun `parse channel with invalid messages structure throws UnexpectedValue`() {
        val root = readYaml("src/test/resources/parser/channels/asyncapi_parser_channel_invalid.yaml")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseMap(root.mandatory("channels"))
        }
    }
}
