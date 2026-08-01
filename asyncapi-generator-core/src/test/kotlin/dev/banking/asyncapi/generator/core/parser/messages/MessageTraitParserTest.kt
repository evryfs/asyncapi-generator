package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageTraitParserTest : ParserTestSupport() {

    private val parser = MessageTraitParser(asyncApiContext)

    @Test
    fun `parse message trait list`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_edge_cases.yaml",
            "components",
            "messages",
            "InlineTraitMessage",
            "traits",
        )

        val traits = parser.parseList(traitsNode)

        assertEquals(1, traits.size)
        assertTrue(traits.first() is MessageTraitInterface.InlineMessageTrait)
        val trait = (traits.first() as MessageTraitInterface.InlineMessageTrait).trait
        assertTrue(trait.headers is SchemaInterface.SchemaInline)
        assertEquals("string", (trait.headers as SchemaInterface.SchemaInline).schema.type)
    }

    @Test
    fun `parse message trait with invalid structure reports its expected type and source`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "InvalidTraitStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.InvalidTraitStructure.badTrait",
            sourcePath = "root.components.messageTraitCases.InvalidTraitStructure.badTrait",
            sourceFile = "asyncapi_parser_message_trait_invalid.yaml",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse message trait with boolean content type reports its expected type and source`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "BooleanContentType",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = true,
            path = "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.BooleanContentType.badTrait.contentType",
            sourcePath = "root.components.messageTraitCases.BooleanContentType.badTrait.contentType",
            sourceFile = "asyncapi_parser_message_trait_invalid.yaml",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse message trait with invalid example structure reports its expected type and source`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "InvalidExampleStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.InvalidExampleStructure.badTrait.examples[0]",
            sourcePath = "root.components.messageTraitCases.InvalidExampleStructure.badTrait.examples[0]",
            sourceFile = "asyncapi_parser_message_trait_invalid.yaml",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse message trait with numeric reference reports its expected type and source`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "NumericReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 42,
            path = "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.NumericReference.badTrait.\$ref",
            sourcePath = "root.components.messageTraitCases.NumericReference.badTrait.\$ref",
            sourceFile = "asyncapi_parser_message_trait_invalid.yaml",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse message trait list from an object reports the container type and source`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "ObjectInsteadOfList",
        )
        assertUnexpectedValueType(
            expectedType = "List<Any?>",
            actualType = ParserValueType.OBJECT,
            actualValue = mapOf("badTrait" to mapOf("contentType" to "application/json")),
            path = "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.ObjectInsteadOfList",
            sourcePath = "root.components.messageTraitCases.ObjectInsteadOfList",
            sourceFile = "asyncapi_parser_message_trait_invalid.yaml",
        ) {
            parser.parseList(traitsNode)
        }
    }
}
