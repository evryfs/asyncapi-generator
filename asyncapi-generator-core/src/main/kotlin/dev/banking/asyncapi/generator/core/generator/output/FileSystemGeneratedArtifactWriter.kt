package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Filesystem-backed writer for generated artifacts.
 *
 * Source artifacts are written under [sourceOutputDirectory]. Java source
 * artifacts are written under [javaSourceOutputDirectory]. Resource and schema
 * artifacts are written under [resourceOutputDirectory]. Bundled documents are
 * written to their explicitly configured files.
 *
 * All artifacts are staged in sibling temporary files before any destination is
 * changed. Staging uses the same parent directory as the destination to maximize
 * the chance of atomic move support. After staging, files with unchanged content
 * are skipped to preserve modification timestamps and avoid unnecessary
 * recompilation. Remaining files are committed individually using atomic move
 * when supported, falling back to non-atomic replacement otherwise.
 *
 * The writer guarantees:
 * - Atomic per-file replacement when the filesystem supports it.
 * - Best-effort replacement otherwise.
 * - No transaction covering the complete set of generated files.
 * - Existing unrelated files are never removed.
 * - Output from earlier executions is preserved.
 * - Temporary files are cleaned after normal success and failure.
 *
 * Abnormal process termination may leave sibling temporary files with the
 * `.asyncapi-generator-` prefix. The build tool's clean lifecycle removes them.
 */
class FileSystemGeneratedArtifactWriter(
    private val sourceOutputDirectory: File,
    private val resourceOutputDirectory: File,
    private val javaSourceOutputDirectory: File = sourceOutputDirectory,
) : GeneratedArtifactWriter {

    override fun write(result: GenerationResult) {
        val outputs = resolveOutputs(result)
        if (outputs.isEmpty()) return

        rejectOutputCollisions(outputs)

        val stagedFiles = mutableListOf<StagedFile>()
        try {
            for (output in outputs) {
                stagedFiles.add(stageFile(output))
            }

            val toCommit = removeUnchanged(stagedFiles)
            for (staged in toCommit) {
                commitFile(staged)
            }
        } finally {
            cleanupTempFiles(stagedFiles)
        }
    }

    private fun resolveOutputs(result: GenerationResult): List<ResolvedOutput> =
        result.artifacts.map { artifact ->
            ResolvedOutput(
                file = outputFile(artifact),
                content = artifact.content,
                description = "${artifact.kind}: ${artifact.relativePath}",
            )
        } + result.documentArtifacts.map { artifact ->
            ResolvedOutput(
                file = artifact.file,
                content = artifact.content,
                description = "BUNDLED_DOCUMENT: ${artifact.file.path}",
            )
        }

    private fun rejectOutputCollisions(outputs: List<ResolvedOutput>) {
        val collision =
            outputs
                .groupBy { output -> output.file.toPath().toAbsolutePath().normalize() }
                .entries
                .firstOrNull { (_, collidingOutputs) -> collidingOutputs.size > 1 }
                ?: return

        throw GeneratedArtifactCollision(
            destination = collision.key.toString(),
            artifacts = collision.value.map(ResolvedOutput::description),
        )
    }

    private fun stageFile(output: ResolvedOutput): StagedFile {
        val destination = output.file.toPath().toAbsolutePath().normalize()
        Files.createDirectories(destination.parent)

        val tempFile = Files.createTempFile(
            destination.parent,
            ".asyncapi-generator-",
            ".tmp",
        )

        try {
            Files.writeString(tempFile, output.content)
        } catch (ex: Exception) {
            try {
                Files.deleteIfExists(tempFile)
            } catch (cleanupEx: Exception) {
                ex.addSuppressed(cleanupEx)
            }
            throw IOException(
                "Failed to stage artifact '${output.description}' to '${destination}'",
                ex,
            )
        }

        return StagedFile(tempFile, destination, output.description)
    }

    private fun removeUnchanged(stagedFiles: MutableList<StagedFile>): List<StagedFile> {
        val toCommit = mutableListOf<StagedFile>()
        val iterator = stagedFiles.iterator()

        while (iterator.hasNext()) {
            val staged = iterator.next()
            if (Files.exists(staged.destination) && Files.mismatch(staged.tempFile, staged.destination) == -1L) {
                try {
                    Files.delete(staged.tempFile)
                } catch (ex: IOException) {
                    throw IOException(
                        "Failed to clean unchanged staged file '${staged.tempFile}' for '${staged.description}'",
                        ex,
                    )
                }
                iterator.remove()
            } else {
                toCommit.add(staged)
            }
        }

        return toCommit
    }

    private fun commitFile(staged: StagedFile) {
        try {
            Files.move(
                staged.tempFile,
                staged.destination,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (ex: AtomicMoveNotSupportedException) {
            try {
                Files.move(
                    staged.tempFile,
                    staged.destination,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (moveEx: IOException) {
                throw IOException(
                    "Failed to commit artifact '${staged.description}' to '${staged.destination}'",
                    moveEx,
                )
            }
        } catch (ex: IOException) {
            throw IOException(
                "Failed to commit artifact '${staged.description}' to '${staged.destination}'",
                ex,
            )
        }
    }

    private fun cleanupTempFiles(stagedFiles: List<StagedFile>) {
        for (staged in stagedFiles) {
            try {
                Files.deleteIfExists(staged.tempFile)
            } catch (ex: IOException) {
                // Cleanup failure should not hide the original failure.
            }
        }
    }

    private fun outputFile(artifact: GeneratedArtifact): File {
        val outputRoot = outputRoot(artifact)
        return outputRoot.toPath().resolve(artifact.relativePath).toFile()
    }

    private fun outputRoot(artifact: GeneratedArtifact): File =
        when (artifact.kind) {
            GeneratedArtifactKind.SOURCE -> sourceOutputDirectory
            GeneratedArtifactKind.JAVA_SOURCE -> javaSourceOutputDirectory
            GeneratedArtifactKind.RESOURCE -> resourceOutputDirectory
            GeneratedArtifactKind.SCHEMA -> resourceOutputDirectory
        }

    private data class ResolvedOutput(
        val file: File,
        val content: String,
        val description: String,
    )

    private data class StagedFile(
        val tempFile: java.nio.file.Path,
        val destination: java.nio.file.Path,
        val description: String,
    )
}
