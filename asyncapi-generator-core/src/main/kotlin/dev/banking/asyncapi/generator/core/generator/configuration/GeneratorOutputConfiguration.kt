package dev.banking.asyncapi.generator.core.generator.configuration

import java.io.File

/**
 * Output directories used by generated source and resource artifacts.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 * - `AsyncApiGeneratorOutputContractTest`
 */
data class GeneratorOutputConfiguration(
    val sourceOutputDirectory: File,
    val resourceOutputDirectory: File,
    val javaSourceOutputDirectory: File = sourceOutputDirectory,
    val document: DocumentOutput? = null,
)

/**
 * Bundled AsyncAPI document output selected by the generator profile.
 *
 * Expected behavior is covered by:
 * - `GenerationPlannerTest`
 * - `AsyncApiGeneratorOutputContractTest`
 */
data class DocumentOutput(
    val file: File,
    val format: DocumentFormat,
)
