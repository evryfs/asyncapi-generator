package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.constants.AsyncApiConstants.ROOT
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.reader.DocumentArray
import dev.banking.asyncapi.generator.core.reader.DocumentBoolean
import dev.banking.asyncapi.generator.core.reader.DocumentNode
import dev.banking.asyncapi.generator.core.reader.DocumentNull
import dev.banking.asyncapi.generator.core.reader.DocumentNumber
import dev.banking.asyncapi.generator.core.reader.DocumentObject
import dev.banking.asyncapi.generator.core.reader.DocumentString
import dev.banking.asyncapi.generator.core.reader.InputDocument
import dev.banking.asyncapi.generator.core.reader.SourceLocation
import java.io.File

/**
 * Creates parser nodes from reader-stage input documents.
 *
 * This is the adapter between the reader stage and parser stage. It preserves
 * the existing parser path convention while registering reader-provided source
 * locations in the parser context.
 *
 * Expected behavior is covered by:
 * - `ParserNodeFactoryTest`
 */
object ParserNodeFactory {

    fun root(
        document: InputDocument,
        context: AsyncApiContext,
    ): ParserNode {
        context.registerSource(document.source.file, document.source.content)

        val rootPath = "${buildFileId(document.source.file)}.$ROOT"
        registerSourceLocations(document.root, rootPath, context)

        return ParserNode(
            name = rootPath,
            node = document.root.toParserValue(),
            path = rootPath,
            context = context,
        )
    }

    private fun registerSourceLocations(
        node: DocumentNode,
        path: String,
        context: AsyncApiContext,
        location: SourceLocation = node.location,
    ) {
        registerSourceLocation(path, location, context)

        when (node) {
            is DocumentObject -> node.members.forEach { (name, member) ->
                registerSourceLocations(
                    node = member.value,
                    path = "$path.$name",
                    context = context,
                    location = member.keyLocation,
                )
            }

            is DocumentArray -> node.elements.forEachIndexed { index, element ->
                registerSourceLocations(element, "$path[$index]", context)
            }

            else -> Unit
        }
    }

    private fun registerSourceLocation(
        path: String,
        location: SourceLocation,
        context: AsyncApiContext,
    ) {
        context.registerSourceLocation(path, location)

        val normalizedPath = normalizeArrayPath(path)
        if (normalizedPath != path) {
            context.registerSourceLocation(normalizedPath, location)
        }
    }

    private fun DocumentNode.toParserValue(): Any? =
        when (this) {
            is DocumentObject -> members.mapValuesTo(linkedMapOf()) { (_, member) ->
                member.value.toParserValue()
            }

            is DocumentArray -> elements.map { element -> element.toParserValue() }
            is DocumentString -> value
            is DocumentNumber -> value
            is DocumentBoolean -> value
            is DocumentNull -> null
        }

    private fun normalizeArrayPath(path: String): String =
        path.replace(Regex("""\[(\d+)]"""), ".$1")

    private fun buildFileId(file: File): String =
        file.nameWithoutExtension
            .replace(Regex("[^A-Za-z0-9_]"), "_")
}
