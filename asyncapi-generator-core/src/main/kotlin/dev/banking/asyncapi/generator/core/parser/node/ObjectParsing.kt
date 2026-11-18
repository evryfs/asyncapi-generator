package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType

/**
 * Parses this node as an [objectType] object: checks its members against the object's fixed members,
 * builds the model and registers it with this node.
 */
internal inline fun <M : Any> ParserNode.parseObject(
    objectType: AsyncApiObjectType,
    build: (ParserObjectNode) -> M,
): M {
    val objectNode = expectObject()
    objectNode.expectOnlyMembers(objectType)
    return build(objectNode).also { model -> context.register(model, this) }
}

/**
 * The `$ref` of this object node as a registered [Reference] to a [category] object, or `null` when
 * the node has no `$ref`.
 */
internal fun ParserNode.referenceOrNull(category: ReferenceCategoryKey): Reference? =
    expectObject().optional($$"$ref")?.expect<String>()?.let { ref ->
        Reference(ref = ref, referenceCategoryKey = category).also { reference -> context.register(reference, this) }
    }

/**
 * Parses this node as either a `$ref` to a [category] object or an inline [objectType] object.
 *
 * A node with a `$ref` member is a reference, whatever its other members are.
 */
internal inline fun <T, M : Any> ParserNode.parseReferenceable(
    category: ReferenceCategoryKey,
    objectType: AsyncApiObjectType,
    reference: (Reference) -> T,
    inline: (M) -> T,
    build: (ParserObjectNode) -> M,
): T {
    val ref = referenceOrNull(category)
    return if (ref != null) reference(ref) else inline(parseObject(objectType, build))
}

/** Parses every member of this object node with [parseMember], keyed by member name. */
internal inline fun <T> ParserNode.parseMap(parseMember: (ParserNode) -> T): Map<String, T> =
    expectObject().members().associate { member -> member.name to parseMember(member) }

/** Parses every element of this array node with [parseElement]. */
internal inline fun <T> ParserNode.parseList(parseElement: (ParserNode) -> T): List<T> =
    expectArray().elements().map { element -> parseElement(element) }
