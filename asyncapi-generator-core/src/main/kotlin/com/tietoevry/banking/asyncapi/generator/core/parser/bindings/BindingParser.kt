package com.tietoevry.banking.asyncapi.generator.core.parser.bindings

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.Binding
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

class BindingParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, BindingInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val reference = node.optional($$"$ref")?.coerce<String>()
            val binding = if (reference != null) {
                BindingInterface.BindingReference(
                    Reference(
                        ref = reference
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                val content = node.coerce<Map<String, Any?>>()
                BindingInterface.BindingInline(
                    Binding(
                        content = content
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, binding)
        }
    }
}
