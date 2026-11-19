package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_LOCATION
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.validator.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValue
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValueRules
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

internal class ParameterValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val referenceResolver = ReferenceResolver(asyncApiContext)
    private val valueRules =
        EnumConstrainedValueRules(PARAMETER_ENUM_UNIQUE, PARAMETER_DEFAULT_ENUM, PARAMETER_EXAMPLES_ENUM)

    fun validateInterface(
        parameterInterface: ParameterInterface,
        contextString: String,
        results: ValidationCollector,
        name: String? = null,
    ) {
        if (name != null && !ValidationFormats.isReusableObjectName(name)) {
            results.error(
                PARAMETER_NAME_FORMAT,
                "$contextString name must contain only letters, digits, underscores, or hyphens.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(parameterInterface.held()),
            )
        }
        when (parameterInterface) {
            is ParameterInterface.ParameterInline ->
                validate(parameterInterface.parameter, contextString, results)
            is ParameterInterface.ParameterReference ->
                referenceResolver.resolve(parameterInterface.reference, PARAMETER, contextString, results)
        }
    }

    fun validate(
        node: Parameter,
        contextString: String,
        results: ValidationCollector,
    ) {
        if (!results.visit(node)) return
        valueRules.validate(
            node,
            EnumConstrainedValue(node.enum, node.default, node.examples),
            contextString,
            results,
            asyncApiContext.modelTracking,
        )
        validateLocation(node, contextString, results)
    }

    private fun validateLocation(
        node: Parameter,
        contextString: String,
        results: ValidationCollector,
    ) {
        val location = node.location ?: return
        if (!ValidationFormats.isRuntimeExpression(location)) {
            results.error(
                PARAMETER_LOCATION,
                $$"$$contextString invalid 'location' expression '$$location'. Must be a valid " +
                    $$"runtime expression (e.g., $message.header#/param).",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::location),
            )
        }
    }
}
