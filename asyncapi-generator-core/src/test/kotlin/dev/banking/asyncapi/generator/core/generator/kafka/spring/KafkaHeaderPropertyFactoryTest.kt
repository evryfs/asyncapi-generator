package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SchemaFixtures
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaHeaderSchema
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KafkaHeaderPropertyFactoryTest {
    @Test
    fun `preserves supported scalar types and header nullability`() {
        val properties =
            KafkaHeaderPropertyFactory.create(
                AnalyzedMessageHeaders(
                    typeName = "AccountEventHeaders",
                    properties =
                        linkedMapOf(
                            "correlationId" to
                                SchemaFixtures.inline(type = "string", description = "Correlation identifier."),
                            "occurredAt" to SchemaFixtures.inline(type = "string", format = "date-time"),
                            "attempt" to SchemaFixtures.inline(type = "integer", format = "int32"),
                            "amount" to SchemaFixtures.inline(type = "number", multipleOf = 0.01),
                            "retryEnabled" to SchemaFixtures.inline(type = "boolean"),
                            "nullableCode" to SchemaFixtures.inline(type = listOf("string", "null")),
                        ),
                    requiredProperties =
                        listOf(
                            "correlationId",
                            "occurredAt",
                            "attempt",
                            "nullableCode",
                        ),
                ),
            )

        assertEquals(
            listOf(
                KafkaHeaderProperty(
                    wireName = "correlationId",
                    parameterName = "correlationId",
                    javaTypeName = "String",
                    kotlinTypeName = "String",
                    description = "Correlation identifier.",
                    required = true,
                    nullable = false,
                ),
                KafkaHeaderProperty(
                    wireName = "occurredAt",
                    parameterName = "occurredAt",
                    javaTypeName = "OffsetDateTime",
                    kotlinTypeName = "OffsetDateTime",
                    importName = "java.time.OffsetDateTime",
                    required = true,
                    nullable = false,
                ),
                KafkaHeaderProperty(
                    wireName = "attempt",
                    parameterName = "attempt",
                    javaTypeName = "Integer",
                    kotlinTypeName = "Int",
                    required = true,
                    nullable = false,
                ),
                KafkaHeaderProperty(
                    wireName = "amount",
                    parameterName = "amount",
                    javaTypeName = "BigDecimal",
                    kotlinTypeName = "BigDecimal",
                    importName = "java.math.BigDecimal",
                    required = false,
                    nullable = true,
                ),
                KafkaHeaderProperty(
                    wireName = "retryEnabled",
                    parameterName = "retryEnabled",
                    javaTypeName = "Boolean",
                    kotlinTypeName = "Boolean",
                    required = false,
                    nullable = true,
                ),
                KafkaHeaderProperty(
                    wireName = "nullableCode",
                    parameterName = "nullableCode",
                    javaTypeName = "String",
                    kotlinTypeName = "String",
                    required = true,
                    nullable = true,
                ),
            ),
            properties,
        )
    }

    @Test
    fun `resolves referenced scalar header schemas`() {
        val referencedSchema = Schema(type = "string", format = "uuid")

        val property =
            KafkaHeaderPropertyFactory.create(
                AnalyzedMessageHeaders(
                    typeName = "AccountEventHeaders",
                    properties =
                        mapOf(
                            "requestId" to
                                SchemaInterface.SchemaReference(
                                    Reference(
                                        ref = "./headers.yaml#/requestId",
                                        model = referencedSchema,
                                    ),
                                ),
                        ),
                    requiredProperties = listOf("requestId"),
                ),
            ).single()

        assertEquals("UUID", property.javaTypeName)
        assertEquals("UUID", property.kotlinTypeName)
        assertEquals("java.util.UUID", property.importName)
        assertEquals(false, property.nullable)
    }

    @Test
    fun `rejects unsupported header schema shapes`() {
        val scenarios =
            listOf(
                SchemaFixtures.inline(type = "object") to "object",
                SchemaFixtures.inline(type = "array") to "array",
                SchemaFixtures.inline(type = listOf("string", "integer")) to "union with multiple non-null types",
                SchemaInterface.SchemaReference(
                    Reference(ref = "./headers.yaml#/missing"),
                ) to "unresolved reference './headers.yaml#/missing'",
            )

        scenarios.forEach { (schema, expectedType) ->
            val error =
                assertFailsWith<UnsupportedKafkaHeaderSchema> {
                    KafkaHeaderPropertyFactory.create(
                        AnalyzedMessageHeaders(
                            typeName = "AccountEventHeaders",
                            properties = mapOf("unsupportedHeader" to schema),
                        ),
                    )
                }

            assertTrue(error.message!!.contains("Header 'unsupportedHeader'"))
            assertTrue(error.message!!.contains("'$expectedType'"))
        }
    }

}
