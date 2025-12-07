package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface


class BindingBundler {

    fun bundleMap(
        bindings: Map<String, BindingInterface>?,
        visited: Set<String>
    ): Map<String, BindingInterface>? =
        bindings?.mapValues { (_, binding) ->
            bundle(binding, visited)
        }

    fun bundle(binding: BindingInterface, visited: Set<String>): BindingInterface =
        when (binding) {
            is BindingInterface.BindingInline -> {
                binding
            }
            is BindingInterface.BindingReference -> {
                val ref = binding.reference.ref
                if (visited.contains(ref)) {
                    binding
                } else {
                    binding.reference.inline()
                    binding
                }
            }
        }
}
