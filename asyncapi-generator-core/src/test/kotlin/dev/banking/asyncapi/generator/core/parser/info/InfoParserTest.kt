package dev.banking.asyncapi.generator.core.parser.info

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InfoParserTest : ParserTestSupport() {

    private val parser = InfoParser(asyncApiContext)

    @Test
    fun `parse valid info object`() {
        val infoNode = readNode("parser/info/asyncapi_parser_info_valid.yaml", "info")
        val result = parser.parseMap(infoNode)
        val expected = simpleInfo()
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expected)
    }

    @Test
    fun `parse Info missing mandatory fields throws RequiredObject`() {
        val infoNode = readNode("parser/info/asyncapi_parser_info_invalid.yaml", "info")
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory 'title'",
            "asyncapi_parser_info_invalid.yaml",
            "asyncapi_parser_info_invalid.root.info.title",
        ) {
            parser.parseMap(infoNode)
        }
    }
}
