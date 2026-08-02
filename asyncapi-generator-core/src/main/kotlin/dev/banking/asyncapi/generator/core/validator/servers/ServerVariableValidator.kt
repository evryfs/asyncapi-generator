package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

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
        val enum = node.enum ?: return
        if (enum.distinct().size != enum.size) {
            results.error(
                SERVER_VARIABLE_ENUM_UNIQUE,
                "$contextString 'enum' contains duplicate values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::enum),
            )
        }
    }

    private fun validateDefault(node: ServerVariable, contextString: String, results: ValidationCollector) {
        val default = node.default ?: return
        val enum = node.enum
        if (enum != null && !enum.contains(default)) {
            results.warn(
                SERVER_VARIABLE_DEFAULT_ENUM,
                "$contextString 'default' ('$default') is not one of the allowed enum values.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::default),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
    }

    private fun validateExamples(node: ServerVariable, contextString: String, results: ValidationCollector) {
        val examples = node.examples ?: return
        if (examples.isEmpty()) {
            results.warn(
                SERVER_VARIABLE_EXAMPLES_EMPTY,
                "$contextString 'examples' list is empty — omit it if unused.",
                sourceLocation = asyncApiContext.getSourceLocation(node, node::examples),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
        val enum = node.enum
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
