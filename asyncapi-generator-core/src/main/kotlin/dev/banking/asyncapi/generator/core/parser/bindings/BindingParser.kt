package dev.banking.asyncapi.generator.core.parser.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser

/**
 * Parses AsyncAPI binding objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `BindingParserTest`
 */
class BindingParser(
    val asyncApiContext: AsyncApiContext,
) {
    private val schemaParser by lazy { SchemaParser(asyncApiContext) }

    fun parseMap(parserNode: ParserNode): Map<String, BindingInterface> = buildMap {
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): BindingInterface {
        val reference = parserNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            BindingInterface.BindingReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = BINDING,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            val content = parserNode.expect<Map<String, Any?>>()
            BindingInterface.BindingInline(
                Binding(
                    content = content,
                    kafkaKeySchema = parserNode.kafkaKeyNode()?.let(schemaParser::parseElement),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }

    private fun ParserNode.kafkaKeyNode(): ParserNode? =
        if (name == "kafka") {
            optional("key")
        } else {
            optional("kafka")?.optional("key")
        }
}
