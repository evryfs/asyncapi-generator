package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TERMS_OF_SERVICE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TITLE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_VERSION_REQUIRED
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

internal class InfoValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val contactValidator = ContactValidator(asyncApiContext)
    private val licenseValidator = LicenseValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)

    fun validate(node: Info, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateTitle(node, contextString, results)
        validateVersion(node, contextString, results)
        validateTermsOfService(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)

        node.contact?.let { contactValidator.validate(it, contextString, results) }
        node.license?.let { licenseValidator.validate(it, contextString, results) }
    }

    private fun validateTitle(node: Info, contextString: String, results: ValidationCollector) {
        val title = node.title
        if (title.isBlank()) {
            results.error(
                INFO_TITLE_REQUIRED,
                "$contextString 'title' field is required and cannot be empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::title),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#infoObject",
            )
        }
    }

    private fun validateVersion(node: Info, contextString: String, results: ValidationCollector) {
        val version = node.version
        if (version.isBlank()) {
            results.error(
                INFO_VERSION_REQUIRED,
                "$contextString 'version' field is required and cannot be empty.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::version),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#infoObject",
            )
            return
        }
    }

    private fun validateTermsOfService(node: Info, contextString: String, results: ValidationCollector) {
        val termsOfService = node.termsOfService ?: return
        if (ValidationFormats.absoluteUri(termsOfService) == null) {
            results.error(
                INFO_TERMS_OF_SERVICE_FORMAT,
                "$contextString 'termsOfService' field must be a valid absolute URL. Got '$termsOfService'.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::termsOfService),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#infoObject",
            )
        }
    }

    private fun validateTags(node: Info, contextString: String, results: ValidationCollector) {
        tagValidator.validateList(node.tags, contextString, results)
    }

    private fun validateExternalDocs(node: Info, contextString: String, results: ValidationCollector) {
        node.externalDocs?.let { externalDocs ->
            externalDocsValidator.validateInterface(externalDocs, "$contextString ExternalDocs", results)
        }
    }
}
