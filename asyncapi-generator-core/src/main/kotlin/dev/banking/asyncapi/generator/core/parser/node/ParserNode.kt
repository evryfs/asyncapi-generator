@file:Suppress("UNCHECKED_CAST")

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
        val currentNodeMap = node as? DocumentObject
            ?: throw AsyncApiParseException.Mandatory(nodeKey, path, context)
        val childNode = currentNodeMap[nodeKey]
            ?.takeUnless { it is DocumentNull }
            ?: throw AsyncApiParseException.Mandatory(nodeKey, "$path.$nodeKey", context)
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context)
    }

    fun optional(nodeKey: String): ParserNode? {
        val currentNodeMap = node as? DocumentObject
            ?: return null
        val childNode = currentNodeMap[nodeKey]
            ?.takeUnless { it is DocumentNull }
            ?: return null
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context)
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

    fun normalize(value: Any?): Any? {
        val dataToNormalize = if (value is ParserNode) {
            value.node
        } else {
            value
        }
        return when (dataToNormalize) {
            is DocumentNode -> dataToNormalize.toValue()
            else -> dataToNormalize
        }
    }
}
