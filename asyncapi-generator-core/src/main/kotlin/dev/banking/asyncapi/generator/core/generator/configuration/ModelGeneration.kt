package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Typed model artifact generation configuration.
 */
sealed interface ModelGeneration {
    data object Disabled : ModelGeneration

    data class Enabled(
        val packageName: String,
        val annotation: QualifiedTypeName? = null,
        val javaModelType: JavaModelType = JavaModelType.CLASS,
    ) : ModelGeneration
}
