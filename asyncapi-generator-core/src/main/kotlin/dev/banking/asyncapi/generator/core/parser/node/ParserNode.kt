package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentNode
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.toValue
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import kotlin.reflect.typeOf

/**
 * Represents one parser input node together with its source path and context.
 *
 * Expected behavior is covered by:
 * - `ParserNodeTest`
 * - parser package tests
 */
data class ParserNode(
    val name: String,
    val node: DocumentNode,
    val path: String,
    val context: AsyncApiContext,
    val profile: AsyncApiParserProfile? = null,
) {

    fun withProfile(profile: AsyncApiParserProfile): ParserNode = copy(profile = profile)

    fun required(nodeKey: String): ParserNode {
        val currentNode = objectNode()
        val childPath = "$path.$nodeKey"
        val childNode = currentNode[nodeKey]
            ?: throw AsyncApiParseException.ParserDiagnosticFailure(
                diagnostic = ParserDiagnostic.MissingRequiredMember(
                    memberName = nodeKey,
                    path = childPath,
                    sourceLocation = currentNode.location,
                ),
                context = context,
            )
        return ParserNode(nodeKey, childNode, childPath, context, profile)
    }

    fun optional(nodeKey: String): ParserNode? {
        val currentNodeMap = objectNode()
        val childNode = currentNodeMap[nodeKey]
            ?: return null
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context, profile)
    }

    fun members(): List<ParserNode> =
        objectNode().members.map { (memberName, member) ->
            ParserNode(memberName, member.value, "$path.$memberName", context, profile)
        }

    fun elements(): List<ParserNode> =
        arrayNode().elements.mapIndexed { index, element ->
            ParserNode("$name[$index]", element, "$path[$index]", context, profile)
        }

    fun startsWith(prefix: String): ParserNode? {
        val currentMap = node as? DocumentObject
            ?: return null
        val matchingEntries = currentMap.members.filter { (key, _) ->
            key.startsWith(prefix)
        }
        if (matchingEntries.isEmpty()) {
            return null
        }
        return ParserNode(
            "$name(prefix:$prefix)",
            DocumentObject(matchingEntries, currentMap.location),
            "$path.(prefix:$prefix)",
            context,
            profile,
        )
    }

    inline fun <reified T> expect(): T =
        ParserValueExpectation.cast(
            ParserValueExpectation.expect(
                node = node,
                expectedType = typeOf<T>(),
                path = path,
                context = context,
            ),
        )

    /** Converts this source-located node to plain maps, lists, scalars, or null. */
    fun toPlainValue(): Any? = node.toValue()

    private fun objectNode(): DocumentObject =
        node as? DocumentObject
            ?: ParserValueExpectation.unexpectedType(
                node = node,
                expectedType = typeOf<Map<String, Any?>>(),
                path = path,
                context = context,
            )

    private fun arrayNode(): DocumentArray =
        node as? DocumentArray
            ?: ParserValueExpectation.unexpectedType(
                node = node,
                expectedType = typeOf<List<Any?>>(),
                path = path,
                context = context,
            )
}
