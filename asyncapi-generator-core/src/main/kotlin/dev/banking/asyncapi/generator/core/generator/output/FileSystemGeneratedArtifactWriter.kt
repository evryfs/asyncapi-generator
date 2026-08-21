package dev.banking.asyncapi.generator.core.generator.output

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.GeneratedArtifactCollision
import java.io.File
import java.nio.file.Files

/**
 * Filesystem-backed writer for generated artifacts.
 *
 * Source artifacts are written under [sourceOutputDirectory]. Java source
 * artifacts are written under [javaSourceOutputDirectory]. Resource and schema
 * artifacts are written under [resourceOutputDirectory]. Bundled documents are
 * written to their explicitly configured files.
 *
 * All regular artifacts are written to temporary staging directories first. Only
 * after every artifact is successfully staged are the old output directories
 * replaced atomically. This prevents partial writes from leaving an
 * inconsistent output directory and ensures stale files from previous runs are
 * removed.
 */
class FileSystemGeneratedArtifactWriter(
    private val sourceOutputDirectory: File,
    private val resourceOutputDirectory: File,
    private val javaSourceOutputDirectory: File = sourceOutputDirectory,
) : GeneratedArtifactWriter {
    override fun write(result: GenerationResult) {
        val outputs = resolveOutputs(result)
        rejectOutputCollisions(outputs)

        val regularOutputs = outputs.filter { !it.isDocument }
        val documentOutputs = outputs.filter { it.isDocument }

        writeStaged(regularOutputs)
        documentOutputs.forEach(::writeOutput)
    }

    private fun resolveOutputs(result: GenerationResult): List<ResolvedOutput> =
        result.artifacts.map { artifact ->
            ResolvedOutput(
                file = outputFile(artifact),
                content = artifact.content,
                description = "${artifact.kind}: ${artifact.relativePath}",
                isDocument = false,
            )
        } +
                result.documentArtifacts.map { artifact ->
                    ResolvedOutput(
                        file = artifact.file,
                        content = artifact.content,
                        description = "BUNDLED_DOCUMENT: ${artifact.file.path}",
                        isDocument = true,
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

    private fun writeStaged(outputs: List<ResolvedOutput>) {
        if (outputs.isEmpty()) return

        val outputsByRoot = outputs.groupBy { outputRoot(it.file) }
        val stagingDirs = mutableMapOf<File, File>()

        try {
            for ((root, rootOutputs) in outputsByRoot) {
                val stagingDir = createStagingDirectory(root)
                stagingDirs[root] = stagingDir
                for (output in rootOutputs) {
                    val relativePath = root.toPath().relativize(output.file.toPath())
                    val stagingFile = stagingDir.resolve(relativePath.toString())
                    stagingFile.parentFile?.mkdirs()
                    stagingFile.writeText(output.content)
                }
            }

            for ((root, stagingDir) in stagingDirs) {
                replaceDirectory(root, stagingDir)
            }
        } catch (ex: Exception) {
            stagingDirs.values.forEach { dir ->
                dir.deleteRecursively()
            }
            throw ex
        }
    }

    private fun createStagingDirectory(targetDir: File): File {
        val parent = targetDir.parentFile
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val stagingDir = Files.createTempDirectory(parent.toPath(), ".asyncapi-staging-")
        return stagingDir.toFile()
    }

    private fun replaceDirectory(
        targetDir: File,
        stagingDir: File,
    ) {
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        val renamed = stagingDir.renameTo(targetDir)
        if (!renamed) {
            throw IllegalStateException("Failed to replace directory: ${targetDir.path}")
        }
    }

    private fun writeOutput(output: ResolvedOutput) {
        output.file.parentFile?.mkdirs()
        output.file.writeText(output.content)
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

    private fun outputRoot(file: File): File {
        val absolutePath = file.toPath().toAbsolutePath()
        for (root in listOf(sourceOutputDirectory, javaSourceOutputDirectory, resourceOutputDirectory)) {
            if (absolutePath.startsWith(root.toPath().toAbsolutePath())) {
                return root
            }
        }
        return file.parentFile
    }

    private fun File.deleteRecursively() {
        if (isDirectory) {
            listFiles()?.forEach { it.deleteRecursively() }
        }
        delete()
    }

    private data class ResolvedOutput(
        val file: File,
        val content: String,
        val description: String,
        val isDocument: Boolean,
    )
}
