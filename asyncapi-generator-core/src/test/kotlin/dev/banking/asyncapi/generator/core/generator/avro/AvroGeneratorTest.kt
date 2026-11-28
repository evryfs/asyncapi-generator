package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.fixtures.generationInputOf
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class AvroGeneratorTest {
    @Test
    fun `render returns Avro schema artifacts in schema name order`() {
        val generator =
            AvroGenerator(
                packageName = "com.example.avro",
            )
        val schemas =
            mapOf(
                "User" to userSchema(),
                "Status" to statusSchema(),
                "IgnoredPrimitive" to Schema(type = JsonSchemaType.Single("string")),
                "Payment" to
                    Schema(
                        oneOf =
                            listOf(
                                SchemaInterface.SchemaReference(Reference("#/components/schemas/CardPayment")),
                                SchemaInterface.SchemaReference(Reference("#/components/schemas/BankPayment")),
                            ),
                    ),
            )

        val result = generator.render(generationInputOf(schemas))

        assertEquals(
            listOf(
                "com/example/avro/Status.avsc",
                "com/example/avro/User.avsc",
            ),
            result.artifacts.map { it.relativePath },
        )
    }

    private fun userSchema(): Schema =
        Schema(
            type = JsonSchemaType.Single("object"),
            properties =
                mapOf(
                    "id" to SchemaInterface.SchemaInline(Schema(type = JsonSchemaType.Single("string"))),
                ),
            required = listOf("id"),
        )

    private fun statusSchema(): Schema =
        Schema(
            type = JsonSchemaType.Single("string"),
            enum = listOf("ACTIVE", "INACTIVE"),
            default = "ACTIVE",
        )
}
