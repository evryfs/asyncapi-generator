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
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class OperationTraitValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val securitySchemeValidator = SecuritySchemeValidator(asyncApiContext)
    private val tagValidator = TagValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val bindingValidator = BindingValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(operationTraitName: String, node: OperationTraitInterface, results: ValidationResults) {
        when (node) {
            is OperationTraitInterface.OperationTraitInline ->
                validate(node.operationTrait, operationTraitName, results)

            is OperationTraitInterface.OperationTraitReference ->
                referenceResolver.resolve(operationTraitName, node.reference, "Operation Trait", results)
        }
    }

    fun validate(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
        validateTitle(node, operationTraitName, results)
        validateSummary(node, operationTraitName, results)
        validateDescription(node, operationTraitName, results)
        validateSecurity(node, operationTraitName, results)
        validateTags(node, operationTraitName, results)
        validateExternalDocs(node, operationTraitName, results)
        validateBindings(node, operationTraitName, results)
    }

    private fun validateTitle(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
        val title = node.title?.let(::sanitizeString)
            ?: return
        if (title.isBlank()) {
            results.warn(
                "Operation trait '$operationTraitName' 'title' is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::title)
            )
        }
    }

    private fun validateSummary(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
        val summary = node.summary?.let(::sanitizeString)
        summary?.length?.let {
            if (it < 3) {
                results.warn(
                    "Operation trait '$operationTraitName' 'summary' is too short to be meaningful.",
                    asyncApiContext.getLine(node, node::summary)
                )
            }
        }
    }

    private fun validateDescription(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
        val description = node.description?.let(::sanitizeString)
        description?.length?.let {
            if (it < 3) {
                results.warn(
                    "Operation trait '$operationTraitName' 'description' is too short to be meaningful.",
                    asyncApiContext.getLine(node, node::description)
                )
            }
        }
    }

    private fun validateSecurity(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
        val securitySchemes = node.security
            ?: return
        if (securitySchemes.isEmpty()) {
            results.warn(
                "Operation trait '$operationTraitName' 'security' list is empty — omit if unused.",
                asyncApiContext.getLine(node, node::security)
            )
            return
        }
        securitySchemes.forEach { (securitySchemeName, securitySchemeInterface) ->
            when (securitySchemeInterface) {
                is SecuritySchemeInterface.SecuritySchemeInline ->
                    securitySchemeValidator.validate(securitySchemeInterface.security, securitySchemeName, results)
                is SecuritySchemeInterface.SecuritySchemeReference ->
                    referenceResolver.resolve(securitySchemeName, securitySchemeInterface.reference, operationTraitName, results)
            }
        }
    }

    private fun validateTags(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
        val tags = node.tags ?: return
        if (tags.isEmpty()) {
            results.warn(
                "Operation trait 'tags' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::tags)
            )
            return
        }

        tags.forEach { tagInterface ->
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, operationTraitName, results)

                is TagInterface.TagReference ->
                    referenceResolver.resolve(operationTraitName, tagInterface.reference, "Operation Trait Tag", results)
            }
        }
    }

    private fun validateExternalDocs(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
        val externalDocs = node.externalDocs ?: return
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, operationTraitName, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(operationTraitName, externalDocs.reference, "Operation Trait ExternalDocs", results)
        }
    }

    private fun validateBindings(node: OperationTrait, operationTraitName: String, results: ValidationResults) {
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
                    referenceResolver.resolve(bindingName, bindingInterface.reference, operationTraitName, results)
            }
        }
    }
}
