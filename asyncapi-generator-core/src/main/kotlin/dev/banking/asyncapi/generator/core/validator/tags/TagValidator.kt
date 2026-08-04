package dev.banking.asyncapi.generator.core.validator.tags

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.TAG_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class TagValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: TagInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is TagInterface.TagInline -> validate(node.tag, contextString, results)
            is TagInterface.TagReference -> referenceResolver.resolve(node.reference, TAG, contextString, results)
        }
    }

    fun validateList(tags: List<TagInterface>?, contextString: String, results: ValidationCollector) {
        tags?.forEachIndexed { index, tagInterface ->
            validateInterface(tagInterface, "$contextString Tag[$index]", results)
        }
    }

    fun validate(node: Tag, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        val name = node.name
        if (name.isBlank()) {
            results.error(
                TAG_NAME_REQUIRED,
                "$contextString 'name' is required and cannot be empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::name),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#tagObject",
            )
        }
        validateExternalDocs(node, contextString, results)
    }

    private fun validateExternalDocs(node: Tag, contextString: String, results: ValidationCollector) {
        val externalDocs = node.externalDocs ?: return
        externalDocsValidator.validateInterface(externalDocs, "$contextString ExternalDocs", results)
    }
}
