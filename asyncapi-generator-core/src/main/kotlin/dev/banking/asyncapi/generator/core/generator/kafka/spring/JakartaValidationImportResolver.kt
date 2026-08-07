package dev.banking.asyncapi.generator.core.generator.kafka.spring

/** Resolves imports for Jakarta Validation annotations emitted on Kafka contract parameters. */
internal object JakartaValidationImportResolver {
    private val importsByAnnotation =
        mapOf(
            "DecimalMax" to "jakarta.validation.constraints.DecimalMax",
            "DecimalMin" to "jakarta.validation.constraints.DecimalMin",
            "Email" to "jakarta.validation.constraints.Email",
            "Max" to "jakarta.validation.constraints.Max",
            "Min" to "jakarta.validation.constraints.Min",
            "NotNull" to "jakarta.validation.constraints.NotNull",
            "Pattern" to "jakarta.validation.constraints.Pattern",
            "Size" to "jakarta.validation.constraints.Size",
        )

    fun resolve(annotations: Iterable<String>): Set<String> =
        annotations
            .mapNotNull { annotation -> importsByAnnotation[annotation.simpleName()] }
            .toSet()

    private fun String.simpleName(): String =
        trim()
            .removePrefix("@")
            .substringAfter(':')
            .substringBefore('(')
}
