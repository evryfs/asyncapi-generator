package dev.banking.asyncapi.generator.core.validator.info

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

    fun validate(node: Info, results: ValidationResults) {
        validateTitle(node, results)
        validateVersion(node, results)
        validateDescription(node, results)
        validateTermsOfService(node, results)
        validateTags(node, results)
        validateExternalDocs(node, results)

        node.contact?.let { contactValidator.validate(it, results) }
        node.license?.let { licenseValidator.validate(it, results) }
    }

    private fun validateTitle(node: Info, results: ValidationResults) {
        val title = node.title.let(::sanitizeString)
        if (title.isBlank()) {
            results.error(
                "The 'title' field in the Info object is required and cannot be empty.",
                asyncApiContext.getLine(node, node::title)
            )
        }
    }

    private fun validateVersion(node: Info, results: ValidationResults) {
        val version = node.version.let(::sanitizeString)
        if (version.isBlank()) {
            results.error(
                "The 'version' field in the Info object is required and cannot be empty.",
                asyncApiContext.getLine(node, node::version)
            )
        } else if (!Regex("""^[A-Za-z0-9_.-]+$""").matches(version)) {
            results.warn(
                "The 'version' field contains unusual characters. Expected alphanumeric with optional '.', '-', or '_'.",
                asyncApiContext.getLine(node, node::version)
            )
        }
    }

    private fun validateDescription(node: Info, results: ValidationResults) {
        val description = node.description?.let(::sanitizeString) ?: return
        if (description.length < 3) {
            results.warn(
                "The 'description' field seems too short to be meaningful.",
                asyncApiContext.getLine(node, node::description)
            )
        }
    }

    private fun validateTermsOfService(node: Info, results: ValidationResults) {
        val tos = node.termsOfService?.let(::sanitizeString) ?: return
        val urlRegex = Regex("""^(https?|wss?)://\S+$""")
        if (!urlRegex.matches(tos)) {
            results.error(
                "The 'termsOfService' field must be a valid absolute URL. Got '$tos'.",
                asyncApiContext.getLine(node, node::termsOfService)
            )
        }
    }

    private fun validateTags(node: Info, results: ValidationResults) {
        val tags = node.tags ?: return
        if (tags.isEmpty()) {
            results.warn(
                "The 'tags' list is empty. Can omit entirely if unused.",
                asyncApiContext.getLine(node, node::tags)
            )
        } else {
            tags.forEach { tagInterface ->
                when (tagInterface) {
                    is TagInterface.TagInline ->
                        tagValidator.validate(tagInterface.tag, "info", results)
                    is TagInterface.TagReference -> {
                        referenceResolver.resolve("info", tagInterface.reference, "Info", results)
                    }
                }
            }
        }
    }

    private fun validateExternalDocs(node: Info, results: ValidationResults) {
        val externalDocs = node.externalDocs ?: return
        when (externalDocs) {
            is ExternalDocInterface.ExternalDocInline ->
                externalDocsValidator.validate(externalDocs.externalDoc, "info", results)
            is ExternalDocInterface.ExternalDocReference ->
                referenceResolver.resolve("info", externalDocs.reference, "Info", results)
        }
    }
}
