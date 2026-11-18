package dev.banking.asyncapi.generator.core.validator.info

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.LICENSE_URL_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.requireAbsoluteUri
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the License Object.
 *
 * @param asyncApiContext the load the licenses belong to
 */
internal class LicenseRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<License> {
    override fun check(
        node: License,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val results = walk.results
        if (!results.visit(node)) return
        results.requireNonBlank(
            node.name,
            "name",
            path,
            LICENSE_NAME_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::name),
        )
        results.requireAbsoluteUri(
            node.url,
            "url",
            path,
            LICENSE_URL_FORMAT,
            asyncApiContext.modelTracking.getSourceLocation(node, node::url),
        )
    }
}
