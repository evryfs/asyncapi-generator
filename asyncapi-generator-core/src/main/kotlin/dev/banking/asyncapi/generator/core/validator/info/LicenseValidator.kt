package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank

internal class LicenseValidator(
    val asyncApiContext: AsyncApiContext,
) {
    fun validate(
        node: License,
        contextString: String,
        results: ValidationCollector,
    ) {
        if (!results.visit(node)) return
        results.requireNonBlank(
            node.name,
            "name",
            contextString,
            LICENSE_NAME_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::name),
        )
        results.requireAbsoluteUri(
            node.url,
            "url",
            contextString,
            LICENSE_URL_FORMAT,
            asyncApiContext.modelTracking.getSourceLocation(node, node::url),
        )
    }
}
