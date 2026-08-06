package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import java.io.File

/**
 * Filesystem-backed writer for generated artifacts.
 *
 * Source artifacts are written under [sourceOutputDirectory]. Java source
 * artifacts are written under [javaSourceOutputDirectory]. Resource and schema
 * artifacts are written under [resourceOutputDirectory].
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
        rejectOutputCollisions(result)
        result.artifacts.forEach(::writeArtifact)
    }

    private fun rejectOutputCollisions(result: GenerationResult) {
        val collision =
            result.artifacts
                .groupBy { artifact -> outputFile(artifact).toPath().toAbsolutePath().normalize() }
                .entries
                .firstOrNull { (_, artifacts) -> artifacts.size > 1 }
                ?: return

        throw GeneratedArtifactCollision(
            destination = collision.key.toString(),
            artifacts = collision.value.map { artifact -> "${artifact.kind}: ${artifact.relativePath}" },
        )
    }

    private fun writeArtifact(artifact: GeneratedArtifact) {
        val outputFile = outputFile(artifact)
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(artifact.content)
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
}
