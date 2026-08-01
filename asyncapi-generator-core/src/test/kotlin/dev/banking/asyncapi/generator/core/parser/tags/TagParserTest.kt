package dev.banking.asyncapi.generator.core.parser.tags

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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

        assertEquals(listOf("inlineTag", "refTag"), result.keys.toList())
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
    fun `parse valid tag list in source order`() {
        val tagsNode = readNode(
            "parser/tags/asyncapi_parser_tag_valid.yaml",
            "components",
            "tagLists",
            "orderedTags",
        )

        val result = parser.parseList(tagsNode)

        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(
                listOf(
                    TagInterface.TagInline(inlineTag()),
                    TagInterface.TagReference(refTag()),
                )
            )
    }

    @Test
    fun `parse tag map reports array container`() {
        val tagsNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tagContainers",
            "ArrayInsteadOfMap",
        )

        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf("not-a-map"),
            path = "asyncapi_parser_tag_invalid.root.components.tagContainers.ArrayInsteadOfMap",
            sourcePath = "root.components.tagContainers.ArrayInsteadOfMap",
            sourceFile = "asyncapi_parser_tag_invalid.yaml",
        ) {
            parser.parseMap(tagsNode)
        }
    }

    @Test
    fun `parse tag list reports object container`() {
        val tagsNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tagContainers",
            "ObjectInsteadOfList",
        )

        assertUnexpectedValueType(
            expectedType = "List<Any?>",
            actualType = ParserValueType.OBJECT,
            actualValue = mapOf("unexpected" to "not-an-array"),
            path = "asyncapi_parser_tag_invalid.root.components.tagContainers.ObjectInsteadOfList",
            sourcePath = "root.components.tagContainers.ObjectInsteadOfList",
            sourceFile = "asyncapi_parser_tag_invalid.yaml",
        ) {
            parser.parseList(tagsNode)
        }
    }

    @Test
    fun `parse tag reports missing name`() {
        val tagNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tags",
            "MissingName",
        )

        assertMissingRequiredMember(
            memberName = "name",
            path = "asyncapi_parser_tag_invalid.root.components.tags.MissingName.name",
            sourcePath = "root.components.tags.MissingName",
            sourceFile = "asyncapi_parser_tag_invalid.yaml",
        ) {
            parser.parseElement(tagNode)
        }
    }

    @Test
    fun `parse tag reports explicit null name`() {
        val tagNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tags",
            "NullName",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_tag_invalid.root.components.tags.NullName.name",
            sourcePath = "root.components.tags.NullName.name",
            sourceFile = "asyncapi_parser_tag_invalid.yaml",
        ) {
            parser.parseElement(tagNode)
        }
    }

    @Test
    fun `parse tag reports explicit null description`() {
        val tagNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tags",
            "NullDescription",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_tag_invalid.root.components.tags.NullDescription.description",
            sourcePath = "root.components.tags.NullDescription.description",
            sourceFile = "asyncapi_parser_tag_invalid.yaml",
        ) {
            parser.parseElement(tagNode)
        }
    }

    @Test
    fun `parse tag reports explicit null ref before inline parsing`() {
        val tagNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tags",
            "NullReference",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_tag_invalid.root.components.tags.NullReference.\$ref",
            sourcePath = "root.components.tags.NullReference.\$ref",
            sourceFile = "asyncapi_parser_tag_invalid.yaml",
        ) {
            parser.parseElement(tagNode)
        }
    }

    @Test
    fun `parse tag reports non-string ref before inline parsing`() {
        val tagNode = readNode(
            "parser/tags/asyncapi_parser_tag_invalid.yaml",
            "components",
            "tags",
            "NumericReference",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 42,
            path = "asyncapi_parser_tag_invalid.root.components.tags.NumericReference.\$ref",
            sourcePath = "root.components.tags.NumericReference.\$ref",
            sourceFile = "asyncapi_parser_tag_invalid.yaml",
        ) {
            parser.parseElement(tagNode)
        }
    }
}
