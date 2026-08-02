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
data class ParserNode(
    val name: String,
    val node: DocumentNode,
    val path: String,
    val context: AsyncApiContext,
    val profile: AsyncApiParserProfile? = null,
) {

    internal fun child(
        name: String,
        node: DocumentNode,
        path: String,
    ): ParserNode = ParserNode(name, node, path, context, profile)

    fun withProfile(profile: AsyncApiParserProfile): ParserNode =
        copy(profile = profile)

    /** Requires this value to be an object and exposes object navigation. */
    fun expectObject(): ParserObjectNode =
        ParserObjectNode(
            parserNode = this,
            documentObject = node as? DocumentObject
                ?: ParserValueExpectation.unexpectedType(
                    node = node,
                    expectedType = typeOf<Map<String, Any?>>(),
                    path = path,
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
                    path = path,
                    context = context,
                ),
        )

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
}
