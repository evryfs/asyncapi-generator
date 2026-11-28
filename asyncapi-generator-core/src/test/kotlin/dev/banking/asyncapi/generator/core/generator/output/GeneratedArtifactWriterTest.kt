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

internal class GeneratedArtifactWriterTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `filesystem writer writes artifacts under the output directory at their relative paths`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "data class User(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
                GeneratedArtifact(
                    relativePath = "com/example/schema/User.avsc",
                    content = """{"type":"record","name":"User"}""",
                    kind = GeneratedArtifactKind.SCHEMA,
                ),
            ),
        )

        assertEquals(
            "data class User(val id: String)",
            outputDirectory.resolve("com/example/User.kt").readText(),
        )
        assertEquals(
            """{"type":"record","name":"User"}""",
            outputDirectory.resolve("com/example/schema/User.avsc").readText(),
        )
    }

    @Test
    fun `filesystem writer writes bundled documents to their explicit destinations`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val documentOutputFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedDocumentArtifact(
                    file = documentOutputFile,
                    content = "asyncapi: 3.0.0\n",
                ),
            ),
        )

        assertEquals("asyncapi: 3.0.0\n", documentOutputFile.readText())
        assertFalse(outputDirectory.exists())
    }

    @Test
    fun `filesystem writer creates parent directories and writes all artifacts`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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

        assertEquals("source", outputDirectory.resolve("com/example/User.kt").readText())
        assertEquals("schema", outputDirectory.resolve("com/example/schema/User.avsc").readText())
    }

    @Test
    fun `filesystem writer ignores empty generation result`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

        writer.write(GenerationResult.Empty)

        assertFalse(outputDirectory.exists())
    }

    @Test
    fun `filesystem writer rejects destination collisions before writing artifacts`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
                            kind = GeneratedArtifactKind.SOURCE,
                        ),
                    ),
                )
            }

        assertEquals(2, Regex("SOURCE: com/example/User.java").findAll(error.message!!).count())
        assertFalse(outputDirectory.exists())
    }

    @Test
    fun `filesystem writer rejects artifact and bundled document destination collisions before writing`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val destination = outputDirectory.resolve("com/example/User.kt")
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
        assertFalse(outputDirectory.exists())
    }

    @Test
    fun `filesystem writer rejects matching bundled document destinations before writing`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val destination = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
    fun `filesystem writer preserves artifacts from earlier executions`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
            outputDirectory.resolve("com/example/ContractA.kt").readText(),
        )
        assertEquals(
            "data class ContractB(val id: String)",
            outputDirectory.resolve("com/example/ContractB.kt").readText(),
        )
    }

    @Test
    fun `filesystem writer preserves unrelated files`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

        outputDirectory.mkdirs()
        outputDirectory.resolve("unrelated.txt").writeText("keep me")

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "data class User(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        assertEquals("keep me", outputDirectory.resolve("unrelated.txt").readText())
        assertEquals(
            "data class User(val id: String)",
            outputDirectory.resolve("com/example/User.kt").readText(),
        )
    }

    @Test
    fun `filesystem writer replaces existing file when content changes`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
            outputDirectory.resolve("com/example/User.kt").readText(),
        )
    }

    @Test
    fun `filesystem writer preserves timestamp when content is identical`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

        writer.write(
            GenerationResult.of(
                GeneratedArtifact(
                    relativePath = "com/example/User.kt",
                    content = "data class User(val id: String)",
                    kind = GeneratedArtifactKind.SOURCE,
                ),
            ),
        )

        val file = outputDirectory.resolve("com/example/User.kt")
        val knownTimestamp =
            java.nio.file.attribute.FileTime
                .fromMillis(1_000_000_000L)
        java.nio.file.Files
            .setLastModifiedTime(file.toPath(), knownTimestamp)

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
            knownTimestamp,
            java.nio.file.Files
                .getLastModifiedTime(file.toPath()),
        )
    }

    @Test
    fun `filesystem writer bundles documents through same staging path`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val documentFile = tempDir.resolve("bundled/asyncapi.yaml").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

        // Create a file where a directory is needed
        outputDirectory.mkdirs()
        outputDirectory.resolve("com").writeText("blocking file")

        val error =
            assertFailsWith<IOException> {
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
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
        outputDirectory.resolve("com/example/sub").writeText("blocking file")

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
        assertEquals("old content", outputDirectory.resolve("com/example/Existing.kt").readText())
        // Artifact 3 was never staged or created
        assertFalse(outputDirectory.resolve("com/example/NotCreated.kt").exists())
    }

    @Test
    fun `filesystem writer cleans temporary files after staging failure`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
        outputDirectory.resolve("com/example/sub").writeText("blocking file")

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
        val tempFiles =
            java.nio.file.Files.walk(outputDirectory.toPath()).use { paths ->
                paths
                    .filter { path ->
                        path.fileName.toString().startsWith(".asyncapi-generator-") &&
                            path.fileName.toString().endsWith(".tmp")
                    }.toList()
            }
        assertTrue(tempFiles.isEmpty(), "Found orphan temp files: $tempFiles")
    }

    @Test
    fun `filesystem writer rejects duplicate destinations before creating directories`() {
        val outputDirectory = tempDir.resolve("output").toFile()
        val writer = FileSystemGeneratedArtifactWriter(outputDirectory)

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
        assertFalse(outputDirectory.exists())
    }
}
