package dev.banking.asyncapi.generator.core.generator.output

/**
 * Result of a generator run before rooted artifacts and explicit bundled
 * documents are written to disk.
 */
data class GenerationResult(
    val artifacts: List<GeneratedArtifact>,
    val documentArtifacts: List<GeneratedDocumentArtifact> = emptyList(),
) {
    fun isEmpty(): Boolean = artifacts.isEmpty() && documentArtifacts.isEmpty()

    fun artifactsOfKind(kind: GeneratedArtifactKind): List<GeneratedArtifact> =
        artifacts.filter { it.kind == kind }

    operator fun plus(other: GenerationResult): GenerationResult =
        GenerationResult(
            artifacts = artifacts + other.artifacts,
            documentArtifacts = documentArtifacts + other.documentArtifacts,
        )

    companion object {
        val Empty = GenerationResult(emptyList())

        fun of(vararg artifacts: GeneratedArtifact): GenerationResult =
            GenerationResult(artifacts.toList())

        fun of(documentArtifact: GeneratedDocumentArtifact): GenerationResult =
            GenerationResult(
                artifacts = emptyList(),
                documentArtifacts = listOf(documentArtifact),
            )
    }
}
