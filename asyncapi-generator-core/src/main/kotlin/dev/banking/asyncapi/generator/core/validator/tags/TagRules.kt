package dev.banking.asyncapi.generator.core.validator.tags

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.TAG_NAME_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Tag Object and walks its external documentation.
 *
 * @param asyncApiContext the load the tags belong to
 */
internal class TagRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Tag> {
    override fun check(
        node: Tag,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        walk.results.requireNonBlank(
            node.name,
            "name",
            path,
            TAG_NAME_REQUIRED,
            asyncApiContext.modelTracking.getSourceLocation(node, node::name),
        )
        walk.externalDocs(node.externalDocs, path)
    }
}
