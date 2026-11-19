package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_EMAIL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri

internal class ContactValidator(
    val asyncApiContext: AsyncApiContext,
) {
    fun validate(
        node: Contact,
        contextString: String,
        results: ValidationCollector,
    ) {
        if (!results.visit(node)) return
        val url = node.url
        val email = node.email
        results.requireAbsoluteUri(
            url,
            "url",
            contextString,
            CONTACT_URL_FORMAT,
            asyncApiContext.modelTracking.getSourceLocation(node, node::url),
        )
        email?.let {
            if (!ValidationFormats.isEmailAddress(it)) {
                results.error(
                    CONTACT_EMAIL_FORMAT,
                    "$contextString 'email' field must be a valid email address.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::email),
                )
            }
        }
    }
}
