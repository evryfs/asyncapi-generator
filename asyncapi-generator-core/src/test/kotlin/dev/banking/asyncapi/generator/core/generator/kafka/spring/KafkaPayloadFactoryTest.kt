package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SchemaFixtures
import dev.banking.asyncapi.generator.core.fixtures.analyzedMessage
import dev.banking.asyncapi.generator.core.fixtures.nativeMessage
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class KafkaPayloadFactoryTest {
    private val factory = KafkaPayloadFactory()

    @Test
    fun `prepares Java and Kotlin payload types without changing model ownership`() {
        val payloads =
            factory.create(
                channel(
                    messages =
                        listOf(
                            analyzedMessage(
                                messageName = "AccountCreated",
                                payloadTypeName = "AccountCreatedPayload",
                                schema =
                                    Schema(
                                        type = JsonSchemaType.Single("object"),
                                        description = "Created account.",
                                        properties = mapOf("accountId" to SchemaFixtures.inline(type = "string")),
                                    ),
                            ),
                            analyzedMessage(
                                messageName = "RawEvent",
                                payloadTypeName = "RawEventPayload",
                                schema = Schema(),
                            ),
                            analyzedMessage(
                                messageName = "StatusChanged",
                                payloadTypeName = "StatusChangedPayload",
                                schema = Schema(type = JsonSchemaType.Single("string")),
                            ),
                            analyzedMessage(
                                messageName = "BalanceChanged",
                                payloadTypeName = "BalanceChangedPayload",
                                schema = Schema(type = JsonSchemaType.Single("number")),
                            ),
                        ),
                ),
            )

        val accountCreated = payloads[0]
        assertEquals(TypeRef.Named(name = "AccountCreatedPayload", isModel = true), accountCreated.type)
        assertEquals("Created account.", accountCreated.payloadDescription)

        val rawEvent = payloads[1]
        assertEquals(TypeRef.Named(name = "RawEventPayload", isModel = true, isOpenObject = true), rawEvent.type)

        assertEquals(TypeRef.Scalar.STRING, payloads[2].type)
        assertEquals(TypeRef.Scalar.DOUBLE, payloads[3].type)
    }

    @Test
    fun `preserves payloadless message keys and headers`() {
        val keySchema = SchemaFixtures.inline(type = "string")
        val payload =
            factory
                .create(
                    channel(
                        messages =
                            listOf(
                                analyzedMessage(
                                    messageName = "Heartbeat",
                                    payloadTypeName = null,
                                    schema = null,
                                    keySchema = keySchema,
                                    headers =
                                        AnalyzedMessageHeaders(
                                            properties =
                                                mapOf(
                                                    "correlationId" to SchemaFixtures.inline(type = "string"),
                                                ),
                                            requiredProperties = listOf("correlationId"),
                                        ),
                                ),
                            ),
                    ),
                ).single()

        assertFalse(payload.hasPayload)
        assertNull(payload.type)
        assertEquals(keySchema, payload.kafkaKey?.declaredSchema)
        assertEquals(1, payload.headerProperties.size)
        assertEquals("correlationId", payload.headerProperties.single().wireName)
        assertTrue(payload.headerProperties.single().required)
    }

    @Test
    fun `prepares native payload imports and contract headers`() {
        val payload =
            factory
                .create(
                    channel(
                        nativeMessages =
                            listOf(
                                nativeMessage(
                                    messageName = "AccountCreated",
                                    payloadName = "AccountCreated",
                                    schema =
                                        MultiFormatSchema(
                                            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
                                            schema =
                                                mapOf(
                                                    "type" to "record",
                                                    "name" to "AccountCreated",
                                                    "namespace" to "com.example.avro",
                                                    "fields" to emptyList<Any>(),
                                                ),
                                        ),
                                    headers =
                                        AnalyzedMessageHeaders(
                                            properties = mapOf("traceId" to SchemaFixtures.inline(type = "string")),
                                        ),
                                ),
                            ),
                    ),
                ).single()

        assertEquals(TypeRef.External(name = "AccountCreated", packageName = "com.example.avro"), payload.type)
        assertEquals("traceId", payload.headerProperties.single().wireName)
    }

    private fun channel(
        messages: List<AnalyzedMessage> = emptyList(),
        nativeMessages: List<AnalyzedMessage> = emptyList(),
    ): AnalyzedChannel =
        AnalyzedChannel(
            channelName = "accountEvents",
            topic = "account.events",
            messages = messages + nativeMessages,
        )

    @Test
    fun `uses the model types for array and scalar payloads`() {
        val payloads =
            factory.create(
                channel(
                    messages =
                        listOf(
                            analyzedMessage(
                                messageName = "OrderBatch",
                                payloadTypeName = "OrderBatchPayload",
                                schema =
                                    Schema(
                                        type = JsonSchemaType.Single("array"),
                                        items =
                                            SchemaInterface.SchemaReference(
                                                Reference("#/components/schemas/Order"),
                                            ),
                                    ),
                            ),
                            analyzedMessage(
                                messageName = "Counter",
                                payloadTypeName = "CounterPayload",
                                schema = Schema(type = JsonSchemaType.Single("integer"), format = "int64"),
                            ),
                            analyzedMessage(
                                messageName = "IdAssigned",
                                payloadTypeName = "IdAssignedPayload",
                                schema = Schema(type = JsonSchemaType.Single("string"), format = "uuid"),
                            ),
                        ),
                ),
            )

        assertEquals(TypeRef.ListOf(TypeRef.Named(name = "Order", isModel = true)), payloads[0].type)
        assertEquals(TypeRef.Scalar.INT64, payloads[1].type)
        assertEquals(TypeRef.Scalar.UUID, payloads[2].type)
    }
}
