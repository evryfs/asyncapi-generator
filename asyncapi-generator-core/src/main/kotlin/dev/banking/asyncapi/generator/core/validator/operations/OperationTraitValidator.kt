package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.bindings.BindingValidator
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.security.SecuritySchemeValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults

class OperationTraitValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val tagValidator = TagValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: OperationTraitInterface, contextString: String, results: ValidationResults) {
        when (node) {
            is OperationTraitInterface.OperationTraitInline ->
                validate(node.operationTrait, contextString, results)

            is OperationTraitInterface.OperationTraitReference ->
                referenceResolver.resolve(node.reference, contextString, results)
        }
    }

    fun validate(node: OperationTrait, contextString: String, results: ValidationResults) {
        validateSecurity(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)
        validateBindings(node, contextString, results)
    }

    private fun validateSecurity(node: OperationTrait, contextString: String, results: ValidationResults) {
        val securitySchemes = node.security ?: return
        securitySchemes.forEach { (securitySchemeName, securitySchemeInterface) ->
            val contextString = "$contextString Security Scheme '$securitySchemeName'"
            when (securitySchemeInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(securitySchemeInterface.security, contextString, results)

                is SecuritySchemeInterface.SecuritySchemeReference ->
                    referenceResolver.resolve(securitySchemeInterface.reference, contextString, results)
            }
        }
    }

    private fun validateTags(node: OperationTrait, contextString: String, results: ValidationResults) {
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

    private fun validateExternalDocs(node: OperationTrait, contextString: String, results: ValidationResults) {
        val externalDocs = node.externalDocs ?: return
        val contextString = "$contextString ExternalDocs"
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(externalDocs.reference, contextString, results)
        }
    }

    private fun validateBindings(node: OperationTrait, contextString: String, results: ValidationResults) {
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
}
