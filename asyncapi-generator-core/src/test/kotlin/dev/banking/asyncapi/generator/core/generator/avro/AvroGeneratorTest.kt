package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AvroGeneratorTest {
    @Test
    fun `render returns generation result with artifacts for Avro schemas`() {
        val generator =
            AvroGenerator(
                packageName = "com.example.avro",
            )
        val schemas =
            mapOf(
                "User" to userSchema(),
                "Status" to statusSchema(),
                "IgnoredPrimitive" to Schema(type = "string"),
            )

        val result = generator.render(schemas)

        assertEquals(
            listOf(
                "com/example/avro/User.avsc",
                "com/example/avro/Status.avsc",
            ),
            result.artifacts.map { it.relativePath },
        )
    }

    private fun userSchema(): Schema =
        Schema(
            type = "object",
            properties =
                mapOf(
                    "id" to SchemaInterface.SchemaInline(Schema(type = "string")),
                ),
            required = listOf("id"),
        )

    private fun statusSchema(): Schema =
        Schema(
            type = "string",
            enum = listOf("ACTIVE", "INACTIVE"),
            default = "ACTIVE",
        )
}
