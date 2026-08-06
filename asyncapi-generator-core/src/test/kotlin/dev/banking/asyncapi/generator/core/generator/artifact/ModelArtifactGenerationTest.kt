package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
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
    private val modelAnnotation =
        QualifiedTypeName.fromConfigurationValue(
            value = "com.example.GeneratedPayload",
            path = "modelConfig.modelAnnotation",
        )

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
                    annotation = modelAnnotation,
                ),
            generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            artifactWriter = artifactWriter,
        )

        val user = sourceOutputDirectory.resolve("com/example/model/User.kt")
        assertTrue(user.exists())
        assertTrue(user.readText().contains("import com.example.GeneratedPayload"))
        assertTrue(user.readText().contains("@GeneratedPayload"))
        assertFalse(resourceOutputDirectory.resolve("com/example/model/User.kt").exists())
    }

    @Test
    fun `generate model artifacts applies configured annotation to Java classes`() {
        val sourceOutputDirectory = tempDir.resolve("java-class-sources").toFile()
        val artifactWriter =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = tempDir.resolve("java-class-resources").toFile(),
            )

        generation.generateModelArtifacts(
            task =
                GenerationTask.ModelArtifacts(
                    language = SourceLanguage.JAVA,
                    packageName = "com.example.model",
                    annotation = modelAnnotation,
                ),
            generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            artifactWriter = artifactWriter,
        )

        val content =
            sourceOutputDirectory
                .resolve("com/example/model/User.java")
                .readText()
        assertTrue(content.contains("import com.example.GeneratedPayload;"))
        assertTrue(content.lineSequence().zipWithNext().any { (annotation, declaration) ->
            annotation == "@GeneratedPayload" && declaration.startsWith("public class User")
        })
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
            artifactWriter = artifactWriter,
        )

        assertTrue(sourceOutputDirectory.resolve("com/example/model/UserCreatedKey.kt").exists())
        assertFalse(sourceOutputDirectory.resolve("com/example/model/UserCreated.kt").exists())
        assertFalse(resourceOutputDirectory.resolve("com/example/model/UserCreatedKey.kt").exists())
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
                    annotation = modelAnnotation,
                    javaModelType = JavaModelType.RECORD,
                ),
            generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            artifactWriter = artifactWriter,
        )

        val user = sourceOutputDirectory.resolve("com/example/model/User.java")
        assertTrue(user.exists())
        assertTrue(user.readText().contains("import com.example.GeneratedPayload;"))
        assertTrue(user.readText().contains("@GeneratedPayload\npublic record User("))
        assertFalse(resourceOutputDirectory.resolve("com/example/model/User.java").exists())
    }
}
