package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedValue
import dev.banking.asyncapi.generator.core.reader.DocumentArray
import dev.banking.asyncapi.generator.core.reader.DocumentNode
import dev.banking.asyncapi.generator.core.reader.DocumentNull
import dev.banking.asyncapi.generator.core.reader.DocumentObject
import dev.banking.asyncapi.generator.core.reader.toValue

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

    fun mandatory(nodeKey: String): ParserNode {
        val currentNode = node as? DocumentObject
            ?: throw AsyncApiParseException.Mandatory(nodeKey, path, context)
        val childNode = currentNode[nodeKey]
            ?.takeUnless { child -> child is DocumentNull }
            ?: throw AsyncApiParseException.Mandatory(nodeKey, "$path.$nodeKey", context)
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context)
    }

    fun optional(nodeKey: String): ParserNode? {
        val currentNode = node as? DocumentObject
            ?: return null
        val childNode = currentNode[nodeKey]
            ?.takeUnless { child -> child is DocumentNull }
            ?: return null
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context)
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
}
