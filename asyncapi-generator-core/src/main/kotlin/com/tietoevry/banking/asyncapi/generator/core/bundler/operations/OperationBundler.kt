package com.tietoevry.banking.asyncapi.generator.core.bundler.operations

import com.tietoevry.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.operations.Operation
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationInterface

class OperationBundler {

    private val tagBundler: TagBundler = TagBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()
    private val securitySchemeBundler: SecuritySchemeBundler = SecuritySchemeBundler()
    private val bindingBundler = BindingBundler()
    private val operationTraitBundler = OperationTraitBundler()
    private val operationReplyBundler = OperationReplyBundler()

    fun bundleMap(
        operations: Map<String, OperationInterface>?,
        visited: Set<String>,
    ): Map<String, OperationInterface>? {
        if (operations == null) return null
        return operations.mapValues { (_, opInterface) ->
            when (opInterface) {
                is OperationInterface.OperationInline ->
                    OperationInterface.OperationInline(
                        bundleOperation(opInterface.operation, visited)
                    )

                is OperationInterface.OperationReference -> {
                    val ref = opInterface.reference.ref
                    if (visited.contains(ref)) {
                        opInterface
                    } else {
                        val opModel = opInterface.reference.requireModel<Operation>()
                        val newVisited = visited + ref
                        val bundledOp = bundleOperation(opModel, newVisited)
                        opInterface.reference.model = bundledOp
                        opInterface.reference.inline()
                        opInterface
                    }
                }
            }
        }
    }

    fun bundleOperation(operation: Operation, visited: Set<String>): Operation {
        val bundledBindings = bindingBundler.bundleMap(operation.bindings, visited)
        val bundledTraits = operationTraitBundler.bundleList(operation.traits, visited)
        val bundledTags = tagBundler.bundleList(operation.tags, visited)
        val bundledExternalDocs = operation.externalDocs?.let { externalDocsBundler.bundle(it, visited) }
        val bundledReply = operation.reply?.let { operationReplyBundler.bundle(it, visited) }
        val bundledSecurity = securitySchemeBundler.bundleList(operation.security, visited)
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
