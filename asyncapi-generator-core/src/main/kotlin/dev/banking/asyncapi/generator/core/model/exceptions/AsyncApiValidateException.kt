package dev.banking.asyncapi.generator.core.model.exceptions

import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding

/** A failure of an AsyncAPI contract to pass validation. */
internal sealed class AsyncApiValidateException(
    message: String,
) : Exception(message) {
    /**
     * Validation found [errors].
     *
     * @property errors the error findings
     */
    class ValidateError internal constructor(
        val errors: List<ValidationFinding>,
        message: String,
    ) : AsyncApiValidateException(message)
}
