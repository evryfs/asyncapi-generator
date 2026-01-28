package dev.banking.asyncapi.generator.core.validator.tags

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class TagValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateMap(nodes: Map<String, TagInterface>, results: ValidationResults) {
        nodes.forEach { (name, node) ->
            val tag = (node as TagInterface.TagInline).tag
            validate(tag, name, results)
        }
    }

    fun validate(node: Tag, contextString: String, results: ValidationResults) {
        val name = node.name.let(::sanitizeString)
        if (name.isBlank()) {
            results.error(
                "$contextString 'name' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::name),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#tagObject"
            )
        }
        validateExternalDocs(node, contextString, results)
    }

    private fun validateExternalDocs(node: Tag, contextString: String, results: ValidationResults) {
        val externalDocs = node.externalDocs ?: return
        val contextString = "$contextString ExternalDocs"
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, contextString, results)
            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(externalDocs.reference, contextString, results)
        }
    }
}
