package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
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
    fun `filesystem writer writes bundled documents to their explicit destinations`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val documentOutputFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedDocumentArtifact(
                    file = documentOutputFile,
                    content = "asyncapi: 3.0.0\n",
                ),
            ),
        )

        assertEquals("asyncapi: 3.0.0\n", documentOutputFile.readText())
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
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
    fun `filesystem writer rejects artifact and bundled document destination collisions before writing`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val destination = sourceOutputDirectory.resolve("com/example/User.kt")
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        val error =
            assertFailsWith<GeneratedArtifactCollision> {
                writer.write(
                    GenerationResult(
                        artifacts =
                            listOf(
                                GeneratedArtifact(
                                    relativePath = "com/example/User.kt",
                                    content = "source model",
                                    kind = GeneratedArtifactKind.SOURCE,
                                ),
                            ),
                        documentArtifacts =
                            listOf(
                                GeneratedDocumentArtifact(
                                    file = destination,
                                    content = "asyncapi: 3.0.0\n",
                                ),
                            ),
                    ),
                )
            }

        assertTrue(error.message!!.contains("SOURCE: com/example/User.kt"))
        assertTrue(error.message!!.contains("BUNDLED_DOCUMENT: ${destination.path}"))
        assertFalse(destination.exists())
        assertFalse(sourceOutputDirectory.exists())
        assertFalse(resourceOutputDirectory.exists())
    }

    @Test
    fun `filesystem writer rejects matching bundled document destinations before writing`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val destination = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        val error =
            assertFailsWith<GeneratedArtifactCollision> {
                writer.write(
                    GenerationResult(
                        artifacts = emptyList(),
                        documentArtifacts =
                            listOf(
                                GeneratedDocumentArtifact(destination, "first"),
                                GeneratedDocumentArtifact(destination, "second"),
                            ),
                    ),
                )
            }

        assertEquals(
            2,
            error.message!!.lineSequence().count { line -> line.contains("BUNDLED_DOCUMENT:") },
        )
        assertFalse(destination.exists())
        assertFalse(destination.parentFile.exists())
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

    @Test
    fun `filesystem writer preserves artifacts from earlier executions`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/ContractA.kt",
                    content = "data class ContractA(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/ContractB.kt",
                    content = "data class ContractB(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        assertEquals(
            "data class ContractA(val id: String)",
            sourceOutputDirectory.resolve("com/example/ContractA.kt").readText(),
        )
        assertEquals(
            "data class ContractB(val id: String)",
            sourceOutputDirectory.resolve("com/example/ContractB.kt").readText(),
        )
    }

    @Test
    fun `filesystem writer preserves unrelated files`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        sourceOutputDirectory.mkdirs()
        sourceOutputDirectory.resolve("unrelated.txt").writeText("keep me")

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "data class User(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        assertEquals("keep me", sourceOutputDirectory.resolve("unrelated.txt").readText())
        assertEquals(
            "data class User(val id: String)",
            sourceOutputDirectory.resolve("com/example/User.kt").readText(),
        )
    }

    @Test
    fun `filesystem writer replaces existing file when content changes`() {
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

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "data class User(val id: String, val name: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        assertEquals(
            "data class User(val id: String, val name: String)",
            sourceOutputDirectory.resolve("com/example/User.kt").readText(),
        )
    }

    @Test
    fun `filesystem writer preserves timestamp when content is identical`() {
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

        val file = sourceOutputDirectory.resolve("com/example/User.kt")
        val knownTimestamp = java.nio.file.attribute.FileTime.fromMillis(1_000_000_000L)
        java.nio.file.Files.setLastModifiedTime(file.toPath(), knownTimestamp)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "data class User(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        assertEquals(knownTimestamp, java.nio.file.Files.getLastModifiedTime(file.toPath()))
    }

    @Test
    fun `filesystem writer bundles documents through same staging path`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val documentFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedDocumentArtifact(
                    file = documentFile,
                    content = "asyncapi: 3.0.0\n",
                ),
            ),
        )

        assertEquals("asyncapi: 3.0.0\n", documentFile.readText())

        writer.write(
            GenerationResult.of(
                GeneratedDocumentArtifact(
                    file = documentFile,
                    content = "asyncapi: 3.0.0\ninfo:\n  title: Updated\n",
                ),
            ),
        )

        assertEquals("asyncapi: 3.0.0\ninfo:\n  title: Updated\n", documentFile.readText())
    }

    @Test
    fun `filesystem writer produces clear error for directory conflict`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        // Create a file where a directory is needed
        sourceOutputDirectory.mkdirs()
        sourceOutputDirectory.resolve("com").writeText("blocking file")

        val error = assertFailsWith<IOException> {
            writer.write(
                GenerationResult.of(
                    GeneratedArtifact(
                        relativePath = "com/example/User.kt",
                        content = "data class User(val id: String)",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                ),
            )
        }

        assertTrue(error.message!!.contains("com/example/User.kt"))
        assertTrue(error.message!!.contains("SOURCE"))
    }

    @Test
    fun `filesystem writer preserves destinations after staging failure`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        // Write initial artifact
        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/Existing.kt",
                    content = "old content",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        // Create a file where a directory is needed for artifact 2
        sourceOutputDirectory.resolve("com/example/sub").writeText("blocking file")

        // Artifact 1 has changed content, artifact 2 conflicts, artifact 3 must not be created
        assertFailsWith<IOException> {
            writer.write(
                GenerationResult.of(
                    GeneratedArtifact(
                        relativePath = "com/example/Existing.kt",
                        content = "new content",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                    GeneratedArtifact(
                        relativePath = "com/example/sub/deeper/New.kt",
                        content = "should fail",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                    GeneratedArtifact(
                        relativePath = "com/example/NotCreated.kt",
                        content = "must not exist",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                ),
            )
        }

        // Artifact 1 retains old content (commit never started)
        assertEquals("old content", sourceOutputDirectory.resolve("com/example/Existing.kt").readText())
        // Artifact 3 was never staged or created
        assertFalse(sourceOutputDirectory.resolve("com/example/NotCreated.kt").exists())
    }

    @Test
    fun `filesystem writer cleans temporary files after staging failure`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        // Write initial artifact
        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/Existing.kt",
                    content = "old content",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        // Create a file where a directory is needed for artifact 2
        sourceOutputDirectory.resolve("com/example/sub").writeText("blocking file")

        // Artifact 1 will stage successfully, artifact 2 will fail
        assertFailsWith<IOException> {
            writer.write(
                GenerationResult.of(
                    GeneratedArtifact(
                        relativePath = "com/example/Existing.kt",
                        content = "new content",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                    GeneratedArtifact(
                        relativePath = "com/example/sub/deeper/New.kt",
                        content = "should fail",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                ),
            )
        }

        // No temporary files should remain in the output tree
        val tempFiles = java.nio.file.Files.walk(sourceOutputDirectory.toPath()).use { paths ->
            paths
                .filter { path ->
                    path.fileName.toString().startsWith(".asyncapi-generator-") && path.fileName.toString().endsWith(".tmp")
                }
                .toList()
        }
        assertTrue(tempFiles.isEmpty(), "Found orphan temp files: $tempFiles")
    }

    @Test
    fun `filesystem writer rejects duplicate destinations before creating directories`() {
        val sourceOutputDirectory = tempDir.resolve("sources").toFile()
        val resourceOutputDirectory = tempDir.resolve("resources").toFile()
        val writer = FileSystemGeneratedArtifactWriter(sourceOutputDirectory, resourceOutputDirectory)

        assertFailsWith<GeneratedArtifactCollision> {
            writer.write(
                GenerationResult.of(
                    GeneratedArtifact(
                        relativePath = "com/example/User.kt",
                        content = "first",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                    GeneratedArtifact(
                        relativePath = "com/example/User.kt",
                        content = "second",
                        kind = GeneratedArtifactKind.SOURCE,
                    ),
                ),
            )
        }

        // Directories should not be created
        assertFalse(sourceOutputDirectory.exists())
    }
}
