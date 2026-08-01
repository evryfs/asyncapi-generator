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
    fun `parse info missing title reports missing required member`() {
        val infoNode = readNode("parser/info/asyncapi_parser_info_invalid.yaml", "info")
        assertParseFailure<AsyncApiParseException.ParserDiagnosticFailure>(
            "Missing required member 'title'",
            "asyncapi_parser_info_invalid.yaml",
            "asyncapi_parser_info_invalid.root.info.title",
        ) {
            parser.parseMap(infoNode)
        }
    }
}
