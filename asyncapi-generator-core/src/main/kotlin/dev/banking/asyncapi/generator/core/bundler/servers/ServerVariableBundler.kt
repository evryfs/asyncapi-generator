package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface

/**
 * Bundles server variable objects and references.
 */
internal class ServerVariableBundler {
    fun bundleMap(
        variables: Map<String, ServerVariableInterface>?,
        scope: BundlingScope,
    ): Map<String, ServerVariableInterface>? =
        variables?.mapValues { (_, variable) ->
            bundle(variable, scope)
        }

    fun bundle(
        variable: ServerVariableInterface,
        scope: BundlingScope,
    ): ServerVariableInterface =
        when (variable) {
            is ServerVariableInterface.ServerVariableInline -> {
                variable
            }
            is ServerVariableInterface.ServerVariableReference -> {
                variable.copy(reference = ReferenceBundler.inlineIfUnvisited(variable.reference, scope))
            }
        }
}
