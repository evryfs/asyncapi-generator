package com.tietoevry.banking.asyncapi.generator.core.parser

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import java.io.File

abstract class AbstractParserTest {

    protected val asyncApiContext = AsyncApiContext()

    protected fun readYaml(path: String): ParserNode {
        return AsyncApiRegistry.readYaml(File(path), asyncApiContext)
    }
}
