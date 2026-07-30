package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeAvroArtifactGenerationTest {
    private val generation = NativeAvroArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate writes native Avro schema artifacts through writer`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
                javaSourceOutputDirectory = javaSourceOutputDirectory,
            )

        generation.generate(
            task =
                GenerationTask.NativeAvroArtifacts(
                    generateSpecificRecords = true,
                    schemaPackageName = "com.example.schemas",
                ),
            generationInput = fixtures.generationInputWithNativeAvroSchema(),
            artifactWriter = artifactWriter,
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/schemas/UserCreated.avsc").exists())
        assertTrue(javaSourceOutputDirectory.resolve("com/example/avro/UserCreated.java").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/avro/UserCreated.avsc").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/avro/UserCreated.java").exists())
        assertFalse(resourceOutputDirectory.resolve("com/example/avro/UserCreated.java").exists())
    }
}
