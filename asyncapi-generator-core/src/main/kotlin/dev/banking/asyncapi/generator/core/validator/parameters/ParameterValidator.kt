package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_LOCATION_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.PARAMETER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

internal class ParameterValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(
        parameterInterface: ParameterInterface,
        contextString: String,
        results: ValidationCollector,
        name: String? = null,
    ) {
        if (name != null && !ValidationFormats.isReusableObjectName(name)) {
            val model = when (parameterInterface) {
                is ParameterInterface.ParameterInline -> parameterInterface.parameter
                is ParameterInterface.ParameterReference -> parameterInterface.reference
            }
            results.error(
                PARAMETER_NAME_FORMAT,
                "$contextString name must contain only letters, digits, underscores, or hyphens.",
                sourceLocation = asyncApiContext.getSourceLocation(model),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#parametersObject",
            )
        }
        when (parameterInterface) {
            is ParameterInterface.ParameterInline ->
                validate(parameterInterface.parameter, contextString, results)
            is ParameterInterface.ParameterReference ->
                referenceResolver.resolve(parameterInterface.reference, PARAMETER, contextString, results)
        }
    }

    fun validate(node: Parameter, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateEnum(node, contextString, results)
        validateDefault(node, contextString, results)
        validateExamples(node, contextString, results)
        validateLocation(node, contextString, results)
    }

    private fun validateEnum(node: Parameter, contextString: String, results: ValidationCollector) {
        val enum = node.enum ?: return
        if (enum.distinct().size != enum.size) {
            results.error(
                PARAMETER_ENUM_UNIQUE,
                "$contextString 'enum' contains duplicate values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
            )
        }
    }

    private fun validateDefault(node: Parameter, contextString: String, results: ValidationCollector) {
        val default = node.default ?: return
        val enum = node.enum ?: return
        if (!enum.contains(default)) {
            results.warn(
                PARAMETER_DEFAULT_ENUM,
                "$contextString 'default' value ('$default') is not included in the allowed enum values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::default),
            )
        }
    }

    private fun validateExamples(node: Parameter, contextString: String, results: ValidationCollector) {
        val examples = node.examples ?: return
        val enum = node.enum
        if (enum != null && examples.any { it !in enum }) {
            results.warn(
                PARAMETER_EXAMPLES_ENUM,
                "$contextString 'examples' are not part of the defined enum values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::examples),
            )
        }
    }

    private fun validateLocation(node: Parameter, contextString: String, results: ValidationCollector) {
        val location = node.location ?: return
        if (!ValidationFormats.isRuntimeExpression(location)) {
            results.error(
                PARAMETER_LOCATION_FORMAT,
                $$"$$contextString invalid 'location' expression '$$location'. Must be a valid " +
                    $$"runtime expression (e.g., $message.header#/param).",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::location),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#parameterObject",
            )
        }
    }
}
