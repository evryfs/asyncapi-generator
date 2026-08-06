package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import java.io.File

/**
 * Filesystem-backed writer for generated artifacts.
 *
 * Source artifacts are written under [sourceOutputDirectory]. Java source
 * artifacts are written under [javaSourceOutputDirectory]. Resource and schema
 * artifacts are written under [resourceOutputDirectory]. Bundled documents are
 * written to their explicitly configured files.
 *
 * Expected behavior is covered by:
 * - `GeneratedArtifactWriterTest`
 */
class FileSystemGeneratedArtifactWriter(
    private val sourceOutputDirectory: File,
    private val resourceOutputDirectory: File,
    private val javaSourceOutputDirectory: File = sourceOutputDirectory,
) : GeneratedArtifactWriter {
    override fun write(result: GenerationResult) {
        val outputs = resolveOutputs(result)
        rejectOutputCollisions(outputs)
        outputs.forEach(::writeOutput)
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
                .groupBy { output -> output.file.toPath().toAbsolutePath().normalize() }
                .entries
                .firstOrNull { (_, collidingOutputs) -> collidingOutputs.size > 1 }
                ?: return

        throw GeneratedArtifactCollision(
            destination = collision.key.toString(),
            artifacts = collision.value.map(ResolvedOutput::description),
        )
    }

    private fun writeOutput(output: ResolvedOutput) {
        output.file.parentFile?.mkdirs()
        output.file.writeText(output.content)
    }

    private fun outputFile(artifact: GeneratedArtifact): File {
        val outputRoot =
            when (artifact.kind) {
                GeneratedArtifactKind.SOURCE -> sourceOutputDirectory
                GeneratedArtifactKind.JAVA_SOURCE -> javaSourceOutputDirectory
                GeneratedArtifactKind.RESOURCE -> resourceOutputDirectory
                GeneratedArtifactKind.SCHEMA -> resourceOutputDirectory
            }
        return outputRoot.toPath().resolve(artifact.relativePath).toFile()
    }

    private data class ResolvedOutput(
        val file: File,
        val content: String,
        val description: String,
    )
}
