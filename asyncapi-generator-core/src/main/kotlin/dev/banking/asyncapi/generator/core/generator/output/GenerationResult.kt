package dev.banking.asyncapi.generator.core.generator.output

/**
 * Result of a generator run before generated artifacts are written to disk.
 *
 * Expected behavior is covered by:
 * - `GenerationOutputContractTest`
 */
data class GenerationResult(
    val artifacts: List<GeneratedArtifact>,
) {
    fun isEmpty(): Boolean = artifacts.isEmpty()

    fun artifactsOfKind(kind: GeneratedArtifactKind): List<GeneratedArtifact> =
        artifacts.filter { it.kind == kind }

    operator fun plus(other: GenerationResult): GenerationResult =
        GenerationResult(artifacts + other.artifacts)

    companion object {
        val Empty = GenerationResult(emptyList())

        fun of(vararg artifacts: GeneratedArtifact): GenerationResult =
            GenerationResult(artifacts.toList())
    }
}
