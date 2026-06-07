package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface

/**
 * Bundles server variable objects and references.
 *
 * Expected behavior is covered by:
 * - `ServerVariableBundlerTest`
 */
class ServerVariableBundler {

    fun bundleMap(
        variables: Map<String, ServerVariableInterface>?,
        visited: Set<String>
    ): Map<String, ServerVariableInterface>? =
        bundleMap(variables, BundlingContext.from(visited))

    fun bundleMap(
        variables: Map<String, ServerVariableInterface>?,
        context: BundlingContext,
    ): Map<String, ServerVariableInterface>? =
        variables?.mapValues { (_, variable) ->
            bundle(variable, context)
        }

    fun bundle(variable: ServerVariableInterface, visited: Set<String>): ServerVariableInterface =
        bundle(variable, BundlingContext.from(visited))

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
