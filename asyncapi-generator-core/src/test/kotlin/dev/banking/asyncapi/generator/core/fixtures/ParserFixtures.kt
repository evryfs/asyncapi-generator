package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import java.io.File

/**
 * Fixture facade for parser-stage tests.
 *
 * It loads parser roots and parsed AsyncAPI documents from shared test
 * resources using the supplied [AsyncApiContext].
 */
internal class ParserFixtures(
    val context: AsyncApiContext = AsyncApiContext(),
) {
    private val parser = AsyncApiParser(context)

    fun root(path: String): ParserNode = root(TestResources.file(path))

    fun root(file: File): ParserNode = AsyncApiRegistry.read(file, context)

    fun document(path: String): AsyncApiDocument = document(TestResources.file(path))

    fun document(file: File): AsyncApiDocument = parser.parse(root(file))
}
