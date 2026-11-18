package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_ADDRESS_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REPLY_ADDRESS_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.util.requireNonBlank
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Operation Reply Address Object.
 *
 * @param asyncApiContext the load the reply addresses belong to
 */
internal class OperationReplyAddressRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<OperationReplyAddress> {
    override fun check(
        node: OperationReplyAddress,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val location = node.location
        val locationSource = asyncApiContext.modelTracking.getSourceLocation(node, node::location)
        if (!walk.results.requireNonBlank(
                location,
                "location",
                path,
                REPLY_ADDRESS_REQUIRED,
                locationSource,
            )
        ) {
            return
        }
        if (!ValidationFormats.isRuntimeExpression(location)) {
            walk.results.error(
                REPLY_ADDRESS_FORMAT,
                "$path 'location' ('$location') must be a valid runtime expression.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::location),
            )
        }
    }
}
