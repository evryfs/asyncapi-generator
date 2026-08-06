package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonSchemaArtifactGenerationTest {
    private val generation = JsonSchemaArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `render returns JSON Schema artifacts`() {
        val result =
            generation.render(
                task = GenerationTask.JsonSchemaArtifacts(packageName = "com.example.schema"),
                generationInput = fixtures.generationInputWithJsonSchemas(),
            )

        assertEquals(
            setOf(
                "com/example/schema/MyAccount.schema.json",
                "com/example/schema/Address.schema.json",
            ),
            result.artifacts.map { it.relativePath }.toSet(),
        )
        assertEquals(
            setOf(GeneratedArtifactKind.SCHEMA),
            result.artifacts.map { it.kind }.toSet(),
        )
    }
}
