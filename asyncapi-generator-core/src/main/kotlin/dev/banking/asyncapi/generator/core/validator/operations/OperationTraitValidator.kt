package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver

internal class OperationTraitValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val tagValidator = TagValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationTraitInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is OperationTraitInterface.OperationTraitInline ->
                validate(node.operationTrait, contextString, results)

            is OperationTraitInterface.OperationTraitReference ->
                referenceResolver.resolve(node.reference, OPERATION_TRAIT, contextString, results)
        }
    }

    fun validate(node: OperationTrait, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateSecurity(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)
    }

    private fun validateSecurity(node: OperationTrait, contextString: String, results: ValidationCollector) {
        val securitySchemes = node.security ?: return
        securitySchemeValidator.validateList(
            securitySchemes,
            "$contextString Security Scheme",
            results,
        )
    }

    private fun validateTags(node: OperationTrait, contextString: String, results: ValidationCollector) {
        tagValidator.validateList(node.tags, contextString, results)
    }

    private fun validateExternalDocs(node: OperationTrait, contextString: String, results: ValidationCollector) {
        val externalDocs = node.externalDocs ?: return
        externalDocsValidator.validateInterface(externalDocs, "$contextString ExternalDocs", results)
    }

    private fun validateBindings(node: OperationTrait, contextString: String, results: ValidationCollector) {
        bindingValidator.validateMap(node.bindings, contextString, results)
    }
}
