package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelType
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
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
            task =
                GenerationTask.NativeProtobufArtifacts(
                    schemaPackageName = "com.example.schemas",
                ),
            generationInput = fixtures.generationInputWithNativeProtobufSchema(),
            artifactWriter = artifactWriter,
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/schemas/UserCreated.proto").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
        assertFalse(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
    }

    @Test
    fun `generate writes native Protobuf Java message artifacts through writer`() {
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
                GenerationTask.NativeProtobufArtifacts(
                    models = ProtobufModelGeneration(packageName = "com.example.protobuf"),
                ),
            generationInput = fixtures.generationInputWithNativeProtobufJavaMessageSchema(),
            artifactWriter = artifactWriter,
        )

        assertTrue(resourceOutputDirectory.resolve("com/example/protobuf/UserCreated.proto").exists())
        assertTrue(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreated.java").exists())
        assertTrue(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreatedOrBuilder.java").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/protobuf/UserCreated.java").exists())
    }

    @Test
    fun `generate writes Kotlin Protobuf DSL and Java message artifacts through writer`() {
        val sourceOutputDirectory = tempDir.resolve("kotlin-sources").toFile()
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
                GenerationTask.NativeProtobufArtifacts(
                    models =
                        ProtobufModelGeneration(
                            packageName = "com.example.protobuf",
                            modelType = ProtobufModelType.KOTLIN,
                        ),
                ),
            generationInput = fixtures.generationInputWithNativeProtobufJavaMessageSchema(),
            artifactWriter = artifactWriter,
        )

        assertTrue(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreated.java").exists())
        assertTrue(sourceOutputDirectory.resolve("com/example/protobuf/UserCreatedKt.kt").exists())
        assertFalse(javaSourceOutputDirectory.resolve("com/example/protobuf/UserCreatedKt.kt").exists())
    }
}
