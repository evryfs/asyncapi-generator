package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.EMAIL
import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ContactValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: Contact, results: ValidationResults) {
        val name = node.name?.let(::sanitizeString)
        val url = node.url?.let(::sanitizeString)
        val email = node.email?.let(::sanitizeString)
        if (name.isNullOrBlank() && url.isNullOrBlank() && email.isNullOrBlank()) {
            results.warn(
                "The Contact object is defined but all its fields are empty.",
                asyncApiContext.getLine(node, node::name)
            )
            return
        }
        url?.let {
            if (!URL.matches(it)) {
                results.error(
                    "The 'url' field in the Contact object must be a valid absolute URL.",
                    asyncApiContext.getLine(node, node::url),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#contactObject",
                )
            }
        }
        email?.let {
            if (!EMAIL.matches(it)) {
                results.error(
                    "The 'email' field in the Contact object must be a valid email address.",
                    asyncApiContext.getLine(node, node::email),
                    "https://www.asyncapi.com/docs/reference/specification/v3.0.0#contactObject",
                )
            }
        }
    }
}
