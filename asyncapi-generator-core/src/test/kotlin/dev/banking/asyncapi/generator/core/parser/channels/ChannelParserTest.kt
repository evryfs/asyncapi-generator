package dev.banking.asyncapi.generator.core.parser.channels

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChannelParserTest {

    private val context = AsyncApiContext()
    private val parser = ChannelParser(context)

    @Test
    fun `parses channels with references and all optional metadata`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channels")

        val result = parser.parseMap(channelsNode)

        val lightingMeasured = assertIs<ChannelInterface.ChannelInline>(result["lightingMeasured"]).channel
        assertEquals(
            "smartylighting.streetlights.1.0.event.{streetlightId}.lighting.measured",
            lightingMeasured.address,
        )
        assertEquals(
            "The topic on which measured values may be produced and consumed.",
            lightingMeasured.description,
        )
        assertEquals("Lighting measurements", lightingMeasured.title)
        assertEquals("Reports measured lighting values", lightingMeasured.summary)
        val server = lightingMeasured.servers?.single()
        assertEquals("#/components/servers/production", server?.ref)
        assertEquals(SERVER, server?.referenceCategoryKey)
        val tag = assertIs<TagInterface.TagInline>(lightingMeasured.tags?.single()).tag
        assertEquals("telemetry", tag.name)
        val externalDocs =
            assertIs<ExternalDocInterface.ExternalDocInline>(lightingMeasured.externalDocs).externalDoc
        assertEquals("https://example.com/docs/channel", externalDocs.url)
        val lightMeasuredMessage =
            assertIs<MessageInterface.MessageReference>(lightingMeasured.messages?.get("lightMeasured")).reference
        assertEquals("#/components/messages/lightMeasured", lightMeasuredMessage.ref)
        assertEquals(MESSAGE, lightMeasuredMessage.referenceCategoryKey)
        val lightingStreetlight =
            assertIs<ParameterInterface.ParameterReference>(lightingMeasured.parameters?.get("streetlightId"))
                .reference
        assertEquals("#/components/parameters/streetlightId", lightingStreetlight.ref)
        assertEquals(PARAMETER, lightingStreetlight.referenceCategoryKey)
        val kafkaBinding =
            assertIs<BindingInterface.BindingInline>(lightingMeasured.bindings?.get("kafka")).binding
        assertEquals(
            mapOf(
                "topic" to "smartylighting.streetlights.1.0.event",
                "partitions" to 3,
                "replicas" to 1,
            ),
            kafkaBinding.content,
        )

        val lightTurnOn = assertIs<ChannelInterface.ChannelInline>(result["lightTurnOn"]).channel
        assertEquals("smartylighting.streetlights.1.0.action.{streetlightId}.turn.on", lightTurnOn.address)
        val turnOnMessage =
            assertIs<MessageInterface.MessageReference>(lightTurnOn.messages?.get("turnOn")).reference
        assertEquals("#/components/messages/turnOnOff", turnOnMessage.ref)
        assertEquals(MESSAGE, turnOnMessage.referenceCategoryKey)
        val turnOnStreetlight =
            assertIs<ParameterInterface.ParameterReference>(lightTurnOn.parameters?.get("streetlightId"))
                .reference
        assertEquals("#/components/parameters/streetlightId", turnOnStreetlight.ref)
        assertEquals(PARAMETER, turnOnStreetlight.referenceCategoryKey)

        val lightTurnOff = assertIs<ChannelInterface.ChannelInline>(result["lightTurnOff"]).channel
        assertEquals("smartylighting.streetlights.1.0.action.{streetlightId}.turn.off", lightTurnOff.address)
        val turnOffMessage =
            assertIs<MessageInterface.MessageReference>(lightTurnOff.messages?.get("turnOff")).reference
        assertEquals("#/components/messages/turnOnOff", turnOffMessage.ref)
        assertEquals(MESSAGE, turnOffMessage.referenceCategoryKey)
        val turnOffStreetlight =
            assertIs<ParameterInterface.ParameterReference>(lightTurnOff.parameters?.get("streetlightId"))
                .reference
        assertEquals("#/components/parameters/streetlightId", turnOffStreetlight.ref)
        assertEquals(PARAMETER, turnOffStreetlight.referenceCategoryKey)

        val lightsDim = assertIs<ChannelInterface.ChannelInline>(result["lightsDim"]).channel
        assertEquals("smartylighting.streetlights.1.0.action.{streetlightId}.dim", lightsDim.address)
        val dimMessage = assertIs<MessageInterface.MessageReference>(lightsDim.messages?.get("dimLight")).reference
        assertEquals("#/components/messages/dimLight", dimMessage.ref)
        assertEquals(MESSAGE, dimMessage.referenceCategoryKey)
        val dimStreetlight =
            assertIs<ParameterInterface.ParameterReference>(lightsDim.parameters?.get("streetlightId"))
                .reference
        assertEquals("#/components/parameters/streetlightId", dimStreetlight.ref)
        assertEquals(PARAMETER, dimStreetlight.referenceCategoryKey)
    }

    @Test
    fun `parse channels with inline parameters`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channels")

        val result = parser.parseMap(channelsNode)

        val lightStatus = assertIs<ChannelInterface.ChannelInline>(result["lightStatus"]).channel
        assertEquals("smartylighting.streetlights.1.0.event.{city}.status", lightStatus.address)
        assertEquals("The topic reporting light status by city.", lightStatus.description)
        val lightStatusMessage =
            assertIs<MessageInterface.MessageReference>(lightStatus.messages?.get("lightStatusMessage")).reference
        assertEquals("#/components/messages/lightMeasured", lightStatusMessage.ref)
        val city = assertIs<ParameterInterface.ParameterInline>(lightStatus.parameters?.get("city")).parameter
        assertEquals("The city where the streetlights are located.", city.description)
        assertEquals("\$message.payload#/city", city.location)
        assertEquals(listOf("helsinki", "oslo", "stockholm"), city.enum)
        assertEquals("helsinki", city.default)
        assertEquals(listOf("helsinki", "oslo"), city.examples)

        val maintenance = assertIs<ChannelInterface.ChannelInline>(result["maintenanceRequest"]).channel
        assertEquals(
            "smartylighting.streetlights.1.0.action.{requestId}.maintenance",
            maintenance.address,
        )
        assertEquals("Command topic for maintenance requests.", maintenance.description)
        val maintenanceMessage =
            assertIs<MessageInterface.MessageReference>(maintenance.messages?.get("maintenanceMessage")).reference
        assertEquals("#/components/messages/turnOnOff", maintenanceMessage.ref)
        val requestId =
            assertIs<ParameterInterface.ParameterInline>(maintenance.parameters?.get("requestId")).parameter
        assertEquals("Identifier for maintenance request.", requestId.description)
        assertEquals("req-001", requestId.default)
        assertEquals("\$message.header#/requestId", requestId.location)

        val cityLights = assertIs<ChannelInterface.ChannelInline>(result["cityLights"]).channel
        assertEquals("smartylighting.streetlights.1.0.{cityId}.light.{lightId}", cityLights.address)
        assertEquals("Channel for controlling individual lights in a city.", cityLights.description)
        val cityId = assertIs<ParameterInterface.ParameterReference>(cityLights.parameters?.get("cityId")).reference
        assertEquals("#/components/parameters/cityId", cityId.ref)
        assertEquals(PARAMETER, cityId.referenceCategoryKey)
        val lightId = assertIs<ParameterInterface.ParameterInline>(cityLights.parameters?.get("lightId")).parameter
        assertEquals("Identifier of the specific light.", lightId.description)
        assertEquals(listOf("lamp-001", "lamp-002", "lamp-003"), lightId.enum)
        assertEquals(listOf("lamp-001", "lamp-002"), lightId.examples)
        assertEquals("\$message.header#/lightId", lightId.location)

        val powerStatus = assertIs<ChannelInterface.ChannelInline>(result["powerStatus"]).channel
        assertEquals("smartylighting.streetlights.1.0.power.{streetlightId}.status", powerStatus.address)
        assertEquals("Channel for power status updates.", powerStatus.description)
        val streetlightId =
            assertIs<ParameterInterface.ParameterInline>(powerStatus.parameters?.get("streetlightId")).parameter
        assertEquals("Identifier for the streetlight.", streetlightId.description)
        assertEquals("\$message.header#/streetlightId", streetlightId.location)
    }

    @Test
    fun `parse referenced channel`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channels")

        val result = parser.parseMap(channelsNode)

        val reference = assertIs<ChannelInterface.ChannelReference>(result["referencedChannel"]).reference
        assertEquals("#/channels/lightingMeasured", reference.ref)
    }

    @Test
    fun `parse channel with invalid messages structure reports its expected type and source`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channels")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(channelsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals("asyncapi_parser_channel_invalid.root.channels.InvalidMessages.messages", diagnostic.path)
        assertEquals("root.channels.InvalidMessages.messages", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_channel_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse channel with list shaped messages reports its expected type and source`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channelCases")
            .expectObject().required("ListMessages")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(channelNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(listOf(mapOf("\$ref" to "#/components/messages/Message")), diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_channel_invalid.root.channelCases.ListMessages.invalidChannel.messages",
            diagnostic.path,
        )
        assertEquals("root.channelCases.ListMessages.invalidChannel.messages", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_channel_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse channel with boolean address reports its expected type and source`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channelCases")
            .expectObject().required("InvalidAddress")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(channelsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(false, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_channel_invalid.root.channelCases.InvalidAddress.invalidChannel.address",
            diagnostic.path,
        )
        assertEquals("root.channelCases.InvalidAddress.invalidChannel.address", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_channel_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse channel with null reference reports its expected type and source`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channelCases")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(channelsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_channel_invalid.root.channelCases.NullReference.invalidChannel.\$ref",
            diagnostic.path,
        )
        assertEquals("root.channelCases.NullReference.invalidChannel.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_channel_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse scalar channel reports the entry type and source`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channelCases")
            .expectObject().required("InvalidChannelStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(channelsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_channel_invalid.root.channelCases.InvalidChannelStructure.invalidChannel",
            diagnostic.path,
        )
        assertEquals("root.channelCases.InvalidChannelStructure.invalidChannel", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_channel_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse channel map from an array reports the container type and source`() {
        val file = TestResources.file("parser/channels/asyncapi_parser_channel_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("channelCases")
            .expectObject().required("ArrayInsteadOfMap")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(channelsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(listOf(mapOf("address" to "valid-address")), diagnostic.actualValue)
        assertEquals("asyncapi_parser_channel_invalid.root.channelCases.ArrayInsteadOfMap", diagnostic.path)
        assertEquals("root.channelCases.ArrayInsteadOfMap", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_channel_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
