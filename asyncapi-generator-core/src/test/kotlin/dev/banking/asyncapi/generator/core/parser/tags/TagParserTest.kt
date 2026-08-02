package dev.banking.asyncapi.generator.core.parser.tags

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class TagParserTest {

    private val context = AsyncApiContext()
    private val parser = TagParser(context)

    @Test
    fun `parse tag map and list in source order`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val components = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject()

        val tags = parser.parseMap(components.required("tags"))

        assertEquals(listOf("inlineTag", "refTag"), tags.keys.toList())
        val inlineTag = assertIs<TagInterface.TagInline>(tags["inlineTag"]).tag
        assertEquals("user", inlineTag.name)
        assertEquals("User related operations", inlineTag.description)
        val externalDocs = assertIs<ExternalDocInterface.ExternalDocInline>(inlineTag.externalDocs).externalDoc
        assertEquals("https://example.com/docs/user", externalDocs.url)
        assertEquals("User docs", externalDocs.description)
        val tagReference = assertIs<TagInterface.TagReference>(tags["refTag"]).reference
        assertEquals("#/components/tags/inlineTag", tagReference.ref)
        assertEquals(TAG, tagReference.referenceCategoryKey)

        val orderedTags = components.required("tagLists")
            .expectObject().required("orderedTags")
        val tagList = parser.parseList(orderedTags)
        val listInlineTag = assertIs<TagInterface.TagInline>(tagList[0]).tag
        assertEquals("user", listInlineTag.name)
        assertEquals("User related operations", listInlineTag.description)
        val listReference = assertIs<TagInterface.TagReference>(tagList[1]).reference
        assertEquals("#/components/tags/inlineTag", listReference.ref)
        assertEquals(TAG, listReference.referenceCategoryKey)
    }

    @Test
    fun `parse tag map reports array container`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val tagsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("tagContainers")
            .expectObject().required("ArrayInsteadOfMap")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(tagsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, diagnostic.actualType)
        assertEquals(listOf("not-a-map"), diagnostic.actualValue)
        assertEquals("asyncapi_parser_tag_invalid.root.components.tagContainers.ArrayInsteadOfMap", diagnostic.path)
        assertEquals("root.components.tagContainers.ArrayInsteadOfMap", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_tag_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse tag list reports object container`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val tagsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("tagContainers")
            .expectObject().required("ObjectInsteadOfList")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(tagsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("List<Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.OBJECT, diagnostic.actualType)
        assertEquals(mapOf("unexpected" to "not-an-array"), diagnostic.actualValue)
        assertEquals("asyncapi_parser_tag_invalid.root.components.tagContainers.ObjectInsteadOfList", diagnostic.path)
        assertEquals("root.components.tagContainers.ObjectInsteadOfList", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_tag_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse tag reports missing name`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val tagNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("tags")
            .expectObject().required("MissingName")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(tagNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.MissingRequiredMember>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.MISSING_REQUIRED_MEMBER, diagnostic.category)
        assertEquals("name", diagnostic.memberName)
        assertEquals("present member", diagnostic.expectedType)
        assertEquals("asyncapi_parser_tag_invalid.root.components.tags.MissingName.name", diagnostic.path)
        assertEquals("root.components.tags.MissingName", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_tag_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse tag reports explicit null name`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val tagNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("tags")
            .expectObject().required("NullName")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(tagNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("asyncapi_parser_tag_invalid.root.components.tags.NullName.name", diagnostic.path)
        assertEquals("root.components.tags.NullName.name", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_tag_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse tag reports explicit null description`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val tagNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("tags")
            .expectObject().required("NullDescription")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(tagNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("asyncapi_parser_tag_invalid.root.components.tags.NullDescription.description", diagnostic.path)
        assertEquals("root.components.tags.NullDescription.description", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_tag_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse tag reports explicit null ref before inline parsing`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val tagNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("tags")
            .expectObject().required("NullReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(tagNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("asyncapi_parser_tag_invalid.root.components.tags.NullReference.\$ref", diagnostic.path)
        assertEquals("root.components.tags.NullReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_tag_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse tag reports non-string ref before inline parsing`() {
        val file = TestResources.file("parser/tags/asyncapi_parser_tag_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val tagNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("tags")
            .expectObject().required("NumericReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(tagNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(42, diagnostic.actualValue)
        assertEquals("asyncapi_parser_tag_invalid.root.components.tags.NumericReference.\$ref", diagnostic.path)
        assertEquals("root.components.tags.NumericReference.\$ref", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_tag_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
