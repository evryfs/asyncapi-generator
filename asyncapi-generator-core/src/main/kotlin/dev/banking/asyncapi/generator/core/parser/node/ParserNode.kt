@file:Suppress("UNCHECKED_CAST")

package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException.UnexpectedValue
import kotlin.collections.get

data class ParserNode(
    val name: String,
    val node: Any?,
    val path: String,
    val context: AsyncApiContext,
) {

    fun mandatory(nodeKey: String): ParserNode {
        val currentNodeMap = node as? Map<*, *>
            ?: throw AsyncApiParseException.Mandatory(nodeKey, path, context)
        val childNode = currentNodeMap[nodeKey]
            ?: throw AsyncApiParseException.Mandatory(nodeKey, "$path.$nodeKey", context)
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context)
    }

    fun optional(nodeKey: String): ParserNode? {
        val currentNodeMap = node as? Map<*, *>
            ?: return null
        val childNode = currentNodeMap[nodeKey]
            ?: return null
        return ParserNode(nodeKey, childNode, "$path.$nodeKey", context)
    }

    fun startsWith(prefix: String): ParserNode? {
        val currentMap = node as? Map<*, *>
            ?: return null
        val matchingEntries = currentMap.filter { (key, _) ->
            key is String && key.startsWith(prefix)
        }
        if (matchingEntries.isEmpty()) {
            return null
        }
        val normalizedNodeEntries = matchingEntries.entries.associate { (key, value) ->
            val keyString = key as String
            keyString to normalize(value)
        }
        return ParserNode("$name(prefix:$prefix)", normalizedNodeEntries, "$path.(prefix:$prefix)", context)
    }

    fun extractNodes(): List<ParserNode> = when (val currentNodeValue = node) {
        is Map<*, *> -> {
            currentNodeValue.entries
                .filter { (key, _) -> key is String }
                .map { (key, value) ->
                    val keyString = key as String
                    ParserNode(keyString, value, "$path.$keyString", context)
                }
        }
        is List<*> -> {
            currentNodeValue.mapIndexed { index, value ->
                ParserNode("$name[$index]", value, "$path[$index]", context)
            }
        }
        else -> {
            val foundType = currentNodeValue?.javaClass?.simpleName ?: "null"
            throw UnexpectedValue(foundType, "Map/List", path, context)
        }
    }

    inline fun <reified T> coerce(): T {
        val normalized = normalize(node)
        val received = normalized?.javaClass?.simpleName ?: "null"
        val expected = T::class.simpleName ?: "null"
        return when (T::class) {
            String::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context)
            Boolean::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context)
            Number::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context)
            List::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context)
            Map::class -> normalized as? T
                ?: throw UnexpectedValue(received, expected, path, context)
            Any::class -> normalized as T
                ?: throw UnexpectedValue(received, expected, path, context)
            else -> throw UnexpectedValue(received, expected, path, context)
        }
    }

    fun normalize(value: Any?): Any? {
        val dataToNormalize = if (value is ParserNode) {
            value.node
        } else {
            value
        }
        return when (dataToNormalize) {
            is Map<*, *> -> dataToNormalize.entries
                .filter { (key, _) -> key is String }
                .associate { (key, value) ->
                    val keyString = key as String
                    keyString to normalize(value)
                }
            is List<*> -> dataToNormalize.map { normalize(it) }
            is String -> when {
                dataToNormalize.equals("true", ignoreCase = true) -> true
                dataToNormalize.equals("false", ignoreCase = true) -> false
                dataToNormalize.toIntOrNull() != null -> dataToNormalize.toInt()
                dataToNormalize.toDoubleOrNull() != null -> dataToNormalize.toDouble()
                else -> dataToNormalize
            }
            else -> dataToNormalize
        }
    }
}
