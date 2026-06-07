package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface


/**
 * Bundles binding objects and references.
 *
 * Expected behavior is covered by:
 * - `BindingBundlerTest`
 */
class BindingBundler {

    fun bundleMap(
        bindings: Map<String, BindingInterface>?,
        visited: Set<String>
    ): Map<String, BindingInterface>? =
        bundleMap(bindings, BundlingContext.from(visited))

    fun bundleMap(
        bindings: Map<String, BindingInterface>?,
        context: BundlingContext,
    ): Map<String, BindingInterface>? =
        bindings?.mapValues { (_, binding) ->
            bundle(binding, context)
        }

    fun bundle(binding: BindingInterface, visited: Set<String>): BindingInterface =
        bundle(binding, BundlingContext.from(visited))

    fun bundle(binding: BindingInterface, context: BundlingContext): BindingInterface =
        when (binding) {
            is BindingInterface.BindingInline -> {
                binding
            }
            is BindingInterface.BindingReference -> {
                ReferenceBundler.inlineIfUnvisited(binding.reference, context)
                binding
            }
        }
}
