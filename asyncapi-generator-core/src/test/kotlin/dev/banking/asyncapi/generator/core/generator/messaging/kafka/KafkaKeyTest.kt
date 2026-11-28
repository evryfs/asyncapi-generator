package dev.banking.asyncapi.generator.core.generator.messaging.kafka

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

internal class KafkaKeyTest {
    @Test
    fun `uses message name for untitled inline object key`() {
        val schema = Schema(type = JsonSchemaType.Single("object"))

        val result =
            KafkaKey.of(
                messageName = "AccountUpdated",
                declaredSchema = SchemaInterface.SchemaInline(schema),
            )

        assertEquals("AccountUpdatedKey", result.modelName)
        assertSame(schema, result.schema)
    }

    @Test
    fun `uses schema title for titled inline object key`() {
        val schema = Schema(type = JsonSchemaType.Single("object"), title = "Institution Account Key")

        val result =
            KafkaKey.of(
                messageName = "AccountUpdated",
                declaredSchema = SchemaInterface.SchemaInline(schema),
            )

        assertEquals("InstitutionAccountKey", result.modelName)
    }

    @Test
    fun `uses reference target name for referenced object key`() {
        val schema = Schema(type = JsonSchemaType.Single("object"), title = "Ignored Reference Title")

        val result =
            KafkaKey.of(
                messageName = "AccountUpdated",
                declaredSchema =
                    SchemaInterface.SchemaReference(
                        Reference(
                            ref = "./key-schemas.yaml#/AccountKey",
                            model = schema,
                        ),
                    ),
            )

        assertEquals("AccountKey", result.modelName)
        assertSame(schema, result.schema)
    }
}
