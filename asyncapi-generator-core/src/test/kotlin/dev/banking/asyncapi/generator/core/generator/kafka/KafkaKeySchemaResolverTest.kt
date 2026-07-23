package dev.banking.asyncapi.generator.core.generator.kafka

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class KafkaKeySchemaResolverTest {
    @Test
    fun `uses message name for untitled inline object key`() {
        val schema = Schema(type = "object")

        val result =
            KafkaKeySchemaResolver.resolveObjectModelOrNull(
                messageName = "AccountUpdated",
                schema = SchemaInterface.SchemaInline(schema),
            )

        assertEquals("AccountUpdatedKey", result?.name)
        assertSame(schema, result?.schema)
    }

    @Test
    fun `uses schema title for titled inline object key`() {
        val schema = Schema(type = "object", title = "Institution Account Key")

        val result =
            KafkaKeySchemaResolver.resolveObjectModelOrNull(
                messageName = "AccountUpdated",
                schema = SchemaInterface.SchemaInline(schema),
            )

        assertEquals("InstitutionAccountKey", result?.name)
    }

    @Test
    fun `uses reference target name for referenced object key`() {
        val schema = Schema(type = "object", title = "Ignored Reference Title")

        val result =
            KafkaKeySchemaResolver.resolveObjectModelOrNull(
                messageName = "AccountUpdated",
                schema =
                    SchemaInterface.SchemaReference(
                        Reference(
                            ref = "./key-schemas.yaml#/AccountKey",
                            model = schema,
                        ),
                    ),
            )

        assertEquals("AccountKey", result?.name)
        assertSame(schema, result?.schema)
    }
}
