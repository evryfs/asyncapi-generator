package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface


/**
 * Bundles binding objects and references.
 *
 * Expected behavior is covered by:
 * - `BindingBundlerTest`
 */
internal class BindingBundler {
    private val schemaBundler by lazy { SchemaBundler() }

    fun bundleMap(
        bindings: Map<String, BindingInterface>?,
        context: BundlingContext,
    ): Map<String, BindingInterface>? =
        bindings?.mapValues { (_, binding) ->
            bundle(binding, context)
        }
    fun bundle(binding: BindingInterface, context: BundlingContext): BindingInterface =
        when (binding) {
            is BindingInterface.BindingInline ->
                BindingInterface.BindingInline(bundleBinding(binding.binding, context))

            is BindingInterface.BindingReference -> {
                ReferenceBundler.bundleReferencedModel<Binding>(binding.reference, context) { model, nextContext ->
                    bundleBinding(model, nextContext)
                }
                binding
            }
        }

    private fun bundleBinding(binding: Binding, context: BundlingContext): Binding {
        val bundledKey = binding.kafkaKeySchema?.let { schemaBundler.bundle(it, context) } ?: return binding
        return binding.copy(
            content = binding.content.withBundledKafkaKey(bundledKey),
        )
    }

    private fun Map<String, Any?>.withBundledKafkaKey(key: SchemaInterface): Map<String, Any?> {
        val kafka = get(KAFKA_PROTOCOL) as? Map<*, *>
        return when {
            kafka?.containsKey(KAFKA_KEY) == true ->
                this + (KAFKA_PROTOCOL to (kafka + (KAFKA_KEY to key)))

            containsKey(KAFKA_KEY) ->
                this + (KAFKA_KEY to key)

            else -> this
        }
    }

    private companion object {
        const val KAFKA_PROTOCOL = "kafka"
        const val KAFKA_KEY = "key"
    }
}
