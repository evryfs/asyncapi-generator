package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import java.nio.file.Path

/**
 * Places schema artifacts under a configured output package without changing
 * schema content or source artifact paths.
 */
internal fun GenerationResult.inSchemaPackage(packageName: String?): GenerationResult {
    if (packageName == null) {
        return this
    }

    return copy(
        artifacts =
            artifacts.map { artifact ->
                if (artifact.kind == GeneratedArtifactKind.SCHEMA) {
                    artifact.copy(
                        relativePath =
                            GeneratedArtifactPaths.fromNamespace(
                                namespace = packageName,
                                fileName = Path.of(artifact.relativePath).fileName.toString(),
                            ),
                    )
                } else {
                    artifact
                }
            },
    )
}
