package com.tietoevry.banking.asyncapi.generator.core.validator

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.parser.AsyncApiParser
import com.tietoevry.banking.asyncapi.generator.core.registry.AsyncApiRegistry
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
