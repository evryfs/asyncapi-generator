package dev.banking.asyncapi.generator.core.validator.externaldocs

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.EXTERNAL_DOC_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.EXTERNAL_DOC_URL_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the External Documentation Object.
 *
 * @param asyncApiContext the load the external documentation belongs to
 */
internal class ExternalDocsRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<ExternalDoc> {
    override fun check(
        node: ExternalDoc,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val urlSource = asyncApiContext.modelTracking.getSourceLocation(node, node::url)
        if (walk.results.requireNonBlank(node.url, "url", path, EXTERNAL_DOC_URL_REQUIRED, urlSource)) {
            walk.results.requireAbsoluteUri(node.url, "url", path, EXTERNAL_DOC_URL_FORMAT, urlSource)
        }
    }
}
