package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.reader.DocumentArray
import dev.banking.asyncapi.generator.core.reader.DocumentBoolean
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
    ): ParserNode = node(value, sourceLine, name, path, fileName)

    fun node(
        value: Any?,
        sourceLine: String,
        name: String = "root",
        path: String = "test.root",
        fileName: String = "asyncapi.yaml",
    ): ParserNode {
        val context = AsyncApiContext()
        val file = File(fileName)
        context.registerSource(file, sourceLine)
        return ParserNode(
            name = name,
            node = documentNode(value, file, path, context),
            path = path,
            context = context,
        )
    }

    private fun documentNode(
        value: Any?,
        file: File,
        path: String,
        context: AsyncApiContext,
    ): DocumentNode {
        val location = SourceLocation(
            sourceId = file.nameWithoutExtension,
            file = file,
            path = path,
            line = 1,
            column = 1,
        )
        context.registerSourceLocation(path, location)
        return when (value) {
            null -> DocumentNull(location)
            is String -> DocumentString(value, location)
            is Number -> DocumentNumber(value, location)
            is Boolean -> DocumentBoolean(value, location)
            is Map<*, *> -> DocumentObject(
                members = value.entries.associate { (key, memberValue) ->
                    require(key is String) { "Document object fixture keys must be strings" }
                    val memberPath = "$path.$key"
                    key to DocumentMember(
                        keyLocation = location.copy(path = memberPath),
                        value = documentNode(memberValue, file, memberPath, context),
                    )
                },
                location = location,
            )
            is List<*> -> DocumentArray(
                elements = value.mapIndexed { index, element ->
                    documentNode(element, file, "$path[$index]", context)
                },
                location = location,
            )
            else -> error("Unsupported fixture value: ${value::class.simpleName}")
        }
    }
}
