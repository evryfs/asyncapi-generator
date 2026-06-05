package dev.banking.asyncapi.generator.core.parser

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.ParserFixtures
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertFailsWith

/**
 * Shared support for parser-stage tests.
 *
 * It exposes reader-backed parser roots so parser tests can focus on
 * `ParserNode` input and model output instead of file-format handling.
 */
abstract class ParserTestSupport {

    protected val asyncApiContext = AsyncApiContext()
    private val parserFixtures = ParserFixtures(asyncApiContext)

    protected fun readRoot(path: String): ParserNode {
        return parserFixtures.root(path)
    }

    protected fun readNode(
        path: String,
        vararg nodePath: String,
    ): ParserNode {
        return nodePath.fold(readRoot(path)) { node, key ->
            node.mandatory(key)
        }
    }

    protected fun parseDocument(path: String): AsyncApiDocument {
        return parserFixtures.document(path)
    }

    protected inline fun <reified T : AsyncApiParseException> assertParseFailure(
        vararg expectedMessageParts: String,
        noinline block: () -> Unit,
    ): T {
        val error = assertFailsWith<T>(block = block)
        expectedMessageParts.forEach { expected ->
            assertThat(error.message).contains(expected)
        }
        return error
    }
}
