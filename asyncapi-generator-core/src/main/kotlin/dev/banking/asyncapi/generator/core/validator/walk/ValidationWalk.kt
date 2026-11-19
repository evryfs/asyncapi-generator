package dev.banking.asyncapi.generator.core.validator.walk

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
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
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_TARGET_CATEGORY
import dev.banking.asyncapi.generator.core.parser.tracking.ModelTracking
import dev.banking.asyncapi.generator.core.validator.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingRules
import dev.banking.asyncapi.generator.core.validator.channels.ChannelRules
import dev.banking.asyncapi.generator.core.validator.correlations.CorrelationIdRules
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsRules
import dev.banking.asyncapi.generator.core.validator.messages.MessageRules
import dev.banking.asyncapi.generator.core.validator.messages.MessageTraitRules
import dev.banking.asyncapi.generator.core.validator.operations.OperationReplyAddressRules
import dev.banking.asyncapi.generator.core.validator.operations.OperationReplyRules
import dev.banking.asyncapi.generator.core.validator.operations.OperationRules
import dev.banking.asyncapi.generator.core.validator.operations.OperationTraitRules
import dev.banking.asyncapi.generator.core.validator.parameters.ParameterRules
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaRules
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeRules
import dev.banking.asyncapi.generator.core.validator.servers.ServerRules
import dev.banking.asyncapi.generator.core.validator.servers.ServerVariableRules
import dev.banking.asyncapi.generator.core.validator.tags.TagRules
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

/**
 * One validation run over the linked model.
 *
 * The walk owns one rule set per kind of object and decides how a member is checked: an inline object by
 * the rules of its kind, once per run, and a `$ref` by reporting its link and queueing its target. Rule
 * sets check their object and walk their children through the walk, so findings keep the order of the
 * document.
 *
 * @param context the load the model belongs to
 * @param results the collector findings are reported to
 */
internal class ValidationWalk(
    val context: AsyncApiContext,
    val results: ValidationCollector,
) {
    private val referenceResolver = ReferenceResolver(context)

    val schemaRules = SchemaRules(context)
    private val channelRules = ChannelRules(context)
    private val messageRules = MessageRules(context)
    private val messageTraitRules = MessageTraitRules(context)
    private val operationRules = OperationRules(context)
    private val operationTraitRules = OperationTraitRules()
    private val operationReplyRules = OperationReplyRules(context)
    private val operationReplyAddressRules = OperationReplyAddressRules(context)
    private val serverRules = ServerRules(context)
    private val serverVariableRules = ServerVariableRules(context)
    private val parameterRules = ParameterRules(context)
    private val securitySchemeRules = SecuritySchemeRules(context)
    private val correlationIdRules = CorrelationIdRules(context)
    private val externalDocsRules = ExternalDocsRules(context)
    private val tagRules = TagRules(context)
    private val bindingRules = BindingRules(context)

    /** Where the models of the run are written. */
    val modelTracking: ModelTracking
        get() = context.modelTracking

    /**
     * Checks the object [member] holds: an inline object by the rules of [category], a `$ref` by its link.
     *
     * @param member the member
     * @param category the kind of object the member holds
     * @param path the member as findings name it
     * @return the inline object or the model the reference leads to, or `null` when it leads to none
     */
    fun member(
        member: ReferenceOr<*>,
        category: ReferenceCategoryKey,
        path: ValidationPath,
    ): Any? =
        when (val held = member.held()) {
            is Reference -> reference(held, category, path)
            else -> held.also { model -> check(category, model, path) }
        }

    /**
     * Reports the link of [reference], which must lead to a [category] object, and queues its target.
     *
     * @param reference the reference
     * @param category the kind of object it must lead to
     * @param path the reference as findings name it
     * @param targetCategoryRule the rule reported when the reference leads to another kind of object
     * @return the model the reference leads to, or `null` when it leads to none
     */
    fun reference(
        reference: Reference,
        category: ReferenceCategoryKey,
        path: ValidationPath,
        targetCategoryRule: ValidationRule = REFERENCE_TARGET_CATEGORY,
    ): Any? = referenceResolver.resolve(reference, category, path, results, targetCategoryRule)

    /** Checks a Schema Object, a boolean schema, a Multi Format Schema Object or a `$ref` to a schema. */
    fun schema(
        schema: SchemaInterface,
        path: ValidationPath,
    ) {
        schemaRules.checkInterface(schema, path, this)
    }

    /** Checks the tags of an object, named `Tag[i]` below [path]. */
    fun tags(
        tags: List<TagInterface>?,
        path: ValidationPath,
    ) {
        tags?.forEachIndexed { index, tag -> member(tag, TAG, path.indexed("Tag", index)) }
    }

    /** Checks the external documentation of an object, named `ExternalDocs` below [path]. */
    fun externalDocs(
        externalDocs: ExternalDocInterface?,
        path: ValidationPath,
    ) {
        externalDocs?.let { member(it, EXTERNAL_DOC, path.child("ExternalDocs")) }
    }

    /** Checks the protocol bindings of an object, each named `Binding 'protocol'` below [path]. */
    fun bindings(
        bindings: Map<String, BindingInterface>?,
        path: ValidationPath,
    ) {
        bindings?.forEach { (protocol, binding) -> member(binding, BINDING, path.named("Binding", protocol)) }
    }

    /** Checks the security requirements of an object, named `Security Scheme[i]` below [path]. */
    fun securitySchemes(
        securitySchemes: List<SecuritySchemeInterface>?,
        path: ValidationPath,
    ) {
        securitySchemes?.forEachIndexed { index, securityScheme ->
            member(securityScheme, SECURITY_SCHEME, path.indexed("Security Scheme", index))
        }
    }

    /** Checks the targets that references led to and that no rule set has checked yet, as `<path> target`. */
    fun drainReferenceTargets() {
        while (true) {
            val pending = results.nextReferenceTarget() ?: return
            check(pending.category, pending.target, pending.path.child("target"))
        }
    }

    /**
     * Checks [model] by the rules of [category], once per run.
     *
     * A model that is not of the kind [category] expects is not checked; the reference that leads to it is
     * reported instead.
     */
    @Suppress("UNCHECKED_CAST")
    fun check(
        category: ReferenceCategoryKey,
        model: Any,
        path: ValidationPath,
    ) {
        if (category == SCHEMA) {
            schemaRules.checkModel(model, path, this)
            return
        }
        if (!category.accepts(model) || !results.visit(model)) return
        val rules: ObjectRules<Any> =
            when (category) {
                CHANNEL -> channelRules
                MESSAGE -> messageRules
                MESSAGE_TRAIT -> messageTraitRules
                OPERATION -> operationRules
                OPERATION_TRAIT -> operationTraitRules
                OPERATION_REPLY -> operationReplyRules
                OPERATION_REPLY_ADDRESS -> operationReplyAddressRules
                SERVER -> serverRules
                SERVER_VARIABLE -> serverVariableRules
                PARAMETER -> parameterRules
                SECURITY_SCHEME -> securitySchemeRules
                CORRELATION_ID -> correlationIdRules
                EXTERNAL_DOC -> externalDocsRules
                TAG -> tagRules
                BINDING -> bindingRules
                SCHEMA -> error("Schemas are checked by the schema rules")
            } as ObjectRules<Any>
        rules.check(model, path, this)
    }
}
