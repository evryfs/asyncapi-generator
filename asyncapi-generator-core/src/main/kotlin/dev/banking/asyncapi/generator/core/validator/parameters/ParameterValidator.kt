package dev.banking.asyncapi.generator.core.validator.parameters

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

    fun validateInterface(parameterName: String, parameterInterface: ParameterInterface, results: ValidationResults) {
        when (parameterInterface) {
            is ParameterInterface.ParameterInline ->
                validate(parameterInterface.parameter, parameterName, results)
            is ParameterInterface.ParameterReference ->
                referenceResolver.resolve(parameterName, parameterInterface.reference, "Parameter", results)
        }
    }

    fun validate(node: Parameter, parameterName: String, results: ValidationResults) {
        validateEnum(node, parameterName, results)
        validateDefault(node, parameterName, results)
        validateExamples(node, parameterName, results)
        validateLocation(node, parameterName, results)
        validateDescription(node, parameterName, results)
    }

    private fun validateEnum(node: Parameter, parameterName: String, results: ValidationResults) {
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
            ?: return
        if (enum.isEmpty()) {
            results.warn(
                "Parameter '$parameterName' 'enum' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::enum)
            )
        }
        if (enum.distinct().size != enum.size) {
            results.warn(
                "Parameter '$parameterName' 'enum' contains duplicate values.",
                asyncApiContext.getLine(node, node::enum)
            )
        }
    }

    private fun validateDefault(node: Parameter, parameterName: String, results: ValidationResults) {
        val default = node.default?.let(::sanitizeString)
            ?: return
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
            ?: return
        if (!enum.contains(default)) {
            results.error(
                "Parameter '$parameterName' 'default' value ('$default') is not included in the allowed enum values.",
                asyncApiContext.getLine(node, node::default)
            )
        }
    }

    private fun validateExamples(node: Parameter, parameterName: String, results: ValidationResults) {
        val examples = node.examples ?: return
        if (examples.isEmpty()) {
            results.warn(
                "Parameter '$parameterName' 'examples' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::examples)
            )
        }
        val enum = node.enum
        if (enum != null && examples.any { it !in enum }) {
            results.warn(
                "Some Parameter 'examples' are not part of the defined enum values.",
                asyncApiContext.getLine(node, node::examples)
            )
        }
    }

    private fun validateLocation(node: Parameter, parameterName: String, results: ValidationResults) {
        val location = node.location?.let(::sanitizeString)
        if (location.isNullOrBlank()) {
            results.warn(
                "Parameter '$parameterName' is missing a 'location' expression — runtime substitution may fail.",
                asyncApiContext.getLine(node, node::location)
            )
            return
        }
        val runtimeRegex = Regex("""^\$(message|context)(\.[A-Za-z0-9_-]+)*(#/[-A-Za-z0-9_/]+)?$""")
        if (!runtimeRegex.matches(location)) {
            results.error(
                $$"Parameter '$$parameterName' invalid 'location' expression '$$location'. Must be a valid " +
                    $$"runtime expression (e.g., $message.header#/param).",
                asyncApiContext.getLine(node, node::location)
            )
        }
    }

    private fun validateDescription(node: Parameter, parameterName: String, results: ValidationResults) {
        val description = node.description?.let(::sanitizeString)
            ?: return
        if (description.length < 3) {
            results.warn(
                "Parameter '$parameterName' description is too short to be meaningful.",
                asyncApiContext.getLine(node, node::description)
            )
        }
    }
}
