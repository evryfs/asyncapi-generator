package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Optional annotations applied to generated client contracts and their payload parameters.
 *
 * Expected behavior is covered by:
 * - `GeneratorConfigurationFactoryTest`
 * - `GenerationPlannerTest`
 * - `AsyncApiGeneratorMojoTest`
 */
data class ClientValidationAnnotations(
    val clientContract: QualifiedTypeName? = null,
    val payloadParameter: QualifiedTypeName? = null,
)
