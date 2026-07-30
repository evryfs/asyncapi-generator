package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonSchemaArtifactGenerationTest {
    private val generation = JsonSchemaArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate writes JSON Schema artifacts through the resource writer`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
            )

        generation.generate(
            task = GenerationTask.JsonSchemaArtifacts(packageName = "com.example.schema"),
            generationInput = fixtures.generationInputWithJsonSchemas(),
            artifactWriter = artifactWriter,
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/schema/MyAccount.schema.json").exists())
        assertTrue(resourceOutputDirectory.resolve("com/example/schema/Address.schema.json").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/schema/MyAccount.schema.json").exists())
    }
}
