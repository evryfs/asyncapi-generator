package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.EMAIL
import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_EMAIL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ContactValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: Contact, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        val name = node.name?.let(::sanitizeString)
        val url = node.url?.let(::sanitizeString)
        val email = node.email?.let(::sanitizeString)
        if (name.isNullOrBlank() && url.isNullOrBlank() && email.isNullOrBlank()) {
            results.warn(
                CONTACT_EMPTY,
                "$contextString is defined but all its fields are empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::name),
            )
            return
        }
        url?.let {
            if (!URL.matches(it)) {
                results.error(
                    CONTACT_URL_FORMAT,
                    "$contextString 'url' field must be a valid absolute URL.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::url),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#contactObject",
                )
            }
        }
        email?.let {
            if (!EMAIL.matches(it)) {
                results.error(
                    CONTACT_EMAIL_FORMAT,
                    "$contextString 'email' field must be a valid email address.",
                    sourceLocation = asyncApiContext.getSourceLocation(node, node::email),
                    doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#contactObject",
                )
            }
        }
    }
}
