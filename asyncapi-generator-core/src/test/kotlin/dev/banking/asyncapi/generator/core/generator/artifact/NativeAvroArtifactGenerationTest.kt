package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NativeAvroArtifactGenerationTest {
    private val generation = NativeAvroArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `render returns relocated native Avro schema and SpecificRecord artifacts`() {
        val result =
            generation.render(
                task =
                    GenerationTask.NativeAvroArtifacts(
                        generateSpecificRecords = true,
                        schemaPackageName = "com.example.schemas",
                    ),
                generationInput = fixtures.generationInputWithNativeAvroSchema(),
            )

        assertEquals(
            setOf(
                "com/example/schemas/UserCreated.avsc" to GeneratedArtifactKind.SCHEMA,
                "com/example/avro/UserCreated.java" to GeneratedArtifactKind.JAVA_SOURCE,
            ),
            result.artifacts.map { it.relativePath to it.kind }.toSet(),
        )
    }
}
