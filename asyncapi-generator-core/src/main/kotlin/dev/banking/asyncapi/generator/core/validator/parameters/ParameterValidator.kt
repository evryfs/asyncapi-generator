package dev.banking.asyncapi.generator.core.validator.parameters

import dev.banking.asyncapi.generator.core.constants.RegexPatterns.RUNTIME_EXPRESSION_PARAMETER
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ParameterValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(parameterInterface: ParameterInterface, contextString: String, results: ValidationResults) {
        when (parameterInterface) {
            is ParameterInterface.ParameterInline ->
                validate(parameterInterface.parameter, contextString, results)
            is ParameterInterface.ParameterReference ->
                referenceResolver.resolve(parameterInterface.reference, contextString, results)
        }
    }

    fun validate(node: Parameter, contextString: String, results: ValidationResults) {
        validateEnum(node, contextString, results)
        validateDefault(node, contextString, results)
        validateExamples(node, contextString, results)
        validateLocation(node, contextString, results)
    }

    private fun validateEnum(node: Parameter, contextString: String, results: ValidationResults) {
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) } ?: return
        if (enum.distinct().size != enum.size) {
            results.warn(
                "$contextString 'enum' contains duplicate values.",
                asyncApiContext.getLine(node, node::enum),
            )
        }
    }

    private fun validateDefault(node: Parameter, contextString: String, results: ValidationResults) {
        val default = node.default?.let(::sanitizeString) ?: return
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) } ?: return
        if (!enum.contains(default)) {
            results.error(
                "$contextString 'default' value ('$default') is not included in the allowed enum values.",
                asyncApiContext.getLine(node, node::default),
            )
        }
    }

    private fun validateExamples(node: Parameter, contextString: String, results: ValidationResults) {
        val examples = node.examples ?: return
        val enum = node.enum
        if (enum != null && examples.any { it !in enum }) {
            results.warn(
                "$contextString 'examples' are not part of the defined enum values.",
                asyncApiContext.getLine(node, node::examples),
            )
        }
    }

    private fun validateLocation(node: Parameter, contextString: String, results: ValidationResults) {
        val location = node.location?.let(::sanitizeString) ?: return
        if (!RUNTIME_EXPRESSION_PARAMETER.matches(location)) {
            results.error(
                $$"$$contextString invalid 'location' expression '$$location'. Must be a valid " +
                    $$"runtime expression (e.g., $message.header#/param).",
                asyncApiContext.getLine(node, node::location),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#parameterObject",
            )
        }
    }
}
