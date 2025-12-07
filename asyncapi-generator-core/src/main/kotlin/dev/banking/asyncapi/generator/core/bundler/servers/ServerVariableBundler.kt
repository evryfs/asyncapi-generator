package dev.banking.asyncapi.generator.core.bundler.servers

import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface

class ServerVariableBundler {

    fun bundleMap(
        variables: Map<String, ServerVariableInterface>?,
        visited: Set<String>
    ): Map<String, ServerVariableInterface>? =
        variables?.mapValues { (_, variable) ->
            bundle(variable, visited)
        }

    fun bundle(variable: ServerVariableInterface, visited: Set<String>): ServerVariableInterface =
        when (variable) {
            is ServerVariableInterface.ServerVariableInline -> {
                variable
            }
            is ServerVariableInterface.ServerVariableReference -> {
                val ref = variable.reference.ref
                if (visited.contains(ref)) {
                    variable
                } else {
                    variable.reference.inline()
                    variable
                }
            }
        }
}
