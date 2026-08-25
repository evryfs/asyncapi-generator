package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Optional annotations applied to generated client contracts and their payload parameters.
 */
data class ClientValidationAnnotations(
    val clientContract: QualifiedTypeName? = null,
    val payloadParameter: QualifiedTypeName? = null,
)
