package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.fixtures.SchemaFixtures
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaHeaderSchema
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
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
                    properties =
                        linkedMapOf(
                            "correlationId" to
                                SchemaFixtures.inline(type = "string", description = "Correlation identifier."),
                            "occurredAt" to SchemaFixtures.inline(type = "string", format = "date-time"),
                            "attempt" to SchemaFixtures.inline(type = "integer", format = "int32"),
                            "amount" to SchemaFixtures.inline(type = "number", multipleOf = 0.01),
                            "retryEnabled" to SchemaFixtures.inline(type = "boolean"),
                            "nullableCode" to
                                SchemaInterface.SchemaInline(
                                    Schema(type = JsonSchemaType.Multiple(listOf("string", "null"))),
                                ),
                        ),
                    requiredProperties =
                        listOf(
                            "correlationId",
                            "occurredAt",
                            "attempt",
                            "nullableCode",
                        ),
                ),
                messageName = "AccountEvent",
            )

        assertEquals(
            listOf(
                KafkaHeaderProperty(
                    wireName = "correlationId",
                    parameterName = "correlationId",
                    type = TypeRef.Scalar.STRING,
                    description = "Correlation identifier.",
                    required = true,
                    nullable = false,
                ),
                KafkaHeaderProperty(
                    wireName = "occurredAt",
                    parameterName = "occurredAt",
                    type = TypeRef.Scalar.DATE_TIME,
                    required = true,
                    nullable = false,
                ),
                KafkaHeaderProperty(
                    wireName = "attempt",
                    parameterName = "attempt",
                    type = TypeRef.Scalar.INT32,
                    required = true,
                    nullable = false,
                ),
                KafkaHeaderProperty(
                    wireName = "amount",
                    parameterName = "amount",
                    type = TypeRef.Scalar.DECIMAL,
                    required = false,
                    nullable = true,
                ),
                KafkaHeaderProperty(
                    wireName = "retryEnabled",
                    parameterName = "retryEnabled",
                    type = TypeRef.Scalar.BOOLEAN,
                    required = false,
                    nullable = true,
                ),
                KafkaHeaderProperty(
                    wireName = "nullableCode",
                    parameterName = "nullableCode",
                    type = TypeRef.Scalar.STRING,
                    required = true,
                    nullable = true,
                ),
            ),
            properties,
        )
    }

    @Test
    fun `resolves referenced scalar header schemas`() {
        val referencedSchema = Schema(type = JsonSchemaType.Single("string"), format = "uuid")

        val property =
            KafkaHeaderPropertyFactory
                .create(
                    AnalyzedMessageHeaders(
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
                    messageName = "AccountEvent",
                ).single()

        assertEquals(TypeRef.Scalar.UUID, property.type)
        assertEquals(false, property.nullable)
    }

    @Test
    fun `rejects unsupported header schema shapes`() {
        val scenarios =
            listOf(
                SchemaFixtures.inline(type = "object") to "object",
                SchemaFixtures.inline(type = "array") to "array",
                SchemaInterface.SchemaInline(Schema(type = JsonSchemaType.Multiple(listOf("string", "integer")))) to
                    "union with multiple non-null types",
                SchemaInterface.SchemaReference(
                    Reference(ref = "./headers.yaml#/missing"),
                ) to "unresolved reference './headers.yaml#/missing'",
            )

        scenarios.forEach { (schema, expectedType) ->
            val error =
                assertFailsWith<UnsupportedKafkaHeaderSchema> {
                    KafkaHeaderPropertyFactory.create(
                        AnalyzedMessageHeaders(
                            properties = mapOf("unsupportedHeader" to schema),
                        ),
                        messageName = "AccountEvent",
                    )
                }

            assertTrue(error.message!!.contains("Header 'unsupportedHeader'"))
            assertTrue(error.message!!.contains("'$expectedType'"))
        }
    }
}
