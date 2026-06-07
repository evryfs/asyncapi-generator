package dev.banking.asyncapi.generator.core.bundler.parameters

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface

/**
 * Bundles parameter objects and references.
 *
 * Expected behavior is covered by:
 * - `ParameterBundlerTest`
 */
class ParameterBundler {

    fun bundleMap(
        parameters: Map<String, ParameterInterface>?,
        visited: Set<String>,
    ): Map<String, ParameterInterface>? =
        bundleMap(parameters, BundlingContext.from(visited))

    fun bundleMap(
        parameters: Map<String, ParameterInterface>?,
        context: BundlingContext,
    ): Map<String, ParameterInterface>? =
        parameters?.mapValues { (_, param) ->
            bundle(param, context)
        }

    fun bundle(parameter: ParameterInterface, context: BundlingContext): ParameterInterface =
        when (parameter) {
            is ParameterInterface.ParameterReference -> {
                ReferenceBundler.inlineIfUnvisited(parameter.reference, context)
                parameter
            }
            else -> parameter
        }
}
