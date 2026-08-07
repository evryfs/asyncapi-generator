package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnumSymbol
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroField
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroSchemaGeneratorTest {
    @Test
    fun `record render returns schema artifact with namespace-relative path and content`() {
        val generator = AvroSchemaGenerator()
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
        val generator = AvroSchemaGenerator()
        val enumModel =
            AvroEnum(
                namespace = "com.example.avro",
                name = "Status",
                doc = null,
                symbols =
                    listOf(
                        AvroEnumSymbol(name = "ACTIVE", last = false),
                        AvroEnumSymbol(name = "INACTIVE", last = true),
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

}
