package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.validator.ReferenceResolver
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValue
import dev.banking.asyncapi.generator.core.validator.util.EnumConstrainedValueRules
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

internal class ServerVariableValidator(
    val asyncApiContext: AsyncApiContext,
) {
    private val referenceResolver = ReferenceResolver(asyncApiContext)
    private val valueRules =
        EnumConstrainedValueRules(
            SERVER_VARIABLE_ENUM_UNIQUE,
            SERVER_VARIABLE_DEFAULT_ENUM,
            SERVER_VARIABLE_EXAMPLES_ENUM,
        )

    fun validateInterface(
        node: ServerVariableInterface,
        contextString: String,
        results: ValidationCollector,
    ) {
        when (node) {
            is ServerVariableInterface.ServerVariableInline ->
                validate(node.serverVariable, contextString, results)

            is ServerVariableInterface.ServerVariableReference ->
                referenceResolver.resolve(node.reference, SERVER_VARIABLE, contextString, results)
        }
    }

    fun validateMap(
        variables: Map<String, ServerVariableInterface>?,
        contextString: String,
        results: ValidationCollector,
    ) {
        variables?.forEach { (name, serverVariableInterface) ->
            validateInterface(
                serverVariableInterface,
                "$contextString Server Variable '$name'",
                results,
            )
        }
    }

    fun validate(
        node: ServerVariable,
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
    }
}
