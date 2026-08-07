package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SchemaFixtures
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMultiFormatMessage
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
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
                            AnalyzedMessage(
                                messageName = "AccountCreated",
                                payloadTypeName = "AccountCreatedPayload",
                                schema =
                                    Schema(
                                        type = "object",
                                        description = "Created account.",
                                        properties = mapOf("accountId" to SchemaFixtures.inline(type = "string")),
                                    ),
                            ),
                            AnalyzedMessage(
                                messageName = "RawEvent",
                                payloadTypeName = "RawEventPayload",
                                schema = Schema(),
                            ),
                            AnalyzedMessage(
                                messageName = "StatusChanged",
                                payloadTypeName = "StatusChangedPayload",
                                schema = Schema(type = "string"),
                            ),
                            AnalyzedMessage(
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
        assertEquals("com.example.model.AccountCreatedPayload", accountCreated.javaImportName)
        assertEquals("com.example.model.AccountCreatedPayload", accountCreated.kotlinImportName)
        assertEquals("Created account.", accountCreated.payloadDescription)

        val rawEvent = payloads[1]
        assertEquals("Object", rawEvent.javaTypeName)
        assertEquals("RawEventPayload", rawEvent.kotlinTypeName)
        assertNull(rawEvent.javaImportName)
        assertEquals("com.example.model.RawEventPayload", rawEvent.kotlinImportName)

        val statusChanged = payloads[2]
        assertEquals("String", statusChanged.javaTypeName)
        assertEquals("String", statusChanged.kotlinTypeName)
        assertNull(statusChanged.javaImportName)
        assertNull(statusChanged.kotlinImportName)

        val balanceChanged = payloads[3]
        assertEquals("java.math.BigDecimal", balanceChanged.javaTypeName)
        assertEquals("java.math.BigDecimal", balanceChanged.kotlinTypeName)
        assertNull(balanceChanged.javaImportName)
        assertNull(balanceChanged.kotlinImportName)
    }

    @Test
    fun `preserves payloadless message keys and headers`() {
        val keySchema = SchemaFixtures.inline(type = "string")
        val payload =
            factory.create(
                channel(
                    messages =
                        listOf(
                            AnalyzedMessage(
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
        assertEquals(keySchema, payload.keySchema)
        assertEquals(1, payload.headerProperties.size)
        assertEquals("correlationId", payload.headerProperties.single().wireName)
        assertTrue(payload.headerProperties.single().required)
    }

    @Test
    fun `prepares native payload imports and contract headers`() {
        val payload =
            factory.create(
                channel(
                    multiFormatMessages =
                        listOf(
                            AnalyzedMultiFormatMessage(
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
        multiFormatMessages: List<AnalyzedMultiFormatMessage> = emptyList(),
    ): AnalyzedChannel =
        AnalyzedChannel(
            channelName = "accountEvents",
            topic = "account.events",
            messages = messages,
            multiFormatMessages = multiFormatMessages,
        )
}
