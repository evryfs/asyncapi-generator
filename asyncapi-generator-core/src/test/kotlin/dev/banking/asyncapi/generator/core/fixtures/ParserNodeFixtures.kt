package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.reader.DocumentBoolean
import dev.banking.asyncapi.generator.core.reader.DocumentNode
import dev.banking.asyncapi.generator.core.reader.DocumentNull
import dev.banking.asyncapi.generator.core.reader.DocumentNumber
import dev.banking.asyncapi.generator.core.reader.DocumentString
import dev.banking.asyncapi.generator.core.reader.SourceLocation
import java.io.File

/**
 * Fixture helpers for tests that exercise [ParserNode] directly.
 *
 * These helpers create minimal parser nodes with source context attached, so
 * tests can verify parser-node behavior and error messages without loading a
 * complete AsyncAPI document.
 */
internal object ParserNodeFixtures {

    fun scalar(
        value: Any?,
        sourceLine: String,
        name: String = "value",
        path: String = "test.root.value",
        fileName: String = "asyncapi.yaml",
    ): ParserNode {
        val context = AsyncApiContext()
        val file = File(fileName)
        context.registerSource(file, sourceLine)
        context.registerLine(path, 1)
        return ParserNode(
            name = name,
            node = scalarNode(value, file, path),
            path = path,
            context = context,
        )
    }

    private fun scalarNode(
        value: Any?,
        file: File,
        path: String,
    ): DocumentNode {
        val location = SourceLocation(
            sourceId = file.nameWithoutExtension,
            file = file,
            path = path,
            line = 1,
            column = 1,
        )
        return when (value) {
            null -> DocumentNull(location)
            is String -> DocumentString(value, location)
            is Number -> DocumentNumber(value, location)
            is Boolean -> DocumentBoolean(value, location)
            else -> error("Unsupported scalar fixture value: ${value::class.simpleName}")
        }
    }
}
