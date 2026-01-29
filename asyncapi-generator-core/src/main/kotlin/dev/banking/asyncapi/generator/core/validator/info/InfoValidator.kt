package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.SEMANTIC_VERSION
import dev.banking.asyncapi.generator.core.constants.RegexPatterns.URL
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.externaldocs.ExternalDocsValidator
import dev.banking.asyncapi.generator.core.validator.tags.TagValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class InfoValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagValidator = TagValidator(asyncApiContext)
    private val contactValidator = ContactValidator(asyncApiContext)
    private val licenseValidator = LicenseValidator(asyncApiContext)
    private val externalDocsValidator = ExternalDocsValidator(asyncApiContext)
    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validate(node: Info, contextString: String, results: ValidationResults) {
        validateTitle(node, contextString, results)
        validateVersion(node, contextString, results)
        validateTermsOfService(node, contextString, results)
        validateTags(node, contextString, results)
        validateExternalDocs(node, contextString, results)

        node.contact?.let { contactValidator.validate(it, contextString, results) }
        node.license?.let { licenseValidator.validate(it, contextString, results) }
    }

    private fun validateTitle(node: Info, contextString: String, results: ValidationResults) {
        val title = node.title.let(::sanitizeString)
        if (title.isBlank()) {
            results.error(
                "$contextString 'title' field is required and cannot be empty.",
                asyncApiContext.getLine(node, node::title),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#infoObject",
            )
        }
    }

    private fun validateVersion(node: Info, contextString: String, results: ValidationResults) {
        val version = node.version.let(::sanitizeString)
        if (version.isBlank()) {
            results.error(
                "$contextString 'version' field is required and cannot be empty.",
                asyncApiContext.getLine(node, node::version),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#infoObject",
            )
            return
        }
        if (!SEMANTIC_VERSION.matches(version)) {
            results.warn(
                "$contextString 'version' field contains unusual characters. Expected Semantic Versioning or alphanumeric format.",
                asyncApiContext.getLine(node, node::version)
            )
        }
    }

    private fun validateTermsOfService(node: Info, contextString: String, results: ValidationResults) {
        val termsOfService = node.termsOfService?.let(::sanitizeString) ?: return
        if (!URL.matches(termsOfService)) {
            results.error(
                "$contextString 'termsOfService' field must be a valid absolute URL. Got '$termsOfService'.",
                asyncApiContext.getLine(node, node::termsOfService),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#infoObject",
            )
        }
    }

    private fun validateTags(node: Info, contextString: String, results: ValidationResults) {
        val tags = node.tags ?: return
        tags.forEachIndexed { index, tagInterface ->
            val contextString = "$contextString Tag[$index]"
            when (tagInterface) {
                is TagInterface.TagInline ->
                    tagValidator.validate(tagInterface.tag, contextString, results)

                is TagInterface.TagReference -> {
                    referenceResolver.resolve(tagInterface.reference, contextString, results)
                }
            }
        }
    }

    private fun validateExternalDocs(node: Info, contextString: String, results: ValidationResults) {
        val externalDocs = node.externalDocs ?: return
        val contextString = "$contextString ExternalDocs"
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, contextString, results)

            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve(externalDocs.reference, contextString, results)
        }
    }
}
