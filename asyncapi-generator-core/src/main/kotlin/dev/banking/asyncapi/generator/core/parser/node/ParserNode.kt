package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentNode
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.toValue
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import kotlin.reflect.typeOf

/**
 * Wraps a [DocumentNode] with its source path, navigation address, and parser context.
 *
 * Provides type-safe navigation via [expectObject], [expectArray], and [expect].
 *
 * @param name display path of this node (e.g., `root.channels.userEvents`)
 * @param node the underlying document value
 * @param address collision-safe identity within the source document
 * @param context shared parser context for registration and diagnostics
 * @param profile AsyncAPI version profile for member policy enforcement, or null for default
 */
internal class ParserNode(
    val name: String,
    val node: DocumentNode,
    val address: NodeAddress,
    val context: AsyncApiContext,
    val profile: AsyncApiParserProfile? = null,
) {

    val path: String
        get() = address.displayPath

    internal val sourceLocation
        get() = node.location.copy(path = address.documentPath)

    internal fun member(
        name: String,
        node: DocumentNode,
    ): ParserNode = ParserNode(name, node, address.member(name), context, profile)

    internal fun index(
        index: Int,
        node: DocumentNode,
    ): ParserNode = ParserNode("$name[$index]", node, address.index(index), context, profile)

    fun withProfile(profile: AsyncApiParserProfile): ParserNode =
        ParserNode(name, node, address, context, profile)

    fun expectObject(): ParserObjectNode =
        ParserObjectNode(
            parserNode = this,
            documentObject = node as? DocumentObject
                ?: ParserValueExpectation.unexpectedType(
                    node = node,
                    expectedType = typeOf<Map<String, Any?>>(),
                    address = address,
                    context = context,
                ),
        )

    fun expectArray(): ParserArrayNode =
        ParserArrayNode(
            parserNode = this,
            documentArray = node as? DocumentArray
                ?: ParserValueExpectation.unexpectedType(
                    node = node,
                    expectedType = typeOf<List<Any?>>(),
                    address = address,
                    context = context,
                ),
        )

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> expect(): T =
        ParserValueExpectation.expect(
            node = node,
            expectedType = typeOf<T>(),
            address = address,
            context = context,
        ) as T

    fun toPlainValue(): Any? = node.toValue()
}
