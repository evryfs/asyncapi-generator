package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelArtifactGenerationTest {
    private val generation = ModelArtifactGeneration()
    private val fixtures = GenerationInputFixtures()
    private val modelAnnotation =
        QualifiedTypeName.fromConfigurationValue(
            value = "com.example.GeneratedPayload",
            path = "modelConfig.modelAnnotation",
        )

    @Test
    fun `render model artifacts applies configured annotation to Kotlin models`() {
        val result =
            generation.renderModelArtifacts(
                task =
                    GenerationTask.ModelArtifacts(
                        language = SourceLanguage.KOTLIN,
                        packageName = "com.example.model",
                        annotation = modelAnnotation,
                    ),
                generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            )

        val user = result.artifacts.single { it.relativePath == "com/example/model/User.kt" }
        assertEquals(GeneratedArtifactKind.SOURCE, user.kind)
        assertTrue(user.content.contains("import com.example.GeneratedPayload"))
        assertTrue(user.content.contains("@GeneratedPayload"))
    }

    @Test
    fun `render model artifacts applies configured annotation to Java classes`() {
        val result =
            generation.renderModelArtifacts(
                task =
                    GenerationTask.ModelArtifacts(
                        language = SourceLanguage.JAVA,
                        packageName = "com.example.model",
                        annotation = modelAnnotation,
                    ),
                generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            )

        val content =
            result.artifacts.single { it.relativePath == "com/example/model/User.java" }.content
        assertTrue(content.contains("import com.example.GeneratedPayload;"))
        assertTrue(content.lineSequence().zipWithNext().any { (annotation, declaration) ->
            annotation == "@GeneratedPayload" && declaration.startsWith("public class User")
        })
    }

    @Test
    fun `render Kafka key model artifacts returns only native payload key models`() {
        val result =
            generation.renderKafkaKeyModelArtifacts(
                task =
                    GenerationTask.KafkaKeyModelArtifacts(
                        language = SourceLanguage.KOTLIN,
                        packageName = "com.example.model",
                    ),
                generationInput = fixtures.generationInputWithNativeAvroMessageAndObjectKey(),
            )

        assertEquals(
            listOf("com/example/model/UserCreatedKey.kt"),
            result.artifacts.map { it.relativePath },
        )
    }

    @Test
    fun `render model artifacts returns Java records when configured`() {
        val result =
            generation.renderModelArtifacts(
                task =
                    GenerationTask.ModelArtifacts(
                        language = SourceLanguage.JAVA,
                        packageName = "com.example.model",
                        annotation = modelAnnotation,
                        javaModelType = JavaModelType.RECORD,
                    ),
                generationInput = fixtures.generationInputWithObjectEnumAndPrimitive(),
            )

        val user = result.artifacts.single { it.relativePath == "com/example/model/User.java" }
        assertEquals(GeneratedArtifactKind.SOURCE, user.kind)
        assertTrue(user.content.contains("import com.example.GeneratedPayload;"))
        assertTrue(user.content.contains("@GeneratedPayload\npublic record User("))
    }
}
