package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `render returns generation result with artifacts for Avro schemas`() {
        val generator =
            AvroGenerator(
                outputDir = tempDir.toFile(),
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

    @Test
    fun `generate writes rendered artifacts to output directory`() {
        val generator =
            AvroGenerator(
                outputDir = tempDir.toFile(),
                packageName = "com.example.avro",
            )

        generator.generate(mapOf("User" to userSchema()))

        val output = tempDir.resolve("com/example/avro/User.avsc").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("\"name\": \"User\""))
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
