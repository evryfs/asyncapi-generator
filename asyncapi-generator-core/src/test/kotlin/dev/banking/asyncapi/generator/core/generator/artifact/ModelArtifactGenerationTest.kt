package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelArtifactGenerationTest {
    private val generation = ModelArtifactGeneration()
    private val fixtures = GenerationInputFixtures()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `generate model artifacts writes Kotlin source artifacts through writer`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
            )

        generation.generateModelArtifacts(
            task =
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.model",
                    annotation = "com.example.NoArg",
                ),
            generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            sourceOutputDirectory = sourceOutputDirectory,
            artifactWriter = artifactWriter,
        )

        val user = sourceOutputDirectory.resolve("com/example/model/User.kt")
        assertTrue(user.exists())
        assertTrue(user.readText().contains("@NoArg"))
        assertFalse(resourceOutputDirectory.resolve("com/example/model/User.kt").exists())
    }

    @Test
    fun `generate Kafka key model artifacts writes only native payload key models`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
            )

        generation.generateKafkaKeyModelArtifacts(
            task =
                GenerationTask.KafkaKeyModelArtifacts(
                    language = SourceLanguage.KOTLIN,
                    packageName = "com.example.model",
                ),
            generationInput = fixtures.generationInputWithNativeAvroMessageAndObjectKey(),
            sourceOutputDirectory = sourceOutputDirectory,
            artifactWriter = artifactWriter,
        )

        assertTrue(sourceOutputDirectory.resolve("com/example/model/UserCreatedKey.kt").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/model/UserCreated.kt").exists())
        assertFalse(resourceOutputDirectory.resolve("com/example/model/UserCreatedKey.kt").exists())
    }

    @Test
    fun `generate header model artifacts writes Java header artifacts through writer`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
            )

        generation.generateHeaderModelArtifacts(
            task =
                GenerationTask.HeaderModelArtifacts(
                    language = SourceLanguage.JAVA,
                    packageName = "com.example.client.header",
                ),
            asyncApiDocument = fixtures.documentWithMessageHeaders(),
            generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            sourceOutputDirectory = sourceOutputDirectory,
            artifactWriter = artifactWriter,
        )

        assertTrue(
            sourceOutputDirectory
                .resolve("com/example/client/header/TopicUserEventsHeadersUserSignup.java")
                .exists(),
        )
        assertFalse(
            resourceOutputDirectory
                .resolve("com/example/client/header/TopicUserEventsHeadersUserSignup.java")
                .exists(),
        )
    }

    @Test
    fun `generate model artifacts writes Java record artifacts when configured`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
            )

        generation.generateModelArtifacts(
            task =
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.JAVA,
                    packageName = "com.example.model",
                    javaModelType = JavaModelType.RECORD,
                ),
            generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            sourceOutputDirectory = sourceOutputDirectory,
            artifactWriter = artifactWriter,
        )

        val user = sourceOutputDirectory.resolve("com/example/model/User.java")
        assertTrue(user.exists())
        assertTrue(user.readText().contains("public record User("))
        assertFalse(resourceOutputDirectory.resolve("com/example/model/User.java").exists())
    }
}
