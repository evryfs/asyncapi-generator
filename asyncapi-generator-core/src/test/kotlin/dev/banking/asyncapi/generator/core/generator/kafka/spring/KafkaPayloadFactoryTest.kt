package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SchemaFixtures
import dev.banking.asyncapi.generator.core.fixtures.analyzedMessage
import dev.banking.asyncapi.generator.core.fixtures.nativeMessage
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KafkaPayloadFactoryTest {
    private val factory = KafkaPayloadFactory(modelPackage = "com.example.model")

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
                                        type = "object",
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
                                schema = Schema(type = "string"),
                            ),
                            analyzedMessage(
                                messageName = "BalanceChanged",
                                payloadTypeName = "BalanceChangedPayload",
                                schema = Schema(type = "number"),
                            ),
                        ),
                ),
            )

        val accountCreated = payloads[0]
        assertEquals("AccountCreatedPayload", accountCreated.javaTypeName)
        assertEquals("AccountCreatedPayload", accountCreated.kotlinTypeName)
        assertEquals(listOf("com.example.model.AccountCreatedPayload"), accountCreated.javaImports)
        assertEquals(listOf("com.example.model.AccountCreatedPayload"), accountCreated.kotlinImports)
        assertEquals("Created account.", accountCreated.payloadDescription)

        val rawEvent = payloads[1]
        assertEquals("Object", rawEvent.javaTypeName)
        assertEquals("RawEventPayload", rawEvent.kotlinTypeName)
        assertEquals(emptyList(), rawEvent.javaImports)
        assertEquals(listOf("com.example.model.RawEventPayload"), rawEvent.kotlinImports)

        val statusChanged = payloads[2]
        assertEquals("String", statusChanged.javaTypeName)
        assertEquals("String", statusChanged.kotlinTypeName)
        assertEquals(emptyList(), statusChanged.javaImports)
        assertEquals(emptyList(), statusChanged.kotlinImports)

        val balanceChanged = payloads[3]
        assertEquals("Double", balanceChanged.javaTypeName)
        assertEquals("Double", balanceChanged.kotlinTypeName)
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
        assertNull(payload.javaTypeName)
        assertNull(payload.kotlinTypeName)
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

        assertEquals("AccountCreated", payload.javaTypeName)
        assertEquals("AccountCreated", payload.kotlinTypeName)
        assertEquals("com.example.avro.AccountCreated", payload.javaImportName)
        assertEquals("com.example.avro.AccountCreated", payload.kotlinImportName)
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
                                        type = "array",
                                        items =
                                            SchemaInterface.SchemaReference(
                                                Reference("#/components/schemas/Order"),
                                            ),
                                    ),
                            ),
                            analyzedMessage(
                                messageName = "Counter",
                                payloadTypeName = "CounterPayload",
                                schema = Schema(type = "integer", format = "int64"),
                            ),
                            analyzedMessage(
                                messageName = "IdAssigned",
                                payloadTypeName = "IdAssignedPayload",
                                schema = Schema(type = "string", format = "uuid"),
                            ),
                        ),
                ),
            )

        val batch = payloads[0]
        assertEquals("List<Order>", batch.javaTypeName)
        assertEquals("List<Order>", batch.kotlinTypeName)
        assertTrue("com.example.model.Order" in batch.kotlinImports)
        assertTrue("java.util.List" in batch.javaImports)
        assertEquals("Long", payloads[1].javaTypeName)
        assertEquals("Long", payloads[1].kotlinTypeName)
        assertEquals("UUID", payloads[2].javaTypeName)
        assertEquals(listOf("java.util.UUID"), payloads[2].kotlinImports)
    }
}
