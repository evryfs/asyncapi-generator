package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Bundles the schema fields of a binding, keeping the binding content as written where bundling changed nothing.
 *
 * @param walk bundles the schema fields
 */
internal class BindingBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [binding] with its schema fields bundled. */
    fun bundle(
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
                            walk.schemas.bundleBindingField(schema, scope)
                        } else {
                            walk.schemas.bundle(schema, scope)
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
