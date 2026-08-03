package dev.banking.asyncapi.generator.core.resolver

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.REFERENCE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_CATEGORY_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_TARGET_CATEGORY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_UNRESOLVED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import java.util.Collections
import java.util.IdentityHashMap

internal class ReferenceResolver(
    private val asyncApiContext: AsyncApiContext,
) {

    fun resolve(
        reference: Reference,
        expectedCategory: ReferenceCategoryKey,
        contextString: String,
        results: ValidationCollector,
        targetCategoryRule: ValidationRule = REFERENCE_TARGET_CATEGORY,
    ): Any? {
        if (expectedCategory == REFERENCE) {
            results.error(
                REFERENCE_CATEGORY_REQUIRED,
                "$contextString reference '${reference.ref}' has no concrete target category.",
                sourceLocation = asyncApiContext.getSourceLocation(reference, reference::ref),
            )
            return null
        }
        if (reference.referenceCategoryKey != expectedCategory) {
            results.error(
                REFERENCE_CATEGORY_REQUIRED,
                "$contextString reference '${reference.ref}' was created for category " +
                    "'${reference.referenceCategoryKey}' instead of '$expectedCategory'.",
                sourceLocation = asyncApiContext.getSourceLocation(reference, reference::ref),
            )
            return null
        }
        if (!results.process(reference)) {
            return finalTarget(reference)
        }

        val chain: MutableSet<Reference> =
            Collections.newSetFromMap(IdentityHashMap())
        var current = reference
        while (chain.add(current)) {
            val target = asyncApiContext.findReference(current)
            if (target == null) {
                results.error(
                    REFERENCE_UNRESOLVED,
                    "$contextString reference '${current.ref}' could not be resolved",
                    sourceLocation = asyncApiContext.getSourceLocation(current, current::ref),
                )
                return null
            }
            current.model = target
            if (target is Reference) {
                results.process(target)
                if (target in chain) {
                    return null
                }
                if (target.referenceCategoryKey != expectedCategory) {
                    results.error(
                        REFERENCE_CATEGORY_REQUIRED,
                        "$contextString reference '${target.ref}' was created for category " +
                            "'${target.referenceCategoryKey}' instead of '$expectedCategory'.",
                        sourceLocation = asyncApiContext.getSourceLocation(target, target::ref),
                    )
                    return null
                }
                current = target
                continue
            }
            if (!matches(expectedCategory, target)) {
                results.error(
                    targetCategoryRule,
                    "$contextString reference '${reference.ref}' must resolve to ${expectedCategory.displayName}; " +
                        "found ${target::class.simpleName}.",
                    sourceLocation = asyncApiContext.getSourceLocation(reference, reference::ref),
                )
                return null
            }
            results.enqueueReferenceTarget(expectedCategory, target, contextString)
            return target
        }

        return null
    }

    private fun finalTarget(reference: Reference): Any? {
        val chain: MutableSet<Reference> =
            Collections.newSetFromMap(IdentityHashMap())
        var target: Any? = reference
        while (target is Reference && chain.add(target)) {
            target = target.model
        }
        return target?.takeUnless { it is Reference }
    }

    private fun matches(category: ReferenceCategoryKey, target: Any): Boolean =
        when (category) {
            SCHEMA -> target is Schema || target is MultiFormatSchema || target is SchemaInterface.BooleanSchema
            CHANNEL -> target is Channel
            MESSAGE -> target is Message
            MESSAGE_TRAIT -> target is MessageTrait
            OPERATION -> target is Operation
            OPERATION_TRAIT -> target is OperationTrait
            OPERATION_REPLY -> target is OperationReply
            OPERATION_REPLY_ADDRESS -> target is OperationReplyAddress
            SERVER -> target is Server
            SERVER_VARIABLE -> target is ServerVariable
            PARAMETER -> target is Parameter
            SECURITY_SCHEME -> target is SecurityScheme
            CORRELATION_ID -> target is CorrelationId
            EXTERNAL_DOC -> target is ExternalDoc
            TAG -> target is Tag
            BINDING -> target is Binding
            REFERENCE -> false
        }

    private val ReferenceCategoryKey.displayName: String
        get() = name.lowercase().replace('_', ' ') + " object"
}
