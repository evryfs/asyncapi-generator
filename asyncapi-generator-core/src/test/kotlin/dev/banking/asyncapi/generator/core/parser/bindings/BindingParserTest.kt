package dev.banking.asyncapi.generator.core.parser.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.CHANNEL
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.MESSAGE
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.OPERATION
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.SERVER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BindingParserTest {

    private val context = AsyncApiContext()
    private val parser = BindingParser(context)

    @Test
    fun `parse channel bindings with nested values and references`() {
        val file = TestResources.file("parser/bindings/asyncapi_parser_bindings_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val channelBindingsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("channelBindings")

        val bindings = parser.parseComponentMap(channelBindingsNode, CHANNEL)

        val kafka = assertIs<BindingInterface.BindingInline>(bindings["userSignedUpChannel"]).binding
        assertEquals("kafka", kafka.protocolBindings.single().protocol)
        assertEquals(CHANNEL, kafka.protocolBindings.single().location)
        assertEquals("0.5.0", kafka.protocolBindings.single().bindingVersion)
        assertEquals(
            mapOf(
                "kafka" to mapOf(
                    "topic" to "my-specific-topic-name",
                    "partitions" to 20,
                    "replicas" to 3,
                    "topicConfiguration" to mapOf(
                        "cleanup.policy" to listOf("delete", "compact"),
                        "retention.ms" to 604800000,
                        "retention.bytes" to 1000000000,
                        "delete.retention.ms" to 86400000,
                        "max.message.bytes" to 1048588,
                    ),
                    "bindingVersion" to "0.5.0",
                ),
            ),
            kafka.content,
        )

        val plain = assertIs<BindingInterface.BindingInline>(bindings["plainChannel"]).binding
        assertEquals(
            mapOf(
                "custom" to mapOf(
                    "enabled" to true,
                    "attempts" to 3,
                    "values" to listOf("primary", 7, false, null),
                    "metadata" to mapOf("nullable" to null),
                ),
            ),
            plain.content,
        )

        val reference = assertIs<BindingInterface.BindingReference>(bindings["referencedChannel"]).reference
        assertEquals("#/components/channelBindings/userSignedUpChannel", reference.ref)
        assertEquals(BINDING, reference.referenceCategoryKey)
    }

    @Test
    fun `parse message bindings including Kafka key schema`() {
        val file = TestResources.file("parser/bindings/asyncapi_parser_bindings_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val messageBindingsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageBindings")

        val bindings = parser.parseComponentMap(messageBindingsNode, MESSAGE)

        val amqp = assertIs<BindingInterface.BindingInline>(bindings["userSignedUpMessage"]).binding
        assertEquals(
            mapOf(
                "amqp" to mapOf(
                    "contentEncoding" to "gzip",
                    "messageType" to "user.signup",
                ),
            ),
            amqp.content,
        )

        val kafka = assertIs<BindingInterface.BindingInline>(bindings["accountUpdatedMessage"]).binding
        assertEquals(MESSAGE, kafka.protocolBindings.single().location)
        assertEquals(setOf("key"), kafka.protocolBindings.single().schemaFields.keys)
        val keySchema = assertIs<SchemaInterface.SchemaInline>(kafka.kafkaKeySchema).schema
        assertEquals("integer", keySchema.type)
        assertEquals("int64", keySchema.format)
        assertEquals("Account identifier used as the Kafka record key.", keySchema.description)
    }

    @Test
    fun `parses a direct Kafka binding key schema`() {
        val file = TestResources.file("parser/messages/asyncapi_parser_message_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val kafkaBindingNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("messageTraits")
            .expectObject().required("commonHeaders")
            .expectObject().required("bindings")
            .expectObject().required("kafka")

        val binding = assertIs<BindingInterface.BindingInline>(parser.parseProtocol(kafkaBindingNode, MESSAGE)).binding

        assertEquals("string", assertIs<SchemaInterface.SchemaInline>(binding.kafkaKeySchema).schema.type)
    }

    @Test
    fun `parse server and operation bindings`() {
        val file = TestResources.file("parser/bindings/asyncapi_parser_bindings_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val components = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject()

        val serverBindings = parser.parseComponentMap(components.required("serverBindings"), SERVER)
        val mqtt = assertIs<BindingInterface.BindingInline>(serverBindings["myServerBinding"]).binding
        assertEquals(
            mapOf("mqtt" to mapOf("clientId" to "guest", "cleanSession" to true)),
            mqtt.content,
        )

        val operationBindings = parser.parseComponentMap(components.required("operationBindings"), OPERATION)
        val http = assertIs<BindingInterface.BindingInline>(operationBindings["myOperationBinding"]).binding
        assertEquals(
            mapOf("http" to mapOf("method" to "POST", "query" to mapOf("type" to "object"))),
            http.content,
        )
    }

}
