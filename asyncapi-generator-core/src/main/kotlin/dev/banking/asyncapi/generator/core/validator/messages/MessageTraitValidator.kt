package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.MIME_TYPE
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.correlations.CorrelationIdValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class MessageTraitValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val schemaValidator = SchemaValidator(asyncApiContext)
    private val correlationIdValidator = CorrelationIdValidator(asyncApiContext)
    private val tagValidator = TagValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: MessageTraitInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is MessageTraitInterface.InlineMessageTrait ->
                validate(node.trait, contextString, results)

            is MessageTraitInterface.ReferenceMessageTrait ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validate(node: MessageTrait, contextString: String, results: ValidationResults) {
        validateMeaningfulContent(node, contextString, results)
        validateHeaders(node, contextString, results)
        validateContentType(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)

        node.correlationId?.let { correlationIdValidator.validateInterface(it, contextString, results) }
    }

    private fun validateMeaningfulContent(node: MessageTrait, contextString: String, results: ValidationResults) {
        if (node.headers == null && node.bindings == null && node.correlationId == null && node.contentType == null) {
            results.warn(
                "$contextString provides neither 'headers', 'bindings', 'correlationId', nor 'contentType'" +
                    " — it might not have any effect.",
                asyncApiContext.getLine(node, node::headers),
            )
        }
    }

    private fun validateHeaders(node: MessageTrait, contextString: String, results: ValidationResults) {
        val headersSchema = node.headers ?: return
        val headers = extractHeaderProperties(headersSchema)
        headers.forEach { (schemaName, schemaInterface) ->
            val contextString = "$contextString Header Schema '$schemaName'"
            when (schemaInterface) {
                is SchemaInterface.SchemaInline ->
                    schemaValidator.validate(schemaInterface.schema, contextString, results)

                is SchemaInterface.SchemaReference ->
                    referenceResolver.resolve(schemaInterface.reference, contextString, results)

                is SchemaInterface.MultiFormatSchemaInline -> {}
                is SchemaInterface.BooleanSchema -> {}
            }
        }
    }

    private fun validateContentType(node: MessageTrait, contextString: String, results: ValidationResults) {
        val contentType = node.contentType?.let(::sanitizeString) ?: return
        if (!MIME_TYPE.matches(contentType)) {
            results.error(
                "$contextString invalid 'contentType' value '$contentType'. Expected a valid MIME type, e.g., 'application/json'.",
                asyncApiContext.getLine(node, node::contentType),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#messageTraitObject",
            )
        }
    }

    private fun validateTags(node: MessageTrait, contextString: String, results: ValidationResults) {
        val tags = node.tags ?: return
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "$contextString Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(tagInterface.reference, contextString, results)
            }
        }
    }

    private fun validateExternalDocs(node: MessageTrait, contextString: String, results: ValidationResults) {
        val externalDocs = node.externalDocs ?: return
        val contextString = "$contextString ExternalDocs"
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(externalDocs.reference, contextString, results)
        }
    }

    private fun validateBindings(node: MessageTrait, contextString: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        bindings.forEach { (bindingName, bindingInterface) ->
            val contextString = "$contextString Binding '$bindingName'"
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingInterface.binding, contextString, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingInterface.reference, contextString, results)
            }
        }
    }

    private fun extractHeaderProperties(schemaInterface: SchemaInterface): Map<String, SchemaInterface> =
        when (schemaInterface) {
            is SchemaInterface.SchemaInline -> {
                val schema = schemaInterface.schema
                if (schema.type.getPrimaryType() == "object") schema.properties ?: emptyMap() else emptyMap()
            }

            is SchemaInterface.SchemaReference -> {
                val schema = schemaInterface.reference.model as? Schema
                if (schema?.type.getPrimaryType() == "object") schema?.properties ?: emptyMap() else emptyMap()
            }

            else -> emptyMap()
        }
}
