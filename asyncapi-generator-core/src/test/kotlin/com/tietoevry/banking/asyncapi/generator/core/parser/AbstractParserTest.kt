package dev.banking.asyncapi.generator.core.parser

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import java.io.File

abstract class AbstractParserTest {

    protected val asyncApiContext = AsyncApiContext()

    protected fun readYaml(path: String): ParserNode {
        return AsyncApiRegistry.readYaml(File(path), asyncApiContext)
    }
}
