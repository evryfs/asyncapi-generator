package dev.banking.asyncapi.generator.core.generator.output

/**
 * Persists generated artifacts after rendering has completed.
 */
fun interface GeneratedArtifactWriter {
    fun write(result: GenerationResult)
}
