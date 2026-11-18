package dev.banking.asyncapi.generator.core.parser.info

import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.info.License
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseObject
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.CONTACT
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.INFO
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.LICENSE

/**
 * Parses the AsyncAPI info object from parser nodes.
 */
internal class InfoParser {
    private val externalDocsParser = ExternalDocsParser()
    private val tagParser = TagParser()

    fun parse(parserNode: ParserNode): Info =
        parserNode.parseObject(INFO) { objectNode ->
            Info(
                title = objectNode.required("title").expect<String>(),
                version = objectNode.required("version").expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                termsOfService = objectNode.optional("termsOfService")?.expect<String>(),
                contact = objectNode.optional("contact")?.let(::parseContact),
                license = objectNode.optional("license")?.let(::parseLicense),
                tags = objectNode.optional("tags")?.parseList(tagParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                extensions = objectNode.extensions(),
            )
        }

    private fun parseContact(parserNode: ParserNode): Contact =
        parserNode.parseObject(CONTACT) { objectNode ->
            Contact(
                name = objectNode.optional("name")?.expect<String>(),
                url = objectNode.optional("url")?.expect<String>(),
                email = objectNode.optional("email")?.expect<String>(),
                extensions = objectNode.extensions(),
            )
        }

    private fun parseLicense(parserNode: ParserNode): License =
        parserNode.parseObject(LICENSE) { objectNode ->
            License(
                name = objectNode.required("name").expect<String>(),
                url = objectNode.optional("url")?.expect<String>(),
                extensions = objectNode.extensions(),
            )
        }
}
