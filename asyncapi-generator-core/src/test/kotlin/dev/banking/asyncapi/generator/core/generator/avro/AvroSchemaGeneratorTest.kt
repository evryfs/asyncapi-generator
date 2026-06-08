package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroField
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroUnionType
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroSchemaGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `record render returns schema artifact with namespace-relative path and content`() {
        val generator = AvroSchemaGenerator(tempDir.toFile())
        val record =
            AvroRecord(
                namespace = "com.example.avro",
                name = "User",
                doc = "Generated user schema.",
                fields =
                    listOf(
                        AvroField(
                            name = "id",
                            doc = "User identifier.",
                            jsonType = "\"string\"",
                            last = true,
                        ),
                    ),
            )

        val artifact = generator.render(record)

        assertEquals(GeneratedArtifactKind.SCHEMA, artifact.kind)
        assertEquals("com/example/avro/User.avsc", artifact.relativePath)
        assertTrue(artifact.content.contains("\"type\": \"record\""))
        assertTrue(artifact.content.contains("\"name\": \"User\""))
        assertTrue(artifact.content.contains("\"namespace\": \"com.example.avro\""))
    }

    @Test
    fun `enum render returns schema artifact with namespace-relative path and content`() {
        val generator = AvroSchemaGenerator(tempDir.toFile())
        val enumModel =
            AvroEnum(
                namespace = "com.example.avro",
                name = "Status",
                doc = null,
                symbols =
                    listOf(
                        AvroUnionType(name = "ACTIVE", last = false),
                        AvroUnionType(name = "INACTIVE", last = true),
                    ),
                default = "ACTIVE",
            )

        val artifact = generator.render(enumModel)

        assertEquals(GeneratedArtifactKind.SCHEMA, artifact.kind)
        assertEquals("com/example/avro/Status.avsc", artifact.relativePath)
        assertTrue(artifact.content.contains("\"type\": \"enum\""))
        assertTrue(artifact.content.contains("\"name\": \"Status\""))
        assertTrue(artifact.content.contains("\"ACTIVE\""))
    }

    @Test
    fun `generate writes rendered schema artifact to output directory`() {
        val generator = AvroSchemaGenerator(tempDir.toFile())
        val record =
            AvroRecord(
                namespace = "com.example.avro",
                name = "User",
                doc = null,
                fields = emptyList(),
            )

        generator.generate(record)

        val output = tempDir.resolve("com/example/avro/User.avsc").toFile()
        assertTrue(output.exists())
        assertTrue(output.readText().contains("\"name\": \"User\""))
    }
}
