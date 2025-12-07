package dev.banking.asyncapi.generator.core.validator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import java.io.File

abstract class AbstractValidatorTest {

    protected val asyncApiContext = AsyncApiContext()
    protected val parser = AsyncApiParser(asyncApiContext)

    /**
     * Helper to read and parse any YAML file relative to project root.
     * Returns the parsed AsyncApiDocument.
     */
    protected fun parse(path: String): AsyncApiDocument {
        val rootNode = AsyncApiRegistry.readYaml(File(path), asyncApiContext)
        return parser.parse(rootNode)
    }
}
