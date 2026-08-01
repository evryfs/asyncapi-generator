package dev.banking.asyncapi.generator.core.parser.info

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun `parse info reports missing title`() {
        val infoNode = readNode("parser/info/asyncapi_parser_info_invalid.yaml", "info")

        assertMissingRequiredMember(
            memberName = "title",
            path = "asyncapi_parser_info_invalid.root.info.title",
            sourcePath = "root.info",
            sourceFile = "asyncapi_parser_info_invalid.yaml",
        ) {
            parser.parseMap(infoNode)
        }
    }

    @Test
    fun `parse info reports missing version`() {
        val infoNode = readNode(
            "parser/info/asyncapi_parser_info_invalid.yaml",
            "infoMissingVersion",
        )

        assertMissingRequiredMember(
            memberName = "version",
            path = "asyncapi_parser_info_invalid.root.infoMissingVersion.version",
            sourcePath = "root.infoMissingVersion",
            sourceFile = "asyncapi_parser_info_invalid.yaml",
        ) {
            parser.parseMap(infoNode)
        }
    }

    @Test
    fun `parse info reports explicit null title`() {
        val infoNode = readNode(
            "parser/info/asyncapi_parser_info_invalid.yaml",
            "infoNullTitle",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_info_invalid.root.infoNullTitle.title",
            sourcePath = "root.infoNullTitle.title",
            sourceFile = "asyncapi_parser_info_invalid.yaml",
        ) {
            parser.parseMap(infoNode)
        }
    }

    @Test
    fun `parse info reports explicit null description`() {
        val infoNode = readNode(
            "parser/info/asyncapi_parser_info_invalid.yaml",
            "infoNullDescription",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_info_invalid.root.infoNullDescription.description",
            sourcePath = "root.infoNullDescription.description",
            sourceFile = "asyncapi_parser_info_invalid.yaml",
        ) {
            parser.parseMap(infoNode)
        }
    }

    @Test
    fun `parse info reports missing license name`() {
        val infoNode = readNode(
            "parser/info/asyncapi_parser_info_invalid.yaml",
            "infoMissingLicenseName",
        )

        assertMissingRequiredMember(
            memberName = "name",
            path = "asyncapi_parser_info_invalid.root.infoMissingLicenseName.license.name",
            sourcePath = "root.infoMissingLicenseName.license",
            sourceFile = "asyncapi_parser_info_invalid.yaml",
        ) {
            parser.parseMap(infoNode)
        }
    }

    @Test
    fun `parse info preserves nested and null extensions in source order`() {
        val infoNode = readNode(
            "parser/info/asyncapi_parser_info_valid.yaml",
            "infoWithExtensions",
        )

        val extensions = parser.parseMap(infoNode).extensions

        assertEquals(
            linkedMapOf(
                "x-object" to mapOf(
                    "items" to listOf(
                        "alpha",
                        mapOf("enabled" to true, "limit" to 2),
                    )
                ),
                "x-null" to null,
                "x-array" to listOf(1, "two", false),
            ),
            extensions,
        )
        assertEquals(listOf("x-object", "x-null", "x-array"), extensions?.keys?.toList())
    }

    @Test
    fun `parse info without extensions returns null extensions`() {
        val infoNode = readNode(
            "parser/info/asyncapi_parser_info_valid.yaml",
            "infoWithoutExtensions",
        )

        assertNull(parser.parseMap(infoNode).extensions)
    }
}
