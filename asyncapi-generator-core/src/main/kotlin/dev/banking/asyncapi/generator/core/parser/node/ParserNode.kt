package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedValue
import dev.banking.asyncapi.generator.core.reader.DocumentArray
import dev.banking.asyncapi.generator.core.reader.DocumentNode
import dev.banking.asyncapi.generator.core.reader.DocumentObject
import dev.banking.asyncapi.generator.core.reader.toValue
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
) {

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
        return ParserNode(nodeKey, childNode, childPath, context)
    }

    fun optional(nodeKey: String): ParserNode? {
        val currentNode = objectNode()
        val childNode = currentNode[nodeKey] ?: return null
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context)
    }

    fun members(): List<ParserNode> =
        objectNode().members.map { (memberName, member) ->
            ParserNode(memberName, member.value, "$path.$memberName", context)
        }

    fun elements(): List<ParserNode> =
        arrayNode().elements.mapIndexed { index, element ->
            ParserNode("$name[$index]", element, "$path[$index]", context)
        }

    fun startsWith(prefix: String): ParserNode? {
        val currentNode = node as? DocumentObject
            ?: return null
        val matchingMembers = currentNode.members.filter { (key, _) ->
            key.startsWith(prefix)
        }
        if (matchingMembers.isEmpty()) {
            return null
        }
        return ParserNode(
            name = "$name(prefix:$prefix)",
            node = DocumentObject(matchingMembers, currentNode.location),
            path = "$path.(prefix:$prefix)",
            context = context,
        )
    }

    fun extractNodes(): List<ParserNode> = when (val currentNodeValue = node) {
        is DocumentObject -> {
            currentNodeValue.members
                .map { (key, member) ->
                    ParserNode(key, member.value, "$path.$key", context)
                }
        }
        is DocumentArray -> {
            currentNodeValue.elements.mapIndexed { index, value ->
                ParserNode("$name[$index]", value, "$path[$index]", context)
            }
        }
        else -> {
            val foundType = currentNodeValue.toValue()?.javaClass?.simpleName ?: "null"
            throw UnexpectedValue(foundType, "Map/List", path, context)
        }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> expect(): T =
        ParserValueExpectation.expect(
            node = node,
            expectedType = typeOf<T>(),
            path = path,
            context = context,
        ) as T

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> coerce(): T {
        val normalized = normalize(node)
        val received = normalized?.javaClass?.simpleName ?: "null"
        val expected = T::class.simpleName ?: "null"
        return when (T::class) {
            String::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context, normalized)
            Boolean::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context, normalized)
            Number::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context, normalized)
            List::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context, normalized)
            Map::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context, normalized)
            Any::class -> normalized as T
                ?: throw UnexpectedValue(received, expected, path, context, normalized)
            else -> throw UnexpectedValue(received, expected, path, context, normalized)
        }
    }

    fun normalize(value: Any?): Any? =
        when (value) {
            is ParserNode -> value.node.toValue()
            is DocumentNode -> value.toValue()
            else -> value
        }

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
