package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.info.Info
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentArtifactGenerationTest {
    private val generation = DocumentArtifactGeneration()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `render serializes bundled document without writing its explicit destination`() {
        val outputFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()

        val result =
            generation.render(
                task =
                    GenerationTask.DocumentArtifact(
                        file = outputFile,
                        format = DocumentFormat.YAML,
                    ),
                asyncApiDocument =
                    AsyncApiDocument(
                        asyncapi = "3.0.0",
                        info = Info(title = "Document output", version = "1.0.0"),
                    ),
            )

        val artifact = result.documentArtifacts.single()
        assertEquals(outputFile, artifact.file)
        assertTrue(artifact.content.startsWith("asyncapi:"))
        assertTrue(artifact.content.contains("Document output"))
        assertFalse(outputFile.exists())
        assertTrue(result.artifacts.isEmpty())
    }
}
