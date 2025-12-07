package dev.banking.asyncapi.generator.core.parser.info

import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.info.Contact
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.info.License

fun simpleInfo() = Info(
    title = "Simple Info Test",
    version = "1.2.3",
    description = "\"A simple description",
    termsOfService = "https://example.com/terms",
    contact = Contact(
        name = "Support",
        url = "https://support.example.com",
        email = "support@example.com"
    ),
    license = License(
        name = "Apache 2.0",
        url = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    tags = listOf(
        TagInterface.TagInline(
            Tag(
                name = "general",
                description = "General tag"
            )
        )
    ),
    externalDocs = ExternalDocInterface.ExternalDocInline(
        ExternalDoc(
            url = "'https://example.com/docs",
            description = "Documentation"
        )
    ),
    extensions = mapOf(
        "x-custom-extension" to "value"
    )
)
