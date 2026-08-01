package dev.banking.asyncapi.generator.core.parser.info

import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

/**
 * Parses the AsyncAPI info object from parser nodes.
 *
 * Expected behavior is covered by:
 * - `InfoParserTest`
 */
class InfoParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val tagParser = TagParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Info {
        return Info(
            title = parserNode.required("title").expect<String>(),
            version = parserNode.required("version").expect<String>(),
            description = parserNode.optional("description")?.expect<String>(),
            termsOfService = parserNode.optional("termsOfService")?.expect<String>(),
            contact = parserNode.optional("contact")?.let(::parseContact),
            license = parserNode.optional("license")?.let(::parseLicense),
            tags = parserNode.optional("tags")?.let(tagParser::parseList),
            externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
            extensions = parseExtensions(parserNode),
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseContact(parserNode: ParserNode): Contact {
        return Contact(
            name = parserNode.optional("name")?.expect<String>(),
            url = parserNode.optional("url")?.expect<String>(),
            email = parserNode.optional("email")?.expect<String>()
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseLicense(parserNode: ParserNode): License {
        return License(
            name = parserNode.required("name").expect<String>(),
            url = parserNode.optional("url")?.expect<String>()
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseExtensions(parserNode: ParserNode): Map<String, Any?>? {
        val extensions = parserNode.members()
            .filter { member -> member.name.startsWith("x-") }
            .associateTo(linkedMapOf()) { member ->
                member.name to member.toPlainValue()
            }
        return extensions.takeIf { it.isNotEmpty() }
    }
}
