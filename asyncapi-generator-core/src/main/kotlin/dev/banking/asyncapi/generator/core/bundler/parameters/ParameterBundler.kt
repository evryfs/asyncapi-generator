package dev.banking.asyncapi.generator.core.bundler.parameters

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface

/**
 * Bundles parameter objects and references.
 */
internal class ParameterBundler {
    fun bundleMap(
        parameters: Map<String, ParameterInterface>?,
        scope: BundlingScope,
    ): Map<String, ParameterInterface>? =
        parameters?.mapValues { (_, param) ->
            bundle(param, scope)
        }

    fun bundle(
        parameter: ParameterInterface,
        scope: BundlingScope,
    ): ParameterInterface =
        when (parameter) {
            is ParameterInterface.ParameterReference -> {
                parameter.copy(reference = ReferenceBundler.inlineIfUnvisited(parameter.reference, scope))
            }
            else -> parameter
        }
}
