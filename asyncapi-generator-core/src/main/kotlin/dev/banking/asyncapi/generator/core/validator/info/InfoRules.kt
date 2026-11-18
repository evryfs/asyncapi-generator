package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TERMS_URI
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_TITLE_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.INFO_VERSION_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Info Object and walks its tags, external documentation, contact and license.
 *
 * @param asyncApiContext the load the info belongs to
 */
internal class InfoRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Info> {
    private val contactRules = ContactRules(asyncApiContext)
    private val licenseRules = LicenseRules(asyncApiContext)

    override fun check(
        node: Info,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val results = walk.results
        if (!results.visit(node)) return
        results.requireNonBlank(
            node.title,
            "title",
            path,
            INFO_TITLE_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::title),
        )
        results.requireNonBlank(
            node.version,
            "version",
            path,
            INFO_VERSION_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::version),
        )
        results.requireAbsoluteUri(
            node.termsOfService,
            "termsOfService",
            path,
            INFO_TERMS_URI,
            asyncApiContext.modelTracking.getSourceLocation(node, node::termsOfService),
        )
        walk.tags(node.tags, path)
        walk.externalDocs(node.externalDocs, path)

        node.contact?.let { contactRules.check(it, path, walk) }
        node.license?.let { licenseRules.check(it, path, walk) }
    }
}
