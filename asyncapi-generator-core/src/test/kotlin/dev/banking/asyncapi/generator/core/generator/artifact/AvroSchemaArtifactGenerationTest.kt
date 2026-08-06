package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AvroSchemaArtifactGenerationTest {
    private val generation = AvroSchemaArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `render returns Avro schema artifacts and ignores unsupported primitive projection`() {
        val result =
            generation.render(
                task = GenerationTask.AvroSchemaArtifacts(packageName = "com.example.avro"),
                generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            )

        assertEquals(
            setOf(
                "com/example/avro/User.avsc",
                "com/example/avro/Status.avsc",
            ),
            result.artifacts.map { it.relativePath }.toSet(),
        )
        assertEquals(
            setOf(GeneratedArtifactKind.SCHEMA),
            result.artifacts.map { it.kind }.toSet(),
        )
    }
}
