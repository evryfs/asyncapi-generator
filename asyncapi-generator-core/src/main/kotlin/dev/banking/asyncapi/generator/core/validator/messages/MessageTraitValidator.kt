package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
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

    fun validateInterface(messageTraitName: String, node: MessageTraitInterface, results: ValidationResults) {
        when (node) {
            is MessageTraitInterface.InlineMessageTrait ->
                validate(node.trait, messageTraitName, results)

            is MessageTraitInterface.ReferenceMessageTrait ->
                referenceResolver.resolve(messageTraitName, node.reference, "Message Trait", results)
        }
    }

    fun validate(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        validateMeaningfulContent(node, messageTraitName, results)
        validateHeaders(node, messageTraitName, results)
        validateContentType(node, messageTraitName, results)
        validateName(node, messageTraitName, results)
        validateTitle(node, messageTraitName, results)
        validateSummary(node, messageTraitName, results)
        validateDescription(node, messageTraitName, results)
        validateTags(node, messageTraitName, results)
        validateExternalDocs(node, messageTraitName, results)
        validateBindings(node, messageTraitName, results)
        validateExamples(node, messageTraitName, results)


        node.correlationId?.let { correlationIdValidator.validateInterface(messageTraitName, it, results) }
    }

    private fun validateMeaningfulContent(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        if (node.headers == null && node.bindings == null && node.correlationId == null && node.contentType == null) {
            results.warn(
                "MessageTrait '$messageTraitName' provides neither 'headers', 'bindings', 'correlationId', nor 'contentType' — it might not have any effect.",
                asyncApiContext.getLine(node, node::headers)
            )
        }
    }

    private fun validateHeaders(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val headers = node.headers ?: return
        headers.forEach { (schemaName, schemaInterface) ->
            when (schemaInterface) {
                is SchemaInterface.SchemaInline ->
                    schemaValidator.validate(schemaInterface.schema, schemaName, results)

                is SchemaInterface.SchemaReference ->
                    referenceResolver.resolve(schemaName, schemaInterface.reference, messageTraitName, results)

                is SchemaInterface.MultiFormatSchemaInline -> {}
                is SchemaInterface.BooleanSchema -> {}
            }
        }
    }

    private fun validateContentType(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val contentType = node.contentType?.let(::sanitizeString) ?: return
        val mimeRegex = Regex("""^[a-zA-Z0-9!#$&^_.+-]+/[a-zA-Z0-9!#$&^_.+-]+$""")
        if (!mimeRegex.matches(contentType)) {
            results.error(
                "Message trait '$messageTraitName' invalid 'contentType' value '$contentType'. " +
                    "Expected a valid MIME type, e.g., 'application/json'.",
                asyncApiContext.getLine(node, node::contentType)
            )
        }
    }

    private fun validateName(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val name = node.name?.let(::sanitizeString)
            ?: return
        if (name.isBlank()) {
            results.warn(
                "Message trait '$messageTraitName' 'name' is empty — omit if unused.",
                asyncApiContext.getLine(node, node::name),
            )
        }
    }

    private fun validateTitle(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val title = node.title?.let(::sanitizeString)
            ?: return
        if (title.isBlank()) {
            results.warn(
                "Message trait '$messageTraitName' 'title' is empty — omit if unused.",
                asyncApiContext.getLine(node, node::title),
            )
        }
    }

    private fun validateSummary(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val summary = node.summary?.let(::sanitizeString)
        summary?.length?.let {
            if (it < 3) {
                results.warn(
                    "Message trait '$messageTraitName' 'summary' is too short to be meaningful.",
                    asyncApiContext.getLine(node, node::summary)
                )
            }
        }
    }

    private fun validateDescription(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val description = node.description?.let(::sanitizeString)
        description?.length?.let {
            if (it < 3) {
                results.warn(
                    "Message trait '$messageTraitName' 'description' is too short to be meaningful.",
                    asyncApiContext.getLine(node, node::description)
                )
            }
        }
    }

    private fun validateTags(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val tags = node.tags
            ?: return
        if (tags.isEmpty()) {
            results.warn(
                "Message trait '$messageTraitName' 'tags' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::tags)
            )
            return
        }
        tags.forEach { tagInterface ->
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, messageTraitName, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(messageTraitName, tagInterface.reference, "Message Trait Tag", results)
            }
        }
    }

    private fun validateExternalDocs(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val externalDocs = node.externalDocs
            ?: return
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, messageTraitName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(messageTraitName, externalDocs.reference, "Message Trait ExternalDocs", results)
        }
    }

    private fun validateBindings(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val bindings = node.bindings
            ?: return
        if (bindings.isEmpty()) {
            return
        }
        bindings.forEach { (bindingName, bindingInterface) ->
            when (bindingInterface) {
                is BindingInterface.BindingInline ->
                    bindingValidator.validate(bindingName, bindingInterface.binding, results)

                is BindingInterface.BindingReference ->
                    referenceResolver.resolve(bindingName, bindingInterface.reference, messageTraitName, results)
            }
        }
    }

    private fun validateExamples(node: MessageTrait, messageTraitName: String, results: ValidationResults) {
        val examples = node.examples
            ?: return
        if (examples.isEmpty()) {
            results.warn(
                "Message trait '$messageTraitName' 'examples' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::examples)
            )
        }
    }
}
