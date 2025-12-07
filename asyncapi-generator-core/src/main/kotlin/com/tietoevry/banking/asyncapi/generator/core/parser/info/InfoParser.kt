package com.tietoevry.banking.asyncapi.generator.core.parser.info

import com.tietoevry.banking.asyncapi.generator.core.model.info.Contact
import com.tietoevry.banking.asyncapi.generator.core.model.info.Info
import com.tietoevry.banking.asyncapi.generator.core.model.info.License
import com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import com.tietoevry.banking.asyncapi.generator.core.parser.tags.TagParser
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

class InfoParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val tagParser = TagParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Info {
        parserNode.coerce<Map<*, *>>()
        return Info(
            title = parserNode.mandatory("title").coerce<String>(),
            version = parserNode.mandatory("version").coerce<String>(),
            description = parserNode.optional("description")?.coerce<String>(),
            termsOfService = parserNode.optional("termsOfService")?.coerce<String>(),
            contact = parserNode.optional("contact")?.let(::parseContact),
            license = parserNode.optional("license")?.let(::parseLicense),
            tags = parserNode.optional("tags")?.let(tagParser::parseList),
            externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
            extensions = parserNode.startsWith("x-")?.coerce<Map<String, Any?>>(),
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseContact(parserNode: ParserNode): Contact {
        return Contact(
            name = parserNode.optional("name")?.coerce<String>(),
            url = parserNode.optional("url")?.coerce<String>(),
            email = parserNode.optional("email")?.coerce<String>()
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun parseLicense(parserNode: ParserNode): License {
        return License(
            name = parserNode.mandatory("name").coerce<String>(),
            url = parserNode.optional("url")?.coerce<String>()
        ).also { asyncApiContext.register(it, parserNode) }
    }
}
