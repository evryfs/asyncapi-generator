package dev.banking.asyncapi.generator.core.bundler.parameters

import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface

class ParameterBundler {

    fun bundleMap(
        parameters: Map<String, ParameterInterface>?,
        visited: Set<String>,
    ): Map<String, ParameterInterface>? =
        parameters?.mapValues { (_, param) ->
            when (param) {
                is ParameterInterface.ParameterReference -> {
                    val ref = param.reference.ref
                    if (visited.contains(ref)) {
                        param
                    } else {
                        param.reference.inline()
                        param
                    }
                }
                else -> param
            }
        }
}
