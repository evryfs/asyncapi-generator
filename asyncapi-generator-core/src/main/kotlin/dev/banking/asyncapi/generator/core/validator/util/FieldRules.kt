package dev.banking.asyncapi.generator.core.validator.util

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.source.SourceLocation
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath

/**
 * Reports [rule] when the required [field] of the object [path] names is missing or blank.
 *
 * @return whether the field has a value
 */
internal fun ValidationCollector.requireNonBlank(
    value: String?,
    field: String,
    path: ValidationPath,
    rule: ValidationRule,
    location: SourceLocation?,
): Boolean {
    val present = !value.isNullOrBlank()
    if (!present) error(rule, "$path '$field' is required and cannot be empty.", location)
    return present
}

/** Reports [rule] when [value], the [field] of the object [path] names, is present but not an absolute URI. */
internal fun ValidationCollector.requireAbsoluteUri(
    value: String?,
    field: String,
    path: ValidationPath,
    rule: ValidationRule,
    location: SourceLocation?,
) {
    if (value != null && ValidationFormats.absoluteUri(value) == null) {
        error(rule, "$path '$field' must be an absolute URL, found '$value'.", location)
    }
}
