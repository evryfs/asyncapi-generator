package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_TRAIT_EMPTY
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.correlations.CorrelationIdValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

class MessageTraitValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val schemaValidator = SchemaValidator(asyncApiContext)
    private val correlationIdValidator = CorrelationIdValidator(asyncApiContext)
    private val tagValidator = TagValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val messageExampleValidator = MessageExampleValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: MessageTraitInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is MessageTraitInterface.InlineMessageTrait ->
                validate(node.trait, contextString, results)

            is MessageTraitInterface.ReferenceMessageTrait ->
                referenceResolver.resolve(node.reference, MESSAGE_TRAIT, contextString, results)
        }
    }

    fun validate(node: MessageTrait, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateMeaningfulContent(node, contextString, results)
        validateHeaders(node, contextString, results)
        validateContentType(node, contextString, results)
        validateExamples(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)

        node.correlationId?.let { correlationIdValidator.validateInterface(it, contextString, results) }
    }

    private fun validateMeaningfulContent(node: MessageTrait, contextString: String, results: ValidationCollector) {
        if (
            node.headers == null &&
            node.correlationId == null &&
            node.contentType == null &&
            node.name == null &&
            node.title == null &&
            node.summary == null &&
            node.description == null &&
            node.tags == null &&
            node.externalDocs == null &&
            node.bindings == null &&
            node.examples == null
        ) {
            results.warn(
                MESSAGE_TRAIT_EMPTY,
                "$contextString does not define any fields and has no effect.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::headers),
            )
        }
    }

    private fun validateHeaders(node: MessageTrait, contextString: String, results: ValidationCollector) {
        val headersSchema = node.headers ?: return
        schemaValidator.validateInterface(headersSchema, "$contextString Headers", results)
    }

    private fun validateContentType(node: MessageTrait, contextString: String, results: ValidationCollector) {
        val contentType = node.contentType ?: return
        if (!ValidationFormats.isSpecificMediaType(contentType)) {
            results.error(
                MESSAGE_CONTENT_TYPE_FORMAT,
                "$contextString has invalid 'contentType' value '$contentType'. Expected a specific media type.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::contentType),
            )
        }
    }

    private fun validateExamples(node: MessageTrait, contextString: String, results: ValidationCollector) {
        node.examples?.let { examples ->
            messageExampleValidator.validate(examples, contextString, results)
        }
    }

    private fun validateTags(node: MessageTrait, contextString: String, results: ValidationCollector) {
        val tags = node.tags ?: return
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "$contextString Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, TAG, contextString, results)
            }
        }
    }

    private fun validateExternalDocs(node: MessageTrait, contextString: String, results: ValidationCollector) {
        val externalDocs = node.externalDocs ?: return
        val contextString = "$contextString ExternalDocs"
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(externalDocs.reference, EXTERNAL_DOC, contextString, results)
        }
    }

    private fun validateBindings(node: MessageTrait, contextString: String, results: ValidationCollector) {
        val bindings = node.bindings ?: return
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, BINDING, contextString, results)
            }
        }
    }

}
