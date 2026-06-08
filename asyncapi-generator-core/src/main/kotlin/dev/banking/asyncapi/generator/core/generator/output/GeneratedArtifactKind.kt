package dev.banking.asyncapi.generator.core.generator.output

/**
 * High-level destination category for generated artifacts.
 *
 * Expected behavior is covered by:
 * - `GenerationOutputContractTest`
 */
enum class GeneratedArtifactKind {
    SOURCE,
    RESOURCE,
    SCHEMA,
}
