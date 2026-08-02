package dev.banking.asyncapi.generator.core.model.validator

/** The authority or purpose behind a validation rule. */
enum class ValidationConcern {
    SPECIFICATION,
    GENERATOR_CAPABILITY,
    ADVISORY,
}
