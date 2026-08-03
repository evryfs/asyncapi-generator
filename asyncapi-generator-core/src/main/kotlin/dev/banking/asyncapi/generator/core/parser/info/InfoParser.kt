package dev.banking.asyncapi.generator.core.parser.info

import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.CONTACT
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.INFO
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.LICENSE

/**
 * Parses the AsyncAPI info object from parser nodes.
 *
 * Expected behavior is covered by:
 * - `InfoParserTest`
 */
internal class InfoParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val tagParser = TagParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Info {
        val objectNode = parserNode.expectObject()
        objectNode.expectOnlyMembers(INFO)
        return Info(
            title = objectNode.required("title").expect<String>(),
            version = objectNode.required("version").expect<String>(),
            description = objectNode.optional("description")?.expect<String>(),
            termsOfService = objectNode.optional("termsOfService")?.expect<String>(),
            contact = objectNode.optional("contact")?.let(::parseContact),
            license = objectNode.optional("license")?.let(::parseLicense),
            tags = objectNode.optional("tags")?.let(tagParser::parseList),
            externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
            extensions = parseExtensions(parserNode),
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseContact(parserNode: ParserNode): Contact {
        val objectNode = parserNode.expectObject()
        objectNode.expectOnlyMembers(CONTACT)
        return Contact(
            name = objectNode.optional("name")?.expect<String>(),
            url = objectNode.optional("url")?.expect<String>(),
            email = objectNode.optional("email")?.expect<String>()
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseLicense(parserNode: ParserNode): License {
        val objectNode = parserNode.expectObject()
        objectNode.expectOnlyMembers(LICENSE)
        return License(
            name = objectNode.required("name").expect<String>(),
            url = objectNode.optional("url")?.expect<String>()
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseExtensions(parserNode: ParserNode): Map<String, Any?>? {
        val extensions = parserNode.expectObject()
            .membersStartingWith("x-")
            .associateTo(linkedMapOf()) { member ->
                member.name to member.toPlainValue()
            }
        return extensions.takeIf { it.isNotEmpty() }
    }
}
