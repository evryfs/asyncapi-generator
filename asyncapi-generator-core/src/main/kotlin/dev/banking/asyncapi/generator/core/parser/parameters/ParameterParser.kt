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
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): ParameterInterface {
        val reference = parserNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            ParameterInterface.ParameterReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = PARAMETER,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            ParameterInterface.ParameterInline(
                Parameter(
                    description = parserNode.optional("description")?.expect<String>(),
                    location = parserNode.optional("location")?.expect<String>(),
                    enum = parserNode.optional("enum")?.expect<List<String>>(),
                    default = parserNode.optional("default")?.expect<String>(),
                    examples = parserNode.optional("examples")?.expect<List<String>>(),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
