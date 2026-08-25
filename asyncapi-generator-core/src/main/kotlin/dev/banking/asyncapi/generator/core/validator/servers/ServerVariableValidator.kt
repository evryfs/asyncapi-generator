package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.resolver.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class ServerVariableValidator(
    val asyncApiContext: AsyncApiContext,
) {

    private val referenceResolver = ReferenceResolver(asyncApiContext)

    fun validateInterface(node: ServerVariableInterface, contextString: String, results: ValidationCollector) {
        when (node) {
            is ServerVariableInterface.ServerVariableInline ->
                validate(node.serverVariable, contextString, results)

            is ServerVariableInterface.ServerVariableReference ->
                referenceResolver.resolve(node.reference, SERVER_VARIABLE, contextString, results)
        }
    }

    fun validateMap(variables: Map<String, ServerVariableInterface>?, contextString: String, results: ValidationCollector) {
        variables?.forEach { (name, serverVariableInterface) ->
            validateInterface(
                serverVariableInterface,
                "$contextString Server Variable '$name'",
                results,
            )
        }
    }

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
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::enum),
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
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::default),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
    }

    private fun validateExamples(node: ServerVariable, contextString: String, results: ValidationCollector) {
        val examples = node.examples ?: return
        val enum = node.enum
        if (enum != null && examples.any { it !in enum }) {
            results.warn(
                SERVER_VARIABLE_EXAMPLES_ENUM,
                "$contextString, some 'examples' values are not included in the allowed enum values.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::examples),
                doc = "https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverVariableObject",
            )
        }
    }
}
