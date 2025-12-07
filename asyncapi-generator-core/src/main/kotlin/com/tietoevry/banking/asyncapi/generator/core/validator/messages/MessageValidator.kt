package com.tietoevry.banking.asyncapi.generator.core.validator.messages

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.messages.Message
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageTrait
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.resolver.ReferenceResolver
import com.tietoevry.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.tags.TagValidator
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults

class MessageValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val schemaValidator = SchemaValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validate(message: Message, messageName: String, results: ValidationResults) {
        validateName(message, messageName, results)
        validatePayload(message, messageName, results)
        validateHeaders(message, messageName, results)
        validateTraits(message, messageName, results)
        validateTags(message, messageName, results)
        validateExternalDocs(message, messageName, results)
        validateBindings(message, messageName, results)
    }

    private fun validateName(node: Message, messageName: String, results: ValidationResults) {
        if (node.name.isNullOrBlank()) {
            results.warn(
                "Message '$messageName' missing 'name'. It’s recommended to provide one for referencing and tooling.",
                asyncApiContext.getLine(node, node::name)
            )
        }
    }

    private fun validatePayload(node: Message, messageName: String, results: ValidationResults) {
        when (val payload = node.payload) {
            is SchemaInterface.SchemaInline ->
                schemaValidator.validate(payload.schema, messageName, results)

            is SchemaInterface.SchemaReference ->
                referenceResolver.resolve(messageName, payload.reference, "Message", results)

            is SchemaInterface.MultiFormatSchemaInline -> {}
            is SchemaInterface.BooleanSchema -> {}
            null -> results.warn(
                "Message '$messageName' does not define a 'payload'. It will be treated as empty.",
                asyncApiContext.getLine(node, node::payload)
            )
        }
    }

    private fun validateHeaders(node: Message, messageName: String, results: ValidationResults) {
        val headers = node.headers ?: return
        headers.forEach { (headerName, schemaInterface) ->
            when (schemaInterface) {
                is SchemaInterface.SchemaInline ->
                    schemaValidator.validate(schemaInterface.schema, headerName, results)

                is SchemaInterface.SchemaReference ->
                    referenceResolver.resolve(headerName, schemaInterface.reference, "Message Header", results)

                is SchemaInterface.MultiFormatSchemaInline -> {
                    results.warn(
                        "Message '${messageName}' MultiFormatSchema in headers are not validated (header '$headerName').",
                        asyncApiContext.getLine(node, node::headers)
                    )
                }

                is SchemaInterface.BooleanSchema -> {}
            }
        }
    }

    private fun validateTraits(node: Message, messageName: String, results: ValidationResults) {
        val traits = node.traits
            ?: return
        if (traits.isEmpty()) {
            return
        }
        traits.forEach { trait ->
            when (trait) {
                is MessageTraitInterface.InlineMessageTrait ->
                    validateTraitInline(trait.trait, messageName, results)

                is MessageTraitInterface.ReferenceMessageTrait ->
                    referenceResolver.resolve(messageName, trait.reference, "Message Trait", results)
            }
        }
    }

    private fun validateTraitInline(node: MessageTrait, messageName: String, results: ValidationResults) {
        if (node.headers == null && node.bindings == null) {
            results.warn(
                "Message '${messageName}' Trait provides neither 'headers' nor 'bindings' — it might not have any effect.",
                asyncApiContext.getLine(node, node::headers)
            )
        }
    }

    private fun validateTags(node: Message, messageName: String, results: ValidationResults) {
        val tags = node.tags ?: return
        tags.forEach { tagInterface ->
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, messageName, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(messageName, tagInterface.reference, "Message Tag", results)
            }
        }
    }

    private fun validateExternalDocs(node: Message, messageName: String, results: ValidationResults) {
        when (val docs = node.externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(docs.externalDoc, messageName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(messageName, docs.reference, "Message ExternalDocs", results)

            null -> {}
        }
    }

    private fun validateBindings(node: Message, messageName: String, results: ValidationResults) {
        val bindings = node.bindings ?: return
        if (bindings.isEmpty()) {
            results.warn(
                "Message '$messageName' defines an empty 'bindings' object.",
                asyncApiContext.getLine(node, node::bindings),
            )
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingName, bindingInterface.binding, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingName, bindingInterface.reference, messageName, results)
            }
        }
    }
}
