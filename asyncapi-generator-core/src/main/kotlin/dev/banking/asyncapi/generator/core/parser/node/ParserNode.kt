package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
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
internal class ParserNode internal constructor(
    val name: String,
    val node: DocumentNode,
    @PublishedApi
    internal val address: NodeAddress,
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

    /** Requires this value to be an object and exposes object navigation. */
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

    /** Requires this value to be an array and exposes array navigation. */
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

    inline fun <reified T> expect(): T =
        ParserValueExpectation.cast(
            ParserValueExpectation.expect(
                node = node,
                expectedType = typeOf<T>(),
                address = address,
                context = context,
            ),
        )

    /** Converts this source-located node to plain maps, lists, scalars, or null. */
    fun toPlainValue(): Any? = node.toValue()
}
