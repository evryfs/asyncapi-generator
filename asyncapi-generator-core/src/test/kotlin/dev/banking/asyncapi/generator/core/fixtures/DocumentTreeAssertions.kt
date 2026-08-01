package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentNode
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentNumber
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentString
import kotlin.test.fail

/**
 * Assertion helper for traversing reader-stage document trees in tests.
 *
 * This keeps tests focused on behavior instead of unchecked casts and provides
 * a domain-oriented failure message when a fixture no longer has the expected
 * object shape.
 */
internal fun DocumentObject.childObject(key: String): DocumentObject {
    val value = this[key]
    if (value !is DocumentObject) {
        fail("Expected '$key' to be an object, but was ${value?.let(::documentTypeName) ?: "absent"}")
    }

    return value
}

internal fun DocumentObject.value(key: String): Any? = this[key]?.semanticValue()

internal fun DocumentNode.semanticValue(): Any? =
    when (this) {
        is DocumentObject -> members.mapValuesTo(linkedMapOf()) { (_, member) ->
            member.value.semanticValue()
        }

        is DocumentArray -> elements.map(DocumentNode::semanticValue)
        is DocumentString -> value
        is DocumentNumber -> value
        is DocumentBoolean -> value
        is DocumentNull -> null
    }

private fun documentTypeName(node: DocumentNode): String =
    when (node) {
        is DocumentObject -> "object"
        is DocumentArray -> "array"
        is DocumentString -> "string"
        is DocumentNumber -> "number"
        is DocumentBoolean -> "boolean"
        is DocumentNull -> "null"
    }
