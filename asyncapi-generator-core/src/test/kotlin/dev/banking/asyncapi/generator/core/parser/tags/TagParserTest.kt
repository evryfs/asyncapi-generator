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
        val result = parser.parseMap(
            readNode(
                "parser/tags/asyncapi_parser_tag_valid.yaml",
                "components",
                "tags",
            )
        )

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
        assertParseFailure<AsyncApiParseException.Mandatory> {
            parser.parseMap(
                readNode(
                    "parser/tags/asyncapi_parser_tag_invalid.yaml",
                    "components",
                    "tags",
                )
            )
        }
    }
}
