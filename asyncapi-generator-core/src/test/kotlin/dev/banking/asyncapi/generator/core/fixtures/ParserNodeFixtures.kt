package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.reader.DocumentBoolean
import dev.banking.asyncapi.generator.core.reader.DocumentArray
import dev.banking.asyncapi.generator.core.reader.DocumentMember
import dev.banking.asyncapi.generator.core.reader.DocumentNode
import dev.banking.asyncapi.generator.core.reader.DocumentNull
import dev.banking.asyncapi.generator.core.reader.DocumentNumber
import dev.banking.asyncapi.generator.core.reader.DocumentObject
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
        require(value !is List<*> && value !is Map<*, *>) {
            "Use ParserNodeFixtures.value for collection values"
        }
        return parserNode(value, sourceLine, name, path, fileName)
    }

    fun value(
        value: Any?,
        sourceLine: String,
        name: String = "value",
        path: String = "test.root.value",
        fileName: String = "asyncapi.yaml",
    ): ParserNode = parserNode(value, sourceLine, name, path, fileName)

    private fun parserNode(
        value: Any?,
        sourceLine: String,
        name: String,
        path: String,
        fileName: String,
    ): ParserNode {
        val context = AsyncApiContext()
        val file = File(fileName)
        context.registerSource(file, sourceLine)
        context.registerLine(path, 1)
        return ParserNode(
            name = name,
            node = documentNode(value, file, path),
            path = path,
            context = context,
        )
    }

    private fun documentNode(
        value: Any?,
        file: File,
        path: String,
    ): DocumentNode {
        val location = sourceLocation(file, path)
        return when (value) {
            null -> DocumentNull(location)
            is String -> DocumentString(value, location)
            is Number -> DocumentNumber(value, location)
            is Boolean -> DocumentBoolean(value, location)
            is List<*> -> DocumentArray(
                elements = value.mapIndexed { index, element ->
                    documentNode(element, file, "$path[$index]")
                },
                location = location,
            )
            is Map<*, *> -> DocumentObject(
                members = value.entries.associateTo(linkedMapOf()) { (key, memberValue) ->
                    require(key is String) { "Parser fixture object keys must be strings" }
                    val memberPath = "$path.$key"
                    key to DocumentMember(
                        keyLocation = sourceLocation(file, memberPath),
                        value = documentNode(memberValue, file, memberPath),
                    )
                },
                location = location,
            )
            else -> error("Unsupported scalar fixture value: ${value::class.simpleName}")
        }
    }

    private fun sourceLocation(
        file: File,
        path: String,
    ): SourceLocation =
        SourceLocation(
            sourceId = file.nameWithoutExtension,
            file = file,
            path = path,
            line = 1,
            column = 1,
        )
}
