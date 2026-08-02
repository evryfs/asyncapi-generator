package dev.banking.asyncapi.generator.core.validator.externaldocs

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.EXTERNAL_DOC_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.EXTERNAL_DOC_URL_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ExternalDocsValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validateInterface(node: ExternalDocInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is ExternalDocInterface.ExternalDocInline -> {
                validate(node.externalDoc, contextString, results)
            }
            is ExternalDocInterface.ExternalDocReference -> {}
        }
    }

    fun validate(node: ExternalDoc, contextString: String, results: ValidationCollector) {
        val url = node.url.let(::sanitizeString)
        if (url.isBlank()) {
            results.error(
                EXTERNAL_DOC_URL_REQUIRED,
                "$contextString 'url' is required and cannot be empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::url),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#externalDocumentationObject",
            )
        } else {
            if (!URL.matches(url)) {
                results.error(
                    EXTERNAL_DOC_URL_FORMAT,
                    "ExternalDoc '${contextString}' 'url' must be a valid absolute URL.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::url),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#externalDocumentationObject",
                )
            }
        }
    }
}
