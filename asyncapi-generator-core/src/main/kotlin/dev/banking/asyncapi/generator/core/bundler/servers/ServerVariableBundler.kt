package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface

/**
 * Bundles server variable objects and references.
 */
internal class ServerVariableBundler {
    fun bundleMap(
        variables: Map<String, ServerVariableInterface>?,
        context: BundlingContext,
    ): Map<String, ServerVariableInterface>? =
        variables?.mapValues { (_, variable) ->
            bundle(variable, context)
        }
    fun bundle(variable: ServerVariableInterface, context: BundlingContext): ServerVariableInterface =
        when (variable) {
            is ServerVariableInterface.ServerVariableInline -> {
                variable
            }
            is ServerVariableInterface.ServerVariableReference -> {
                ReferenceBundler.inlineIfUnvisited(variable.reference, context)
                variable
            }
        }
}
