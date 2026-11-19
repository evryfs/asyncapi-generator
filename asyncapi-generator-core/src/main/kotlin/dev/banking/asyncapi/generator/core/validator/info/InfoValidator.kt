package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TERMS_URI
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TITLE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_VERSION_REQUIRED
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank

internal class InfoValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val tagValidator = TagValidator(asyncApiContext)
    private val contactValidator = ContactValidator(asyncApiContext)
    private val licenseValidator = LicenseValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)

    fun validate(
        node: Info,
        contextString: String,
        results: ValidationCollector,
    ) {
        if (!results.visit(node)) return
        validateTitle(node, contextString, results)
        validateVersion(node, contextString, results)
        validateTermsOfService(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)

        node.contact?.let { contactValidator.validate(it, contextString, results) }
        node.license?.let { licenseValidator.validate(it, contextString, results) }
    }

    private fun validateTitle(
        node: Info,
        contextString: String,
        results: ValidationCollector,
    ) {
        results.requireNonBlank(
            node.title,
            "title",
            contextString,
            INFO_TITLE_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::title),
        )
    }

    private fun validateVersion(
        node: Info,
        contextString: String,
        results: ValidationCollector,
    ) {
        results.requireNonBlank(
            node.version,
            "version",
            contextString,
            INFO_VERSION_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::version),
        )
    }

    private fun validateTermsOfService(
        node: Info,
        contextString: String,
        results: ValidationCollector,
    ) {
        results.requireAbsoluteUri(
            node.termsOfService,
            "termsOfService",
            contextString,
            INFO_TERMS_URI,
            asyncApiContext.modelTracking.getSourceLocation(node, node::termsOfService),
        )
    }

    private fun validateTags(
        node: Info,
        contextString: String,
        results: ValidationCollector,
    ) {
        tagValidator.validateList(node.tags, contextString, results)
    }

    private fun validateExternalDocs(
        node: Info,
        contextString: String,
        results: ValidationCollector,
    ) {
        node.externalDocs?.let { externalDocs ->
            externalDocsValidator.validateInterface(externalDocs, "$contextString ExternalDocs", results)
        }
    }
}
