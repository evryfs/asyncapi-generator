package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ServerVariableValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: ServerVariable, contextString: String, results: ValidationResults) {
        validateEnum(node, contextString, results)
        validateDefault(node, contextString, results)
        validateExamples(node, contextString, results)
    }

    private fun validateEnum(node: ServerVariable, contextString: String, results: ValidationResults) {
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) } ?: return
        if (enum.distinct().size != enum.size) {
            results.warn(
                "$contextString 'enum' contains duplicate values.",
                asyncApiContext.getLine(node, node::enum)
            )
        }
    }

    private fun validateDefault(node: ServerVariable, contextString: String, results: ValidationResults) {
        val default = node.default?.let(::sanitizeString)
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
        if (default == null) {
            results.error(
                "$contextString must specify a 'default' value.",
                asyncApiContext.getLine(node, node::default),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
            return
        }
        if (enum != null && !enum.contains(default)) {
            results.error(
                "$contextString 'default' ('$default') is not one of the allowed enum values.",
                asyncApiContext.getLine(node, node::default),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
    }

    private fun validateExamples(node: ServerVariable, contextString: String, results: ValidationResults) {
        val examples = node.examples?.map { example -> example.let(::sanitizeString) } ?: return
        if (examples.isEmpty()) {
            results.warn(
                "$contextString 'examples' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::examples),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
        if (enum != null && examples.any { it !in enum }) {
            results.warn(
                "$contextString, some 'examples' values are not included in the allowed enum values.",
                asyncApiContext.getLine(node, node::examples),
                "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
    }
}
