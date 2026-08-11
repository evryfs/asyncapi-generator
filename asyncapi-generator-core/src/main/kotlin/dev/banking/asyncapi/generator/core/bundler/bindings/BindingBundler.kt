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
        val bundledContent = binding.protocolBindings.fold(binding.content) { content, protocolBinding ->
            protocolBinding.schemaFields.entries.fold(content) { currentContent, (fieldName, schema) ->
                currentContent.withBundledSchema(
                    protocol = protocolBinding.protocol,
                    fieldName = fieldName,
                    schema = schemaBundler.bundle(schema, context),
                )
            }
        }
        if (bundledContent === binding.content) return binding
        return binding.copy(
            content = bundledContent,
        )
    }

    private fun Map<String, Any?>.withBundledSchema(
        protocol: String,
        fieldName: String,
        schema: SchemaInterface,
    ): Map<String, Any?> {
        val protocolContent = get(protocol) as? Map<*, *>
        return when {
            protocolContent?.containsKey(fieldName) == true ->
                this + (protocol to (protocolContent + (fieldName to schema)))

            containsKey(fieldName) ->
                this + (fieldName to schema)

            else -> this
        }
    }
}
