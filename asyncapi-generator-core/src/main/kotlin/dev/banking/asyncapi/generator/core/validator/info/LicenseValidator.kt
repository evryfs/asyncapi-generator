package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class LicenseValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: License, contextString: String, results: ValidationResults) {
        val name = node.name.let(::sanitizeString)
        if (name.isBlank()) {
            results.error(
                "$contextString 'name' field is required and cannot be empty.",
                asyncApiContext.getLine(node, node::name),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#licenseObject",
            )
        }
        val url = node.url?.let(::sanitizeString)
        url?.let {
            val url = it.trim().trim('"', '\'')
            if (!URL.matches(url)) {
                results.error(
                    "$contextString 'url' field must be a valid absolute URL.",
                    asyncApiContext.getLine(node, node::url),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#licenseObject",
                )
            }
        }
    }
}
