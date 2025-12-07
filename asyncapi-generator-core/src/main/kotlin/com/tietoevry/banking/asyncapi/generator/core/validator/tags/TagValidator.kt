package com.tietoevry.banking.asyncapi.generator.core.validator.tags

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.Tag
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.resolver.ReferenceResolver
import com.tietoevry.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

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

    fun validate(node: Tag, tagName: String, results: ValidationResults) {
        val name = node.name.let(::sanitizeString)
        if (name.isBlank()) {
            results.error(
                "Tag '$tagName' 'name' is required and cannot be empty.",
                asyncApiContext.getLine(node, node::name)
            )
        }
        val description = node.description?.let(::sanitizeString)
        description?.let {
            if (it.length < 3) {
                results.warn(
                    "Tag '$tagName' 'description' seems too short.",
                    asyncApiContext.getLine(node, node::description)
                )
            }
        }
        validateExternalDocs(node, tagName, results)
    }

    private fun validateExternalDocs(node: Tag, tagName: String, results: ValidationResults) {
        val externalDocs = node.externalDocs ?: return
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, tagName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(tagName, externalDocs.reference, "Tag ExternalDocs", results)
        }
    }
}
