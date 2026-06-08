package dev.banking.asyncapi.generator.core.generator.output

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GeneratedArtifactWriterTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `filesystem writer writes source artifacts under source output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "data class User(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        assertEquals(
            "data class User(val id: String)",
            sourceOutputDirectory.resolve("com/example/User.kt").readText(),
        )
        assertFalse(resourceOutputDirectory.resolve("com/example/User.kt").exists())
    }

    @Test
    fun `filesystem writer writes resource artifacts under resource output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
                    content = "com.example.AsyncApiKafkaAutoConfiguration",
                    kind = GeneratedArtifactKind.RESOURCE,
                ),
            ),
        )

        assertEquals(
            "com.example.AsyncApiKafkaAutoConfiguration",
            resourceOutputDirectory
                .resolve("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .readText(),
        )
        assertFalse(
            sourceOutputDirectory
                .resolve("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .exists(),
        )
    }

    @Test
    fun `filesystem writer writes schema artifacts under resource output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/schema/User.avsc",
                    content = """{"type":"record","name":"User"}""",
                    kind = GeneratedArtifactKind.SCHEMA,
                ),
            ),
        )

        assertEquals(
            """{"type":"record","name":"User"}""",
            resourceOutputDirectory.resolve("com/example/schema/User.avsc").readText(),
        )
        assertFalse(sourceOutputDirectory.resolve("com/example/schema/User.avsc").exists())
    }

    @Test
    fun `filesystem writer creates parent directories and writes all artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "source",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
                GeneratedArtifact(
                    relativePath = "com/example/schema/User.avsc",
                    content = "schema",
                    kind = GeneratedArtifactKind.SCHEMA,
                ),
            ),
        )

        assertEquals("source", sourceOutputDirectory.resolve("com/example/User.kt").readText())
        assertEquals("schema", resourceOutputDirectory.resolve("com/example/schema/User.avsc").readText())
    }

    @Test
    fun `filesystem writer ignores empty generation result`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(GenerationResult.Empty)

        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }
}
