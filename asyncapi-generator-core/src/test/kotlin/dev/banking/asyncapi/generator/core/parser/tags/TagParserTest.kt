package dev.banking.asyncapi.generator.core.parser.tags

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class TagParserTest : ParserTestSupport() {

    private val parser = TagParser(asyncApiContext)

    @Test
    fun `parse valid tags`() {
        val tagsNode = readNode(
            "parser/tags/asyncapi_parser_tag_valid.yaml",
            "components",
            "tags",
        )
        val result = parser.parseMap(tagsNode)

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
        val tagsNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tags",
        )
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory 'name'",
            "asyncapi_parser_tag_invalid.yaml",
            "asyncapi_parser_tag_invalid.root.components.tags.MissingName.name",
        ) {
            parser.parseMap(tagsNode)
        }
    }
}
