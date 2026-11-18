package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_EMAIL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CONTACT_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Contact Object.
 *
 * @param asyncApiContext the load the contacts belong to
 */
internal class ContactRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Contact> {
    override fun check(
        node: Contact,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val results = walk.results
        if (!results.visit(node)) return
        results.requireAbsoluteUri(
            node.url,
            "url",
            path,
            CONTACT_URL_FORMAT,
            asyncApiContext.modelTracking.getSourceLocation(node, node::url),
        )
        node.email?.let {
            if (!ValidationFormats.isEmailAddress(it)) {
                results.error(
                    CONTACT_EMAIL_FORMAT,
                    "$path 'email' field must be a valid email address.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::email),
                )
            }
        }
    }
}
