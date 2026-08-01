package dev.banking.asyncapi.generator.core.parser.references

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.REFERENCE
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReferenceParserTest : ParserTestSupport() {

    private val parser = ReferenceParser(asyncApiContext)

    @Test
    fun `parse reference element`() {
        val referenceNode = readNode(
            "parser/operations/asyncapi_parser_operations_valid.yaml",
            "operations",
            "receiveLightMeasurement",
            "channel",
        )

        val reference = parser.parseElement(referenceNode)

        assertEquals("#/channels/lightingMeasured", reference.ref)
        assertEquals(REFERENCE, reference.referenceCategoryKey)
    }

    @Test
    fun `parse reference list`() {
        val referencesNode = readNode(
            "parser/operations/asyncapi_parser_operations_valid.yaml",
            "operations",
            "receiveLightMeasurement",
            "messages",
        )

        val references = parser.parseList(referencesNode)

        assertEquals(listOf("#/components/messages/lightMeasured"), references.map { it.ref })
        assertEquals(listOf(REFERENCE), references.map { it.referenceCategoryKey })
    }

    @Test
    fun `parse reference reports missing ref`() {
        val referenceNode = readNode(
            "parser/references/asyncapi_parser_reference_invalid.yaml",
            "components",
            "references",
            "MissingReference",
        )
        assertMissingRequiredMember(
            memberName = $$"$ref",
            path = "asyncapi_parser_reference_invalid.root.components.references.MissingReference.\$ref",
            sourcePath = "root.components.references.MissingReference",
            sourceFile = "asyncapi_parser_reference_invalid.yaml",
        ) {
            parser.parseElement(referenceNode)
        }
    }

    @Test
    fun `parse reference reports non-string ref`() {
        val referenceNode = readNode(
            "parser/references/asyncapi_parser_reference_invalid.yaml",
            "components",
            "references",
            "NumericReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 12345,
            path = "asyncapi_parser_reference_invalid.root.components.references.NumericReference.\$ref",
            sourcePath = "root.components.references.NumericReference.\$ref",
            sourceFile = "asyncapi_parser_reference_invalid.yaml",
        ) {
            parser.parseElement(referenceNode)
        }
    }

    @Test
    fun `parse reference reports explicit null ref`() {
        val referenceNode = readNode(
            "parser/references/asyncapi_parser_reference_invalid.yaml",
            "components",
            "references",
            "NullReference",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_reference_invalid.root.components.references.NullReference.\$ref",
            sourcePath = "root.components.references.NullReference.\$ref",
            sourceFile = "asyncapi_parser_reference_invalid.yaml",
        ) {
            parser.parseElement(referenceNode)
        }
    }

    @Test
    fun `parse reference list reports missing ref at element path`() {
        val referencesNode = readNode(
            "parser/references/asyncapi_parser_reference_invalid.yaml",
            "components",
            "references",
            "ReferenceList",
        )
        assertMissingRequiredMember(
            memberName = $$"$ref",
            path = "asyncapi_parser_reference_invalid.root.components.references.ReferenceList[0].\$ref",
            sourcePath = "root.components.references.ReferenceList[0]",
            sourceFile = "asyncapi_parser_reference_invalid.yaml",
        ) {
            parser.parseList(referencesNode)
        }
    }

    @Test
    fun `parse reference list reports object container`() {
        val referenceNode = readNode(
            "parser/references/asyncapi_parser_reference_invalid.yaml",
            "components",
            "references",
            "MissingReference",
        )

        assertUnexpectedValueType(
            expectedType = "List<Any?>",
            actualType = ParserValueType.OBJECT,
            actualValue = mapOf("summary" to "Reference object missing its reference value"),
            path = "asyncapi_parser_reference_invalid.root.components.references.MissingReference",
            sourcePath = "root.components.references.MissingReference",
            sourceFile = "asyncapi_parser_reference_invalid.yaml",
        ) {
            parser.parseList(referenceNode)
        }
    }
}
