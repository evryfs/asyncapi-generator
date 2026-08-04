package dev.banking.asyncapi.generator.core.parser.tags

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
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

}
