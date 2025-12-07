package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class LicenseValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: License, results: ValidationResults) {
        val name = node.name.let(::sanitizeString)
        if (name.isBlank()) {
            results.error(
                "The 'name' field in the License object is required and cannot be empty.",
                asyncApiContext.getLine(node, node::name)
            )
        }
        val url = node.url?.let(::sanitizeString)
        url?.let {
            val url = it.trim().trim('"', '\'')
            val urlRegex = Regex("""^(https?|wss?)://\S+$""")
            if (!urlRegex.matches(url)) {
                results.error(
                    "The 'url' field in the License object must be a valid absolute URL.",
                    asyncApiContext.getLine(node, node::url)
                )
            }
        }
    }
}
