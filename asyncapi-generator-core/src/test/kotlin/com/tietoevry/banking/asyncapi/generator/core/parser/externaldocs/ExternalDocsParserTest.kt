package com.tietoevry.banking.asyncapi.generator.core.parser.externaldocs

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExternalDocsParserTest : AbstractParserTest() {

    private val parser = ExternalDocsParser(asyncApiContext)

    @Test
    fun `parse valid external docs`() {
        val root = readYaml("src/test/resources/parser/externaldocs/asyncapi_parser_externaldocs_valid.yaml")
        val result = parser.parseMap(root.mandatory("components").mandatory("externalDocs"))

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
        val root = readYaml("src/test/resources/parser/externaldocs/asyncapi_parser_externaldocs_invalid.yaml")
        val externalDocsNode = root.mandatory("components").mandatory("externalDocs").mandatory("MissingUrl")
        assertFailsWith<AsyncApiParseException.Mandatory> {
            parser.parseElement(externalDocsNode)
        }
    }
}
