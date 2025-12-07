package com.tietoevry.banking.asyncapi.generator.core.parser.info

import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class InfoParserTest : AbstractParserTest() {

    private val parser = InfoParser(asyncApiContext)

    @Test
    fun `parse valid info object`() {
        val root = readYaml("src/test/resources/parser/info/asyncapi_parser_info_valid.yaml")
        val result = parser.parseMap(root.mandatory("info"))
        val expected = simpleInfo()
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expected)
    }

    @Test
    fun `parse Info missing mandatory fields throws RequiredObject`() {
        val root = readYaml("src/test/resources/parser/info/asyncapi_parser_info_invalid.yaml")
        val infoNode = root.mandatory("info")
        assertFailsWith<AsyncApiParseException.Mandatory> {
            parser.parseMap(infoNode)
        }
    }
}
