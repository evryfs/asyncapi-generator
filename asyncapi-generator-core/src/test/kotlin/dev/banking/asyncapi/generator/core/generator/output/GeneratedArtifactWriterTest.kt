package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `filesystem writer writes java source artifacts under java source output directory`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
                javaSourceOutputDirectory = javaSourceOutputDirectory,
            )

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.java",
                    content = "public class User {}",
                    kind = GeneratedArtifactKind.JAVA_SOURCE,
                ),
            ),
        )

        assertEquals(
            "public class User {}",
            javaSourceOutputDirectory.resolve("com/example/User.java").readText(),
        )
        assertFalse(sourceOutputDirectory.resolve("com/example/User.java").exists())
        assertFalse(resourceOutputDirectory.resolve("com/example/User.java").exists())
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

    @Test
    fun `filesystem writer rejects destination collisions before writing artifacts`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        val error =
            assertFailsWith<GeneratedArtifactCollision> {
                writer.write(
                    GenerationResult.of(
                        GeneratedArtifact(
                            relativePath = "com/example/Unrelated.kt",
                            content = "unrelated",
                            kind = GeneratedArtifactKind.SOURCE,
                        ),
                        GeneratedArtifact(
                            relativePath = "com/example/User.java",
                            content = "source model",
                            kind = GeneratedArtifactKind.SOURCE,
                        ),
                        GeneratedArtifact(
                            relativePath = "com/example/User.java",
                            content = "native model",
                            kind = GeneratedArtifactKind.JAVA_SOURCE,
                        ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("SOURCE: com/example/User.java"))
        assertTrue(error.message!!.contains("JAVA_SOURCE: com/example/User.java"))
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `filesystem writer allows matching relative paths under distinct output roots`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val javaSourceOutputDirectory = tempDir.resolve("java-sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer =
            FileSystemGeneratedArtifactWriter(
                sourceOutputDirectory = sourceOutputDirectory,
                resourceOutputDirectory = resourceOutputDirectory,
                javaSourceOutputDirectory = javaSourceOutputDirectory,
            )

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.java",
                    content = "source model",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
                GeneratedArtifact(
                    relativePath = "com/example/User.java",
                    content = "native model",
                    kind = GeneratedArtifactKind.JAVA_SOURCE,
                ),
            ),
        )

        assertEquals("source model", sourceOutputDirectory.resolve("com/example/User.java").readText())
        assertEquals("native model", javaSourceOutputDirectory.resolve("com/example/User.java").readText())
    }
}
