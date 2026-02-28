package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults

class MessageValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val tagValidator = TagValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val schemaValidator = SchemaValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val messageTraitValidator = MessageTraitValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validate(message: Message, contextString: String, results: ValidationResults) {
        validatePayload(message, contextString, results)
        validateHeaders(message, contextString, results)
        validateTraits(message, contextString, results)
        validateTags(message, contextString, results)
        validateExternalDocs(message, contextString, results)
        validateBindings(message, contextString, results)
    }

    private fun validatePayload(node: Message, contextString: String, results: ValidationResults) {
        val contextString = "$contextString Payload"
        when (val payload = node.payload) {
            is SchemaInterface.SchemaInline ->
                schemaValidator.validate(payload.schema, contextString, results)

            is SchemaInterface.SchemaReference ->
                referenceResolver.resolve(payload.reference, contextString, results)

            is SchemaInterface.MultiFormatSchemaInline -> {}
            is SchemaInterface.BooleanSchema -> {}
            null -> {}
        }
    }

    private fun validateHeaders(node: Message, contextString: String, results: ValidationResults) {
        val headersSchema = node.headers ?: return
        val headers = extractHeaderProperties(headersSchema)
        headers.forEach { (headerName, schemaInterface) ->
            val contextString = "$contextString Header '$headerName'"
            when (schemaInterface) {
                is SchemaInterface.SchemaInline ->
                    schemaValidator.validate(schemaInterface.schema, contextString, results)

                is SchemaInterface.SchemaReference ->
                    referenceResolver.resolve(schemaInterface.reference, contextString, results)

                is SchemaInterface.MultiFormatSchemaInline -> {
                    results.warn(
                        "$contextString MultiFormatSchema in headers are not validated (header '$headerName').",
                        asyncApiContext.getLine(node, node::headers),
                    )
                }

                is SchemaInterface.BooleanSchema -> {}
            }
        }
    }

    private fun validateTraits(node: Message, contextString: String, results: ValidationResults) {
        val traits = node.traits ?: return
        if (traits.isEmpty()) return
        traits.forEachIndexed { index, trait ->
            val contextString = "$contextString Trait[$index]"
            when (trait) {
                is MessageTraitInterface.InlineMessageTrait ->
                    messageTraitValidator.validate(trait.trait, contextString, results)

                is MessageTraitInterface.ReferenceMessageTrait ->
                    referenceResolver.resolve(trait.reference, contextString, results)
            }
        }
    }

    private fun validateTags(node: Message, contextString: String, results: ValidationResults) {
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

    private fun validateExternalDocs(node: Message, contextString: String, results: ValidationResults) {
        val contextString = "$contextString ExternalDocs"
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(docs.reference, contextString, results)

            null -> {}
        }
    }

    private fun validateBindings(node: Message, contextString: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        if (bindings.isEmpty()) return
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
