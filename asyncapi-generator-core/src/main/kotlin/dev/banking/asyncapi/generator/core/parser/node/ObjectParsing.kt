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
 * the node has no `$ref`. [parseTarget] parses the node the reference points at.
 */
internal fun ParserNode.referenceOrNull(
    category: ReferenceCategoryKey,
    parseTarget: (ParserNode) -> Any,
): Reference? =
    expectObject().optional($$"$ref")?.expect<String>()?.let { ref ->
        Reference(ref = ref, referenceCategoryKey = category)
            .also { reference -> context.registerReference(reference, this, parseTarget) }
    }

/**
 * Parses this node as either a `$ref` to a [category] object or an inline [objectType] object.
 *
 * A node with a `$ref` member is a reference, whatever its other members are. The node a reference
 * points at is parsed the same way.
 */
internal fun <T : Any, M : Any> ParserNode.parseReferenceable(
    category: ReferenceCategoryKey,
    objectType: AsyncApiObjectType,
    reference: (Reference) -> T,
    inline: (M) -> T,
    build: (ParserObjectNode) -> M,
): T {
    val ref =
        referenceOrNull(
            category,
        ) { target -> target.parseReferenceable(category, objectType, reference, inline, build) }
    return if (ref != null) reference(ref) else inline(parseObject(objectType, build))
}

/** Parses every member of this object node with [parseMember], keyed by member name. */
internal inline fun <T> ParserNode.parseMap(parseMember: (ParserNode) -> T): Map<String, T> =
    expectObject().members().mapValues { (_, member) -> parseMember(member) }

/** Parses every element of this array node with [parseElement]. */
internal inline fun <T> ParserNode.parseList(parseElement: (ParserNode) -> T): List<T> =
    expectArray().elements().map { element -> parseElement(element) }
