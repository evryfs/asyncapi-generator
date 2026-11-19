package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_LOCATION
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValue
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValueRules
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the Parameter Object.
 *
 * @param asyncApiContext the load the parameters belong to
 */
internal class ParameterRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Parameter> {
    private val valueRules =
        EnumConstrainedValueRules(PARAMETER_ENUM_UNIQUE, PARAMETER_DEFAULT_ENUM, PARAMETER_EXAMPLES_ENUM)

    override fun check(
        node: Parameter,
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
        validateLocation(node, path, walk)
    }

    private fun validateLocation(
        node: Parameter,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val location = node.location ?: return
        if (!ValidationFormats.isRuntimeExpression(location)) {
            walk.results.error(
                PARAMETER_LOCATION,
                $$"$$path invalid 'location' expression '$$location'. Must be a valid " +
                    $$"runtime expression (e.g., $message.header#/param).",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::location),
            )
        }
    }

    companion object {
        /** Reports a [name] of a `parameters` map that is not a reusable object name. */
        fun checkName(
            name: String,
            parameter: ParameterInterface,
            path: ValidationPath,
            walk: ValidationWalk,
        ) {
            if (!ValidationFormats.isReusableObjectName(name)) {
                walk.results.error(
                    PARAMETER_NAME_FORMAT,
                    "$path name must contain only letters, digits, underscores, or hyphens.",
                    sourceLocation = walk.modelTracking.getSourceLocation(parameter.held()),
                )
            }
        }
    }
}
