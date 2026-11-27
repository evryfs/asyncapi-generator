package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Writes generated artifacts to the filesystem, preserving outputs from earlier executions.
 *
 * Sources and schemas are written under [outputDirectory] at their relative paths. Bundled
 * documents are written to their explicitly configured files.
 *
 * Writing has three steps. Every file is first staged next to its destination; nothing is
 * replaced until every file is staged. Staged files whose content equals their destination are
 * then left out, so unchanged files keep their timestamps. The rest are committed, each replacing
 * its destination.
 *
 * The writer guarantees:
 * - Atomic per-file replacement when the filesystem supports it.
 * - Best-effort replacement otherwise.
 * - No transaction covering the complete set of generated files.
 * - Existing unrelated files are never removed.
 * - Output from earlier executions is preserved. With a [manifest], files this execution generated
 *   before but no longer generates are removed, unless another execution still generates them.
 * - Temporary files are cleaned after normal success and failure.
 *
 * Abnormal process termination may leave sibling temporary files with the
 * `.asyncapi-generator-` prefix. Build-tool clean removes them when the
 * destination is under a cleaned build directory. Bundled documents targeting
 * arbitrary locations outside build output may require manual cleanup.
 *
 * @param outputDirectory root directory of generated sources and schemas
 * @param manifest records generated files across runs; without it, stale files are kept
 */
internal class FileSystemGeneratedArtifactWriter(
    private val outputDirectory: File,
    private val manifest: GeneratedOutputManifest? = null,
) {
    fun write(result: GenerationResult) {
        val outputs = resolveOutputs(result)
        if (outputs.isEmpty()) return

        rejectOutputCollisions(outputs)

        val stagedFiles = mutableListOf<StagedFile>()
        // Every failure is rethrown; cleanup failures are attached to it.
        val outcome =
            runCatching {
                for (output in outputs) {
                    // Recorded before it is written, so a failed write still leaves it to the cleanup.
                    val staged = reserve(output)
                    stagedFiles += staged
                    fill(staged, output.content)
                }
                stagedFiles.filterNot(::isUnchanged).forEach(::commit)
                manifest?.update(result.artifacts.map(::outputFile), listOf(outputDirectory))
            }
        val cleanupFailure = deleteTemporaryFiles(stagedFiles)
        outcome.exceptionOrNull()?.let { failure ->
            cleanupFailure?.let(failure::addSuppressed)
            throw failure
        }
        cleanupFailure?.let { throw it }
    }

    private fun resolveOutputs(result: GenerationResult): List<ResolvedOutput> =
        result.artifacts.map { artifact ->
            ResolvedOutput(
                file = outputFile(artifact),
                content = artifact.content,
                description = "${artifact.kind}: ${artifact.relativePath}",
            )
        } +
            result.documentArtifacts.map { artifact ->
                ResolvedOutput(
                    file = artifact.file,
                    content = artifact.content,
                    description = "BUNDLED_DOCUMENT: ${artifact.file.path}",
                )
            }

    private fun rejectOutputCollisions(outputs: List<ResolvedOutput>) {
        val collision =
            outputs
                .groupBy(ResolvedOutput::destination)
                .entries
                .firstOrNull { (_, collidingOutputs) -> collidingOutputs.size > 1 }
                ?: return

        throw GeneratedArtifactCollision(
            destination = collision.key.toString(),
            artifacts = collision.value.map(ResolvedOutput::description),
        )
    }

    /** Creates the temporary file next to the destination of [output] that will hold its content. */
    private fun reserve(output: ResolvedOutput): StagedFile {
        val destination = output.destination
        val tempFile =
            failingWith({ stagingFailure(output.description, destination) }) {
                Files.createDirectories(destination.parent)
                Files.createTempFile(destination.parent, TEMPORARY_FILE_PREFIX, ".tmp")
            }
        return StagedFile(tempFile, destination, output.description)
    }

    private fun fill(
        staged: StagedFile,
        content: String,
    ) {
        failingWith({ stagingFailure(staged.description, staged.destination) }) {
            Files.writeString(staged.tempFile, content)
        }
    }

    private fun stagingFailure(
        description: String,
        destination: Path,
    ): String = "Failed to stage artifact '$description' to '$destination'"

    private fun isUnchanged(staged: StagedFile): Boolean =
        failingWith({ "Failed to compare staged file with destination for '${staged.description}'" }) {
            Files.exists(staged.destination) && Files.mismatch(staged.tempFile, staged.destination) == -1L
        }

    private fun commit(staged: StagedFile) {
        failingWith({ "Failed to commit artifact '${staged.description}' to '${staged.destination}'" }) {
            try {
                Files.move(
                    staged.tempFile,
                    staged.destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (ignored: AtomicMoveNotSupportedException) {
                // The file system cannot replace atomically; replacing in place is the documented fallback.
                Files.move(staged.tempFile, staged.destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    /** Deletes every temporary file that is left; the failure, if any, names the first file that stayed. */
    private fun deleteTemporaryFiles(stagedFiles: List<StagedFile>): IOException? {
        val failures =
            stagedFiles.mapNotNull { staged ->
                runCatching { Files.deleteIfExists(staged.tempFile) }.exceptionOrNull()?.let { staged to it }
            }
        val (firstFile, firstFailure) = failures.firstOrNull() ?: return null
        return IOException("Failed to clean temporary file for '${firstFile.description}'", firstFailure).apply {
            failures.drop(1).forEach { (_, failure) -> addSuppressed(failure) }
        }
    }

    private fun outputFile(artifact: GeneratedArtifact): File = outputDirectory.resolve(artifact.relativePath)

    /**
     * One file to write.
     *
     * @param file the destination file
     * @param content the content to write
     * @param description the artifact, as named in error messages
     */
    private data class ResolvedOutput(
        val file: File,
        val content: String,
        val description: String,
    ) {
        val destination: Path
            get() = file.toPath().toAbsolutePath().normalize()
    }

    /**
     * Content written to a temporary file, waiting to replace its destination.
     *
     * @param tempFile the temporary file holding the content
     * @param destination the file it replaces
     * @param description the artifact, as named in error messages
     */
    private data class StagedFile(
        val tempFile: Path,
        val destination: Path,
        val description: String,
    )

    private companion object {
        const val TEMPORARY_FILE_PREFIX = ".asyncapi-generator-"
    }
}

/** Runs [action], wrapping a file system failure in one that says what was being written. */
private inline fun <T> failingWith(
    message: () -> String,
    action: () -> T,
): T =
    try {
        action()
    } catch (exception: IOException) {
        throw IOException(message(), exception)
    }
