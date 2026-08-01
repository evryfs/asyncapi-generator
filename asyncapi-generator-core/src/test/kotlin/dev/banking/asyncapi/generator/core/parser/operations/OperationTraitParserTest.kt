package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class OperationTraitParserTest : ParserTestSupport() {

    private val parser = OperationTraitParser(asyncApiContext)

    @Test
    fun `parse operation traits`() {
        val traitsNode = readNode(
            "parser/operations/asyncapi_parser_operations_valid.yaml",
            "components",
            "operationTraits",
        )

        val traits = parser.parseMap(traitsNode)

        assertTrue(traits["kafka"] is OperationTraitInterface.OperationTraitInline)
        assertTrue(traits["logging"] is OperationTraitInterface.OperationTraitInline)
    }

    @Test
    fun `parse operation trait with invalid structure reports its expected type and source`() {
        val traitsNode = readNode(
            "parser/operations/asyncapi_parser_operation_trait_invalid.yaml",
            "components",
            "operationTraitCases",
            "InvalidTraitStructure",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "not-a-map",
            path = "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.InvalidTraitStructure.badTrait",
            sourcePath = "root.components.operationTraitCases.InvalidTraitStructure.badTrait",
            sourceFile = "asyncapi_parser_operation_trait_invalid.yaml",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse operation trait with boolean title reports its expected type and source`() {
        val traitsNode = readNode(
            "parser/operations/asyncapi_parser_operation_trait_invalid.yaml",
            "components",
            "operationTraitCases",
            "BooleanTitle",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = true,
            path = "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.BooleanTitle.badTrait.title",
            sourcePath = "root.components.operationTraitCases.BooleanTitle.badTrait.title",
            sourceFile = "asyncapi_parser_operation_trait_invalid.yaml",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse operation trait with numeric reference reports its expected type and source`() {
        val traitsNode = readNode(
            "parser/operations/asyncapi_parser_operation_trait_invalid.yaml",
            "components",
            "operationTraitCases",
            "NumericReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 42,
            path = "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.NumericReference.badTrait.\$ref",
            sourcePath = "root.components.operationTraitCases.NumericReference.badTrait.\$ref",
            sourceFile = "asyncapi_parser_operation_trait_invalid.yaml",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse operation trait list from an object reports the container type and source`() {
        val traitsNode = readNode(
            "parser/operations/asyncapi_parser_operation_trait_invalid.yaml",
            "components",
            "operationTraitCases",
            "ObjectInsteadOfList",
        )
        assertUnexpectedValueType(
            expectedType = "List<Any?>",
            actualType = ParserValueType.OBJECT,
            actualValue = mapOf("badTrait" to mapOf("title" to "valid title")),
            path = "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.ObjectInsteadOfList",
            sourcePath = "root.components.operationTraitCases.ObjectInsteadOfList",
            sourceFile = "asyncapi_parser_operation_trait_invalid.yaml",
        ) {
            parser.parseList(traitsNode)
        }
    }
}
