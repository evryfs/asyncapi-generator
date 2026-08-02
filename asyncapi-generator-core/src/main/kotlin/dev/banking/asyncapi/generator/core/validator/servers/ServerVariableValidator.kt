package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ServerVariableValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(node: ServerVariable, contextString: String, results: ValidationCollector) {
        if (!results.visit(node)) return
        validateEnum(node, contextString, results)
        validateDefault(node, contextString, results)
        validateExamples(node, contextString, results)
    }

    private fun validateEnum(node: ServerVariable, contextString: String, results: ValidationCollector) {
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) } ?: return
        if (enum.distinct().size != enum.size) {
            results.warn(
                SERVER_VARIABLE_ENUM_UNIQUE,
                "$contextString 'enum' contains duplicate values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
            )
        }
    }

    private fun validateDefault(node: ServerVariable, contextString: String, results: ValidationCollector) {
        val default = node.default?.let(::sanitizeString)
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
        if (default == null) {
            results.error(
                SERVER_VARIABLE_DEFAULT_REQUIRED,
                "$contextString must specify a 'default' value.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::default),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
            return
        }
        if (enum != null && !enum.contains(default)) {
            results.error(
                SERVER_VARIABLE_DEFAULT_ENUM,
                "$contextString 'default' ('$default') is not one of the allowed enum values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::default),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
    }

    private fun validateExamples(node: ServerVariable, contextString: String, results: ValidationCollector) {
        val examples = node.examples?.map { example -> example.let(::sanitizeString) } ?: return
        if (examples.isEmpty()) {
            results.warn(
                SERVER_VARIABLE_EXAMPLES_EMPTY,
                "$contextString 'examples' list is empty — omit it if unused.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::examples),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
        if (enum != null && examples.any { it !in enum }) {
            results.warn(
                SERVER_VARIABLE_EXAMPLES_ENUM,
                "$contextString, some 'examples' values are not included in the allowed enum values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::examples),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
    }
}
