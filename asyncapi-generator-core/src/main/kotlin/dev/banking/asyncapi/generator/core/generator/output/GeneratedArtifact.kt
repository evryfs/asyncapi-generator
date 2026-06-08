package dev.banking.asyncapi.generator.core.generator.output

import java.nio.file.Path

/**
 * Generated file-like output produced before it is written to disk.
 *
 * [GeneratedArtifact] is the generator output boundary. It keeps rendered
 * content together with the relative path and artifact kind that a writer can
 * later use to persist the artifact.
 *
 * Expected behavior is covered by:
 * - `GenerationOutputContractTest`
 */
data class GeneratedArtifact(
    val relativePath: String,
    val content: String,
    val kind: GeneratedArtifactKind,
) {
    init {
        require(relativePath.isNotBlank()) {
            "Generated artifact relative path cannot be blank"
        }
        require(!Path.of(relativePath).isAbsolute) {
            "Generated artifact path must be relative: $relativePath"
        }
        require(Path.of(relativePath).none { it.toString() == ".." }) {
            "Generated artifact path cannot contain parent directory segments: $relativePath"
        }
    }
}
