package dev.banking.asyncapi.generator.core.validator.externaldocs

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ExternalDocsValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validateMap(nodeMap: Map<String, ExternalDocInterface>, results: ValidationResults) {
        for ((externalDocName, node) in nodeMap) {
            val externalDoc = when (node) {
                is ExternalDocInterface.ExternalDocInline -> node.externalDoc
                is ExternalDocInterface.ExternalDocReference -> continue
            }
            validate(externalDoc, externalDocName, results)
        }
    }

    fun validate(node: ExternalDoc, externalDocName: String, results: ValidationResults) {
        val url = node.url.let(::sanitizeString)
        if (url.isBlank()) {
            results.error(
                "$externalDocName 'url' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::url)
            )
        } else {
            val urlRegex = Regex("""^(https?|wss?)://\S+$""")
            if (!urlRegex.matches(url)) {
                results.error(
                    "ExternalDoc '${externalDocName}' 'url' must be a valid absolute URL.",
                    asyncApiContext.getLine(node, node::url)
                )
            }
        }
        val description = node.description?.let(::sanitizeString)
            ?: return
        if (description.isBlank()) {
            results.warn(
                "ExternalDoc '${externalDocName}' 'description' is empty. Can be omitted if not used.",
                asyncApiContext.getLine(node, node::description)
            )
        }
    }
}
