package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Bundles binding objects and references.
 */
internal class BindingBundler {
    private val schemaBundler by lazy { SchemaBundler() }

    fun bundleMap(
        bindings: Map<String, BindingInterface>?,
        scope: BundlingScope,
    ): Map<String, BindingInterface>? =
        bindings?.mapValues { (_, binding) ->
            bundle(binding, scope)
        }

    fun bundle(
        binding: BindingInterface,
        scope: BundlingScope,
    ): BindingInterface =
        when (binding) {
            is BindingInterface.BindingInline ->
                BindingInterface.BindingInline(bundleBinding(binding.binding, scope))

            is BindingInterface.BindingReference -> {
                binding.copy(
                    reference =
                        ReferenceBundler.bundleReferencedModel<Binding>(
                            binding.reference,
                            scope,
                        ) { model, nextScope ->
                            bundleBinding(model, nextScope)
                        },
                )
            }
        }

    private fun bundleBinding(
        binding: Binding,
        scope: BundlingScope,
    ): Binding {
        if (binding.protocolBindings.none { it.schemaFields.isNotEmpty() }) return binding
        var bundledContent = binding.content
        val bundledProtocolBindings =
            binding.protocolBindings.map { protocolBinding ->
                val bundledFields =
                    protocolBinding.schemaFields.mapValues { (_, schema) ->
                        // The message key becomes a generated type, so it keeps the name of its source pointer.
                        val isKey = schema === binding.kafkaKeySchema
                        if (isKey) {
                            schemaBundler.bundleBindingField(schema, scope)
                        } else {
                            schemaBundler.bundle(schema, scope)
                        }
                    }
                // Raw content is only replaced where bundling changed the schema.
                val changedFields =
                    bundledFields.filter { (name, bundled) ->
                        bundled != protocolBinding.schemaFields[name]
                    }
                bundledContent = bundledContent.withBundledFields(protocolBinding.protocol, changedFields)
                protocolBinding.copy(schemaFields = bundledFields)
            }
        return binding.copy(
            content = bundledContent,
            protocolBindings = bundledProtocolBindings,
        )
    }

    /** A protocol-keyed binding content holds the fields under the protocol; a single protocol's content holds them directly. */
    private fun Map<String, Any?>.withBundledFields(
        protocol: String,
        fields: Map<String, SchemaInterface>,
    ): Map<String, Any?> {
        val protocolContent = get(protocol) as? Map<*, *>
        return if (protocolContent != null) {
            this + (protocol to (protocolContent + fields.filterKeys(protocolContent::containsKey)))
        } else {
            this + fields.filterKeys(::containsKey)
        }
    }
}
