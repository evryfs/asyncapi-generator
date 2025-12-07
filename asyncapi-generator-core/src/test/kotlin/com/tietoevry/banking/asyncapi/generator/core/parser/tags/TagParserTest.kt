package com.tietoevry.banking.asyncapi.generator.core.parser.tags

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TagParserTest : AbstractParserTest() {

    private val parser = TagParser(asyncApiContext)

    @Test
    fun `parse valid tags`() {
        val root = readYaml("src/test/resources/parser/tags/asyncapi_parser_tag_valid.yaml")
        val result = parser.parseMap(root.mandatory("components").mandatory("tags"))

        assertTrue("inlineTag" in result)
        val inlineTag = (result["inlineTag"] as TagInterface.TagInline).tag
        assertThat(inlineTag)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(inlineTag())

        assertTrue("refTag" in result)
        val refTag = (result["refTag"] as TagInterface.TagReference).reference
        assertThat(refTag)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(refTag())
    }

    @Test
    fun `parse tag missing name throws RequiredField`() { // or RequiredObject depending on parser logic
        val root = readYaml("src/test/resources/parser/tags/asyncapi_parser_tag_invalid.yaml")
        assertFailsWith<AsyncApiParseException.Mandatory> {
            parser.parseMap(root.mandatory("components").mandatory("tags"))
        }
    }
}
