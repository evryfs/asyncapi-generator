package dev.banking.asyncapi.generator.core.parser.node

import dev.banking.asyncapi.generator.core.constants.AsyncApiConstants.ROOT
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentNode
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.InputDocument
import dev.banking.asyncapi.generator.core.document.SourceLocation

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
        val sourceId = context.registerDocumentSource(document.source.file, document.source.content)

        val rootPath = "$sourceId.$ROOT"
        registerLocations(
            node = document.root,
            parserPath = rootPath,
            location = document.root.location,
            context = context,
        )

        return ParserNode(
            name = rootPath,
            node = document.root,
            path = rootPath,
            context = context,
        )
    }

    private fun registerLocations(
        node: DocumentNode,
        parserPath: String,
        location: SourceLocation,
        context: AsyncApiContext,
    ) {
        registerLocation(parserPath, location, context)

        when (node) {
            is DocumentObject -> node.members.forEach { (name, member) ->
                registerLocations(
                    node = member.value,
                    parserPath = "$parserPath.$name",
                    location = member.keyLocation,
                    context = context,
                )
            }

            is DocumentArray -> node.elements.forEachIndexed { index, element ->
                registerLocations(
                    node = element,
                    parserPath = "$parserPath[$index]",
                    location = element.location,
                    context = context,
                )
            }

            else -> Unit
        }
    }

    private fun registerLocation(
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

    private fun normalizeArrayPath(path: String): String =
        path.replace(Regex("""\[(\d+)]"""), ".$1")

}
