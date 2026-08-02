package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class OperationTraitParserTest {

    private val context = AsyncApiContext()
    private val parser = OperationTraitParser(context)

    @Test
    fun `parses all operation trait fields`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operations_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationTraits")

        val traits = parser.parseMap(traitsNode)

        val kafka = assertIs<OperationTraitInterface.OperationTraitInline>(traits["kafka"]).operationTrait
        assertEquals("Kafka operation defaults", kafka.title)
        assertEquals("Shared Kafka operation settings", kafka.summary)
        assertEquals("Applied to Kafka operations", kafka.description)
        val security = assertIs<SecuritySchemeInterface.SecuritySchemeInline>(kafka.security?.get("scram")).security
        assertEquals("userPassword", security.type)
        val tag = assertIs<TagInterface.TagInline>(kafka.tags?.single()).tag
        assertEquals("kafka", tag.name)
        val externalDocs = assertIs<ExternalDocInterface.ExternalDocInline>(kafka.externalDocs).externalDoc
        assertEquals("https://example.com/docs/operation-trait", externalDocs.url)
        val binding = assertIs<BindingInterface.BindingInline>(kafka.bindings?.get("kafka")).binding
        assertEquals(mapOf("groupId" to "streetlights"), binding.content)
        assertIs<OperationTraitInterface.OperationTraitInline>(traits["logging"])
    }

    @Test
    fun `parse operation trait with invalid structure reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationTraitCases")
            .expectObject().required("InvalidTraitStructure")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-map", diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.InvalidTraitStructure.badTrait",
            diagnostic.path,
        )
        assertEquals(
            "root.components.operationTraitCases.InvalidTraitStructure.badTrait",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operation_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation trait with boolean title reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationTraitCases")
            .expectObject().required("BooleanTitle")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.BOOLEAN, diagnostic.actualType)
        assertEquals(true, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.BooleanTitle.badTrait.title",
            diagnostic.path,
        )
        assertEquals(
            "root.components.operationTraitCases.BooleanTitle.badTrait.title",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operation_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation trait with numeric reference reports its expected type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationTraitCases")
            .expectObject().required("NumericReference")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(42, diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.NumericReference.badTrait.\$ref",
            diagnostic.path,
        )
        assertEquals(
            "root.components.operationTraitCases.NumericReference.badTrait.\$ref",
            diagnostic.sourceLocation.path,
        )
        assertEquals("asyncapi_parser_operation_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse operation trait list from an object reports the container type and source`() {
        val file = TestResources.file("parser/operations/asyncapi_parser_operation_trait_invalid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val traitsNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("operationTraitCases")
            .expectObject().required("ObjectInsteadOfList")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(traitsNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("List<Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.OBJECT, diagnostic.actualType)
        assertEquals(mapOf("badTrait" to mapOf("title" to "valid title")), diagnostic.actualValue)
        assertEquals(
            "asyncapi_parser_operation_trait_invalid.root.components.operationTraitCases.ObjectInsteadOfList",
            diagnostic.path,
        )
        assertEquals("root.components.operationTraitCases.ObjectInsteadOfList", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_parser_operation_trait_invalid.yaml", diagnostic.sourceLocation.file.name)
    }
}
