package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * The outcome of checking an instance value against a schema: what it violates and which parts could not be
 * checked.
 *
 * @property violations the constraints the value breaks
 * @property unsupportedFormats the schema formats the value could not be checked against
 */
internal data class InstanceEvaluation(
    val violations: List<Violation> = emptyList(),
    val unsupportedFormats: List<UnsupportedFormat> = emptyList(),
) {
    /**
     * A constraint a value breaks.
     *
     * @property path where the value sits in the instance
     * @property message what is wrong with it
     */
    data class Violation(
        val path: String,
        val message: String,
    )

    /**
     * A schema format no instance validator exists for.
     *
     * @property path where the value sits in the instance
     * @property schemaFormat the format the schema is written in
     */
    data class UnsupportedFormat(
        val path: String,
        val schemaFormat: String,
    )

    /** Whether the value broke no constraint and everything could be checked. */
    val valid: Boolean get() = violations.isEmpty() && unsupportedFormats.isEmpty()

    /** Whether the value broke no constraint but some part could not be checked. */
    val indeterminate: Boolean get() = violations.isEmpty() && unsupportedFormats.isNotEmpty()

    operator fun plus(other: InstanceEvaluation): InstanceEvaluation =
        InstanceEvaluation(violations + other.violations, unsupportedFormats + other.unsupportedFormats)

    companion object {
        /** An evaluation holding one violation at [path]. */
        fun violation(
            path: String,
            message: String,
        ) = InstanceEvaluation(listOf(Violation(path, message)))

        /** An evaluation holding the unsupported format of [schema] at [path]. */
        fun unsupported(
            path: String,
            schema: MultiFormatSchema,
        ) = InstanceEvaluation(unsupportedFormats = listOf(UnsupportedFormat(path, schema.schemaFormat)))
    }
}
