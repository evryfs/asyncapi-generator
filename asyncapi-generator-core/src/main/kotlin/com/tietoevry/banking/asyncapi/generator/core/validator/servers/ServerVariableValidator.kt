package com.tietoevry.banking.asyncapi.generator.core.validator.servers

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerVariable
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidatorUtility.sanitizeString

class ServerVariableValidator(
    val asyncApiContext: AsyncApiContext,
) {

    fun validate(serverVariableName: String, node: ServerVariable, results: ValidationResults) {
        validateEnum(node, serverVariableName, results)
        validateDefault(node, serverVariableName, results)
        validateExamples(node, serverVariableName, results)
        validateDescription(node, serverVariableName, results)
    }

    private fun validateEnum(node: ServerVariable, serverVariableName: String, results: ValidationResults) {
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) } ?: return
        if (enum.isEmpty()) {
            results.warn(
                "Server variable '$serverVariableName' 'enum' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::enum)
            )
        }
        if (enum.distinct().size != enum.size) {
            results.warn(
                "Server variable '$serverVariableName' 'enum' contains duplicate values.",
                asyncApiContext.getLine(node, node::enum)
            )
        }
    }

    private fun validateDefault(node: ServerVariable, serverVariableName: String, results: ValidationResults) {
        val default = node.default?.let(::sanitizeString)
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
        if (default == null) {
            results.error(
                "Server variable '$serverVariableName' must specify a 'default' value.",
                asyncApiContext.getLine(node, node::default)
            )
            return
        }
        if (enum != null && !enum.contains(default)) {
            results.error(
                "Server variable '$serverVariableName' 'default' ('$default') is not one of the allowed enum values.",
                asyncApiContext.getLine(node, node::default)
            )
        }
    }

    private fun validateExamples(node: ServerVariable, serverVariableName: String, results: ValidationResults) {
        val examples = node.examples?.map { example -> example.let(::sanitizeString) } ?: return
        if (examples.isEmpty()) {
            results.warn(
                "Server variable '$serverVariableName' 'examples' list is empty — omit it if unused.",
                asyncApiContext.getLine(node, node::examples)
            )
        }
        val enum = node.enum?.map { enum -> enum.let(::sanitizeString) }
        if (enum != null && examples.any { it !in enum }) {
            results.warn(
                "Server variable '$serverVariableName', some 'examples' values are not included in the allowed enum values.",
                asyncApiContext.getLine(node, node::examples)
            )
        }
    }

    private fun validateDescription(node: ServerVariable, serverVariableName: String, results: ValidationResults) {
        val description = node.description?.let(::sanitizeString) ?: return
        if (description.length < 3) {
            results.warn(
                "Server variable '$serverVariableName' description is too short to be meaningful.",
                asyncApiContext.getLine(node, node::description)
            )
        }
    }
}
