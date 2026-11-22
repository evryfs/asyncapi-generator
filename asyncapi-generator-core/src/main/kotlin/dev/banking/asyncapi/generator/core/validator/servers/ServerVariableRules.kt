package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValue
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValueRules
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Server Variable Object.
 *
 * @param asyncApiContext the load the server variables belong to
 */
internal class ServerVariableRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<ServerVariable> {
    private val valueRules =
        EnumConstrainedValueRules(
            SERVER_VARIABLE_ENUM_UNIQUE,
            SERVER_VARIABLE_DEFAULT_ENUM,
            SERVER_VARIABLE_EXAMPLES_ENUM,
        )

    override fun check(
        node: ServerVariable,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        valueRules.validate(
            node,
            EnumConstrainedValue(node.enum, node.default, node.examples),
            path,
            walk.results,
            asyncApiContext.modelTracking,
        )
    }
}
