package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.diagnostics.ValidationReporter
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
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationReport
import dev.banking.asyncapi.generator.core.parser.external.LoadedTarget
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.channels.ChannelValidator
import dev.banking.asyncapi.generator.core.validator.correlations.CorrelationIdValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.messages.MessageTraitValidator
import dev.banking.asyncapi.generator.core.validator.messages.MessageValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationReplyAddressValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationReplyValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationTraitValidator
import dev.banking.asyncapi.generator.core.validator.operations.OperationValidator
import dev.banking.asyncapi.generator.core.validator.parameters.ParameterValidator
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.servers.ServerValidator
import dev.banking.asyncapi.generator.core.validator.servers.ServerVariableValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

private typealias FragmentValidation = (model: Any, contextString: String, results: ValidationCollector) -> Unit

/**
 * Validates the documents and fragments loaded for references, each on its own.
 *
 * A fragment is validated as the kind of object its reference expects, and its findings name it
 * `External <Kind> '<name>'`.
 *
 * @param context the load the targets belong to
 */
internal class LoadedTargetValidator(
    private val context: AsyncApiContext,
) {
    /**
     * Validates [targets] in order and fails on the first one with errors.
     *
     * @return the warnings of all targets
     */
    fun validate(targets: List<LoadedTarget>): List<ValidationFinding> =
        targets.flatMap { target ->
            val report =
                when (target) {
                    is LoadedTarget.Document -> AsyncApiValidator(context).validate(target.document)
                    is LoadedTarget.Fragment -> validate(target)
                }
            ValidationReporter(context).throwErrors(report)
            report.warnings
        }

    private fun validate(fragment: LoadedTarget.Fragment): ValidationReport {
        val results = ValidationCollector()
        val contextString = "External ${fragment.category.label} '${fragment.name}'"
        validatorFor(fragment.category)(fragment.model, contextString, results)
        ReferenceTargetTraversal(context).drain(results)
        return results.report()
    }

    private fun validatorFor(category: ReferenceCategoryKey): FragmentValidation =
        when (category) {
            SCHEMA -> typed(SchemaValidator(context)::validateInterface)
            CHANNEL -> typed(ChannelValidator(context)::validateInterface)
            MESSAGE -> typed(MessageValidator(context)::validateInterface)
            MESSAGE_TRAIT -> typed(MessageTraitValidator(context)::validateInterface)
            OPERATION -> typed(OperationValidator(context)::validateInterface)
            OPERATION_TRAIT -> typed(OperationTraitValidator(context)::validateInterface)
            OPERATION_REPLY -> typed(OperationReplyValidator(context)::validateInterface)
            OPERATION_REPLY_ADDRESS -> typed(OperationReplyAddressValidator(context)::validateInterface)
            SERVER ->
                typed<ServerInterface> {
                    model,
                    name,
                    results,
                    ->
                    ServerValidator(context).validateInterface(model, name, results)
                }
            SERVER_VARIABLE -> typed(ServerVariableValidator(context)::validateInterface)
            PARAMETER -> typed(ParameterValidator(context)::validateInterface)
            SECURITY_SCHEME -> typed(SecuritySchemeValidator(context)::validateInterface)
            CORRELATION_ID -> typed(CorrelationIdValidator(context)::validateInterface)
            EXTERNAL_DOC -> typed(ExternalDocsValidator(context)::validateInterface)
            TAG -> typed(TagValidator(context)::validateInterface)
            BINDING -> typed(BindingValidator(context)::validateInterface)
        }

    private inline fun <reified M> typed(
        crossinline validate: (M, String, ValidationCollector) -> Unit,
    ): FragmentValidation = { model, contextString, results -> validate(model as M, contextString, results) }

    /** The kind's name as written in fragment findings, such as `MessageTrait`. */
    private val ReferenceCategoryKey.label: String
        get() = name.split('_').joinToString("") { word -> word.lowercase().replaceFirstChar(Char::uppercase) }
}
