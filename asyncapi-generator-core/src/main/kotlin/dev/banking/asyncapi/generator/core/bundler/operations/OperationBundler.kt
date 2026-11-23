package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface

/**
 * Bundles operation objects and references.
 */
internal class OperationBundler {
    private val tagBundler = TagBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val securitySchemeBundler = SecuritySchemeBundler()
    private val bindingBundler = BindingBundler()
    private val operationTraitBundler = OperationTraitBundler()
    private val operationReplyBundler = OperationReplyBundler()

    fun bundleMap(
        operations: Map<String, OperationInterface>?,
        scope: BundlingScope,
    ): Map<String, OperationInterface>? {
        if (operations == null) return null
        return operations.mapValues { (_, opInterface) ->
            bundle(opInterface, scope)
        }
    }

    fun bundle(
        operationInterface: OperationInterface,
        scope: BundlingScope,
    ): OperationInterface =
        when (operationInterface) {
            is OperationInterface.OperationInline ->
                OperationInterface.OperationInline(
                    bundleOperation(operationInterface.operation, scope),
                )

            is OperationInterface.OperationReference -> {
                operationInterface.copy(
                    reference =
                        ReferenceBundler.bundleReferencedModel<Operation>(
                            reference = operationInterface.reference,
                            scope = scope,
                        ) { operation, nextScope ->
                            bundleOperation(operation, nextScope)
                        },
                )
            }
        }

    fun bundleOperation(
        operation: Operation,
        scope: BundlingScope,
    ): Operation {
        val bundledBindings = bindingBundler.bundleMap(operation.bindings, scope)
        val bundledTraits = operationTraitBundler.bundleList(operation.traits, scope)
        val bundledTags = tagBundler.bundleList(operation.tags, scope)
        val bundledExternalDocs = operation.externalDocs?.let { externalDocsBundler.bundle(it, scope) }
        val bundledReply = operation.reply?.let { operationReplyBundler.bundle(it, scope) }
        val bundledSecurity = securitySchemeBundler.bundleList(operation.security, scope)
        return operation.copy(
            bindings = bundledBindings,
            traits = bundledTraits,
            tags = bundledTags,
            externalDocs = bundledExternalDocs,
            reply = bundledReply,
            security = bundledSecurity,
        )
    }
}
