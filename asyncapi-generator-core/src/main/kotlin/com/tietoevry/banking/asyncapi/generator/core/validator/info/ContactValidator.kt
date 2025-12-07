package com.tietoevry.banking.asyncapi.generator.core.validator.info

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.info.Contact
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

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
        }
        url?.let {
            val urlRegex = Regex("""^(https?)://\S+$""")
            if (!urlRegex.matches(it)) {
                results.error(
                    "The 'url' field in the Contact object must be a valid absolute URL.",
                    asyncApiContext.getLine(node, node::url)
                )
            }
        }
        email?.let {
            val emailRegex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
            if (!emailRegex.matches(it)) {
                results.error(
                    "The 'email' field in the Contact object must be a valid email address.",
                    asyncApiContext.getLine(node, node::email)
                )
            }
        }
    }
}
