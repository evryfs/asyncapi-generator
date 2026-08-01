package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER

/**
 * Parses AsyncAPI parameter objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `ParameterParserTest`
 * - `ChannelParserTest`
 */
class ParameterParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseMap(parserNode: ParserNode): Map<String, ParameterInterface> = buildMap {
        val nodes = parserNode.members()
        nodes.forEach { node ->
            val reference = node.optional($$"$ref")?.expect<String>()
            val parameterInterface = if (reference != null) {
                ParameterInterface.ParameterReference(
                    Reference(
                        ref = reference,
                        referenceCategoryKey = PARAMETER,
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                ParameterInterface.ParameterInline(
                    Parameter(
                        description = node.optional("description")?.expect<String>(),
                        location = node.optional("location")?.expect<String>(),
                        enum = node.optional("enum")?.expect<List<String>>(),
                        default = node.optional("default")?.expect<String>(),
                        examples = node.optional("examples")?.expect<List<String>>(),
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, parameterInterface)
        }
    }
}
