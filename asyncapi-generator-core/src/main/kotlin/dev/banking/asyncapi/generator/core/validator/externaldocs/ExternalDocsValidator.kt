package dev.banking.asyncapi.generator.core.validator.externaldocs

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ExternalDocsValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validateInterface(node: ExternalDocInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is ExternalDocInterface.ExternalDocInline -> {
                validate(node.externalDoc, contextString, results)
            }
            is ExternalDocInterface.ExternalDocReference -> {}
        }
    }

    fun validate(node: ExternalDoc, contextString: String, results: ValidationResults) {
        val url = node.url.let(::sanitizeString)
        if (url.isBlank()) {
            results.error(
                "$contextString 'url' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::url),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#externalDocumentationObject",
            )
        } else {
            if (!URL.matches(url)) {
                results.error(
                    "ExternalDoc '${contextString}' 'url' must be a valid absolute URL.",
                    asyncApiContext.getLine(node, node::url),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#externalDocumentationObject",
                )
            }
        }
    }
}
