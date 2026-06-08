package dev.banking.asyncapi.generator.core.generator.output

/**
 * Persists generated artifacts after rendering has completed.
 *
 * Expected behavior is covered by:
 * - `GeneratedArtifactWriterTest`
 */
fun interface GeneratedArtifactWriter {
    fun write(result: GenerationResult)
}
