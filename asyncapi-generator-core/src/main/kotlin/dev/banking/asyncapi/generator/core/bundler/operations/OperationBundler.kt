package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface

/**
 * Bundles operation objects and references.
 *
 * Expected behavior is covered by:
 * - `OperationBundlerTest`
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
        context: BundlingContext,
    ): Map<String, OperationInterface>? {
        if (operations == null) return null
        return operations.mapValues { (_, opInterface) ->
            bundle(opInterface, context)
        }
    }
    fun bundle(operationInterface: OperationInterface, context: BundlingContext): OperationInterface =
        when (operationInterface) {
            is OperationInterface.OperationInline ->
                OperationInterface.OperationInline(
                    bundleOperation(operationInterface.operation, context)
                )

            is OperationInterface.OperationReference -> {
                ReferenceBundler.bundleReferencedModel<Operation>(
                    reference = operationInterface.reference,
                    context = context,
                ) { operation, nextContext ->
                    bundleOperation(operation, nextContext)
                }
                operationInterface
            }
        }
    fun bundleOperation(operation: Operation, context: BundlingContext): Operation {
        val bundledBindings = bindingBundler.bundleMap(operation.bindings, context)
        val bundledTraits = operationTraitBundler.bundleList(operation.traits, context)
        val bundledTags = tagBundler.bundleList(operation.tags, context)
        val bundledExternalDocs = operation.externalDocs?.let { externalDocsBundler.bundle(it, context) }
        val bundledReply = operation.reply?.let { operationReplyBundler.bundle(it, context) }
        val bundledSecurity = securitySchemeBundler.bundleList(operation.security, context)
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
