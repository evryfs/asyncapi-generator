package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeProtobufArtifactGenerationTest {
    private val generation = NativeProtobufArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate writes native Protobuf schema artifacts through writer`() {
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
            generationInput = fixtures.generationInputWithNativeProtobufSchema(),
            artifactWriter = artifactWriter,
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
        assertFalse(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
    }
}
