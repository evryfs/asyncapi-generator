package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Optional annotations applied to generated client contracts and their payload parameters.
 *
 * @param clientContract the annotation placed on each generated contract.
 * @param payloadParameter the annotation placed on each payload parameter.
 */
data class ClientValidationAnnotations(
    val clientContract: QualifiedTypeName? = null,
    val payloadParameter: QualifiedTypeName? = null,
)
