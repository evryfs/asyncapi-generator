package dev.banking.asyncapi.generator.core.parser.externaldocs

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ExternalDocsParserTest : ParserTestSupport() {

    private val parser = ExternalDocsParser(asyncApiContext)

    @Test
    fun `parse valid external docs`() {
        val externalDocsNode = readNode(
            "parser/externaldocs/asyncapi_parser_externaldocs_valid.yaml",
            "components",
            "externalDocs",
        )
        val result = parser.parseMap(externalDocsNode)

        assertTrue("MyExternalDocs" in result)
        val myExternalDocs = (result["MyExternalDocs"] as ExternalDocInterface.ExternalDocInline).externalDoc
        assertThat(myExternalDocs)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(myExternalDocs())

        assertTrue("RefExternalDocs" in result)
        val refExternalDocs = (result["RefExternalDocs"] as ExternalDocInterface.ExternalDocReference).reference
        assertThat(refExternalDocs)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(refExternalDocs())
    }

    @Test
    fun `parse external docs missing url throws RequiredObject`() {
        val externalDocsNode = readNode(
            "parser/externaldocs/asyncapi_parser_externaldocs_invalid.yaml",
            "components",
            "externalDocs",
            "MissingUrl",
        )
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory 'url'",
            "asyncapi_parser_externaldocs_invalid.yaml",
            "asyncapi_parser_externaldocs_invalid.root.components.externalDocs.MissingUrl.url",
        ) {
            parser.parseElement(externalDocsNode)
        }
    }
}
