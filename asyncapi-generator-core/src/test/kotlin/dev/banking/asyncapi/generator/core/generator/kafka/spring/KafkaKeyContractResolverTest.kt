package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKey
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KafkaKeyContractResolverTest {
    @Test
    fun `resolves supported scalar key schemas`() {
        val scenarios =
            listOf(
                Schema(type = "string") to TypeRef.Scalar.STRING,
                Schema(type = "string", format = "uuid") to TypeRef.Scalar.UUID,
                Schema(type = "integer", format = "int32") to TypeRef.Scalar.INT32,
                Schema(type = "integer", format = "int64") to TypeRef.Scalar.INT64,
                Schema(type = "number", format = "float") to TypeRef.Scalar.FLOAT,
                Schema(type = "number", format = "double") to TypeRef.Scalar.DOUBLE,
                Schema(type = "boolean") to TypeRef.Scalar.BOOLEAN,
            )

        scenarios.forEach { (schema, expected) ->
            val result =
                requireNotNull(
                    KafkaKeyContractResolver.resolve(
                        kafkaKey = KafkaKey.of("AccountUpdated", SchemaInterface.SchemaInline(schema)),
                    ),
                )

            assertEquals(schema, result.schema)
            assertEquals(expected, result.type)
            assertEquals(false, result.nullable)
        }
    }

    @Test
    fun `returns no key contract when the binding key is absent`() {
        assertNull(
            KafkaKeyContractResolver.resolve(
                kafkaKey = null,
            ),
        )
    }

    @Test
    fun `preserves nullable scalar key schemas`() {
        val result =
            requireNotNull(
                KafkaKeyContractResolver.resolve(
                    kafkaKey =
                        KafkaKey.of(
                            "AccountUpdated",
                            SchemaInterface.SchemaInline(
                                Schema(type = listOf("integer", "null"), format = "int64"),
                            ),
                        ),
                ),
            )

        assertEquals(TypeRef.Scalar.INT64, result.type)
        assertTrue(result.nullable)
    }

    @Test
    fun `resolves referenced scalar key schemas`() {
        val referencedSchema = Schema(type = "string", format = "uuid")
        val result =
            requireNotNull(
                KafkaKeyContractResolver.resolve(
                    kafkaKey =
                        KafkaKey.of(
                            "AccountUpdated",
                            SchemaInterface.SchemaReference(
                                Reference(
                                    ref = "./keys.yaml#/accountId",
                                    model = referencedSchema,
                                ),
                            ),
                        ),
                ),
            )

        assertEquals(referencedSchema, result.schema)
        assertEquals(TypeRef.Scalar.UUID, result.type)
    }

    @Test
    fun `resolves inline object key schema to generated model type`() {
        val schema =
            Schema(
                type = "object",
                description = "Composite account key.",
                properties =
                    mapOf(
                        "institutionId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                        "accountId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )

        val result =
            requireNotNull(
                KafkaKeyContractResolver.resolve(
                    kafkaKey = KafkaKey.of("AccountUpdated", SchemaInterface.SchemaInline(schema)),
                ),
            )

        assertEquals(schema, result.schema)
        assertEquals(TypeRef.Named(name = "AccountUpdatedKey", isModel = true), result.type)
        assertTrue(result.isModel)
    }
}
