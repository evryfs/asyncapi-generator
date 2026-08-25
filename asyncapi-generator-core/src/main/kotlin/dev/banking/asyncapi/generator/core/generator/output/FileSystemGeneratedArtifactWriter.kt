package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Writes generated artifacts to the filesystem, preserving outputs from earlier executions.
 *
 * Source artifacts are written under [sourceOutputDirectory]. Java source
 * artifacts are written under [javaSourceOutputDirectory]. Resource and schema
 * artifacts are written under [resourceOutputDirectory]. Bundled documents are
 * written to their explicitly configured files.
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
 * `.asyncapi-generator-` prefix. Build-tool clean removes them when the
 * destination is under a cleaned build directory. Bundled documents targeting
 * arbitrary locations outside build output may require manual cleanup.
 *
 * @param sourceOutputDirectory root directory for Kotlin and general source artifacts
 * @param resourceOutputDirectory root directory for resource and schema artifacts
 * @param javaSourceOutputDirectory root directory for Java source artifacts
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
        var primaryFailure: Exception? = null
        try {
            for (output in outputs) {
                stagedFiles.add(stageFile(output))
            }

            val toCommit = removeUnchanged(stagedFiles)
            for (staged in toCommit) {
                commitFile(staged)
            }
        } catch (ex: Exception) {
            primaryFailure = ex
            throw ex
        } finally {
            cleanupTempFiles(stagedFiles, primaryFailure)
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
        var tempFile: java.nio.file.Path? = null

        try {
            Files.createDirectories(destination.parent)
            tempFile = Files.createTempFile(
                destination.parent,
                ".asyncapi-generator-",
                ".tmp",
            )
            Files.writeString(tempFile, output.content)
        } catch (ex: Exception) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile)
                } catch (cleanupEx: Exception) {
                    ex.addSuppressed(cleanupEx)
                }
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
            val unchanged = try {
                Files.exists(staged.destination) && Files.mismatch(staged.tempFile, staged.destination) == -1L
            } catch (ex: IOException) {
                throw IOException(
                    "Failed to compare staged file with destination for '${staged.description}'",
                    ex,
                )
            }

            if (unchanged) {
                try {
                    Files.delete(staged.tempFile)
                } catch (ex: IOException) {
                    throw IOException(
                        "Failed to clean unchanged staged file for '${staged.description}'",
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

    private fun cleanupTempFiles(stagedFiles: List<StagedFile>, primaryFailure: Exception?) {
        var cleanupFailure: IOException? = null
        for (staged in stagedFiles) {
            try {
                Files.deleteIfExists(staged.tempFile)
            } catch (ex: Exception) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(ex)
                } else if (cleanupFailure == null) {
                    cleanupFailure = IOException(
                        "Failed to clean temporary file for '${staged.description}'",
                        ex,
                    )
                } else {
                    cleanupFailure.addSuppressed(ex)
                }
            }
        }
        if (cleanupFailure != null) {
            throw cleanupFailure
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
