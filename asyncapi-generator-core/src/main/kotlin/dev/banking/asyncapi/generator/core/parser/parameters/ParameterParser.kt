package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.PARAMETER as PARAMETER_OBJECT

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
        parserNode.expectObject().members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): ParameterInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            ParameterInterface.ParameterReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = PARAMETER,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            objectNode.expectOnlyMembers(PARAMETER_OBJECT)
            ParameterInterface.ParameterInline(
                Parameter(
                    description = objectNode.optional("description")?.expect<String>(),
                    location = objectNode.optional("location")?.expect<String>(),
                    enum = objectNode.optional("enum")?.expect<List<String>>(),
                    default = objectNode.optional("default")?.expect<String>(),
                    examples = objectNode.optional("examples")?.expect<List<String>>(),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
