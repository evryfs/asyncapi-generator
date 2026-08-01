package dev.banking.asyncapi.generator.core.parser.externaldocs

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
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
    fun `parse external docs reports missing url`() {
        val externalDocsNode = readNode(
            "parser/externaldocs/asyncapi_parser_externaldocs_invalid.yaml",
            "components",
            "externalDocs",
            "MissingUrl",
        )
        assertMissingRequiredMember(
            memberName = "url",
            path = "asyncapi_parser_externaldocs_invalid.root.components.externalDocs.MissingUrl.url",
            sourcePath = "root.components.externalDocs.MissingUrl",
            sourceFile = "asyncapi_parser_externaldocs_invalid.yaml",
        ) {
            parser.parseElement(externalDocsNode)
        }
    }

    @Test
    fun `parse external docs reports explicit null ref before inline parsing`() {
        val externalDocsNode = readNode(
            "parser/externaldocs/asyncapi_parser_externaldocs_invalid.yaml",
            "components",
            "externalDocs",
            "NullReference",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_externaldocs_invalid.root.components.externalDocs.NullReference.\$ref",
            sourcePath = "root.components.externalDocs.NullReference.\$ref",
            sourceFile = "asyncapi_parser_externaldocs_invalid.yaml",
        ) {
            parser.parseElement(externalDocsNode)
        }
    }
}
