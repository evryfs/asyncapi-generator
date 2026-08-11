package dev.banking.asyncapi.generator.core.bundler.bindings

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.references.isExternalReference
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
                if (currentContent.schemaField(protocolBinding.protocol, fieldName).containsExternalReference()) {
                    currentContent.withBundledSchema(
                        protocol = protocolBinding.protocol,
                        fieldName = fieldName,
                        schema = schemaBundler.bundle(schema, context),
                    )
                } else {
                    currentContent
                }
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

    private fun Map<String, Any?>.schemaField(protocol: String, fieldName: String): Any? {
        val protocolContent = get(protocol) as? Map<*, *>
        return if (protocolContent?.containsKey(fieldName) == true) {
            protocolContent[fieldName]
        } else {
            get(fieldName)
        }
    }

    private fun Any?.containsExternalReference(): Boolean =
        when (this) {
            is Map<*, *> ->
                (get("\$ref") as? String)?.isExternalReference() == true ||
                    values.any { it.containsExternalReference() }

            is List<*> -> any { it.containsExternalReference() }
            else -> false
        }
}
