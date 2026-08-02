package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

class LicenseValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: License, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        val name = node.name
        if (name.isBlank()) {
            results.error(
                LICENSE_NAME_REQUIRED,
                "$contextString 'name' field is required and cannot be empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::name),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#licenseObject",
            )
        }
        node.url?.let { url ->
            if (ValidationFormats.absoluteUri(url) == null) {
                results.error(
                    LICENSE_URL_FORMAT,
                    "$contextString 'url' field must be a valid absolute URL.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::url),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#licenseObject",
                )
            }
        }
    }
}
