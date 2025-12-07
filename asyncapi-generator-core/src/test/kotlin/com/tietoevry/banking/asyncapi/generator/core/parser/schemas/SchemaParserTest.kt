package com.tietoevry.banking.asyncapi.generator.core.parser.schemas

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import com.tietoevry.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SchemaParserTest : AbstractParserTest() {

    private val parser = SchemaParser(asyncApiContext)

    @Test
    fun parseSchemas_parser_data_classes() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_valid.yaml")
        val result = parser.parseMap(root.mandatory("components").mandatory("schemas"))

        assertTrue("lightMeasuredPayload" in result)
        assertTrue("turnOnOffPayload" in result)
        assertTrue("dimLightPayload" in result)
        assertTrue("sentAt" in result)
        assertTrue("commandPayload" in result)
        assertTrue("simpleString" in result)
        assertTrue("simpleNumber" in result)
        assertTrue("numberArray" in result)
        assertTrue("complexObject" in result)
        assertTrue("composedSchema" in result)
        assertTrue("conditionalExample" in result)
        assertTrue("asyncApiSpecific" in result)
        assertTrue("referencedSchema" in result)
        assertTrue("allowAnything" in result)
        assertTrue("allowNothing" in result)

        val lightMeasuredPayload = (result["lightMeasuredPayload"] as SchemaInterface.SchemaInline).schema
        val expectedLightMeasuredPayload = lightMeasuredPayload()
        assertThat(lightMeasuredPayload)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedLightMeasuredPayload)

        val turnOnOffPayload = (result["turnOnOffPayload"] as SchemaInterface.SchemaInline).schema
        val expectedTurnOnOffPayload = turnOnOffPayload()
        assertThat(turnOnOffPayload)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedTurnOnOffPayload)

        val dimLightPayload = (result["dimLightPayload"] as SchemaInterface.SchemaInline).schema
        val expectedDimLightPayload = dimLightPayload()
        assertThat(dimLightPayload)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedDimLightPayload)

        val sentAt = (result["sentAt"] as SchemaInterface.SchemaInline).schema
        val expectedSentAt = sentAt()
        assertThat(sentAt)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSentAt)

        val commandPayload = (result["commandPayload"] as SchemaInterface.SchemaInline).schema
        val expectedCommandPayload = commandPayload()
        assertThat(commandPayload)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedCommandPayload)

        val simpleString = (result["simpleString"] as SchemaInterface.SchemaInline).schema
        val expectedSimpleString = simpleString()
        assertThat(simpleString)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSimpleString)

        val simpleNumber = (result["simpleNumber"] as SchemaInterface.SchemaInline).schema
        val expectedSimpleNumber = simpleNumber()
        assertThat(simpleNumber)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSimpleNumber)

        val numberArray = (result["numberArray"] as SchemaInterface.SchemaInline).schema
        val expectedNumberArray = numberArray()
        assertThat(numberArray)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedNumberArray)

        val complexObject = (result["complexObject"] as SchemaInterface.SchemaInline).schema
        val expectedComplexObject = complexObject()
        assertThat(complexObject)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedComplexObject)

        val composedSchema = (result["composedSchema"] as SchemaInterface.SchemaInline).schema
        val expectedComposedSchema = composedSchema()
        assertThat(composedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedComposedSchema)

        val conditionalExample = (result["conditionalExample"] as SchemaInterface.SchemaInline).schema
        val expectedConditionalExample = conditionalExample()
        assertThat(conditionalExample)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedConditionalExample)

        val asyncApiSpecific = (result["asyncApiSpecific"] as SchemaInterface.SchemaInline).schema
        val expectedAsyncApiSpecific = asyncApiSpecific()
        assertThat(asyncApiSpecific)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedAsyncApiSpecific)

        val referencedSchema = (result["referencedSchema"] as SchemaInterface.SchemaReference).reference
        val expectedReferencedSchema = referencedSchema()
        assertThat(referencedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedReferencedSchema)

        val allowAnything = result["allowAnything"] as SchemaInterface.BooleanSchema
        val expectedAllowAnything = allowAnything()
        assertThat(allowAnything)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedAllowAnything)

        val allowNothing = result["allowNothing"] as SchemaInterface.BooleanSchema
        val expectedAllowNothing = allowNothing()
        assertThat(allowNothing)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedAllowNothing)
    }

    @Test
    fun parseSchemas_parser_SimpleObject_asyncapi_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("SimpleObject")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = simpleObject()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_NestedObject_asyncapi_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("NestedObject")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = nestedObject()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_ArrayOfObjects_asyncapi_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("ArrayOfObjects")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = arrayOfObjects()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_EnumAndConst_asyncapi_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("EnumAndConst")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = enumAndConst()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_AllosAll_and_DenyAll_asyncapi_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val allowAllNode = root.mandatory("components").mandatory("schemas").mandatory("AllowAll")
        val allowAll = parser.parseElement(allowAllNode) as SchemaInterface.BooleanSchema
        val expectedAllowAll = SchemaInterface.BooleanSchema(value = true)
        assertThat(allowAll)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedAllowAll)

        val denyAllNode = root.mandatory("components").mandatory("schemas").mandatory("DenyAll")
        val denyAll = parser.parseElement(denyAllNode) as SchemaInterface.BooleanSchema
        val expectedDenyAll = SchemaInterface.BooleanSchema(value = false)
        assertThat(denyAll)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedDenyAll)
    }

    @Test
    fun parseSchemas_parser_Combined_asyncapi_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("Combined")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = combined()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_Conditional_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("Conditional")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = conditional()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_ObjectWithDeps_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("ObjectWithDeps")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = objectWithDeps()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_ArrayWithContains_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("ArrayWithContains")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = arrayWithContains()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_FlexibleObject_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("FlexibleObject")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaInline).schema
        val expectedSchema = flexibleObject()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_UserRef_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("UserRef")
        val nestedObjectSchema = (parser.parseElement(node) as SchemaInterface.SchemaReference).reference
        val expectedSchema = Reference(ref = "\"#/components/schemas/SimpleObject")
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_InvalidCoercions_schema_parser_assertion_yaml() {
        val root = readYaml("src/test/resources/schemas/asyncapi_schema_parser_assertion.yaml")
        val node = root.mandatory("components").mandatory("schemas").mandatory("InvalidCoercions")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseElement(node)
        }
    }

    @Test
    fun `parse schema with property and schema dependencies`() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_dependencies.yaml")
        val productNode = root.mandatory("components").mandatory("schemas").mandatory("Product")
        val productSchema = (parser.parseElement(productNode) as SchemaInterface.SchemaInline).schema

        assertNotNull(productSchema.dependencies, "Product schema should have dependencies")
        assertEquals(2, productSchema.dependencies.size, "Product schema should have 2 dependencies")

        val creditCardDependency = productSchema.dependencies["credit_card"]
        assertNotNull(creditCardDependency, "Credit card dependency should exist")
        assertTrue(creditCardDependency is List<*>, "Credit card dependency should be a list of strings")
        assertEquals(listOf("\"billing_address"), creditCardDependency)

        val nameDependency = productSchema.dependencies["name"]
        assertNotNull(nameDependency, "Name dependency (schema) should exist")
        assertTrue(
            nameDependency is SchemaInterface.SchemaInline,
            "Name dependency should be a SchemaInterface.SchemaInline"
        ) // This will fail!

        val nameDependencySchema = (nameDependency as SchemaInterface.SchemaInline).schema
        assertEquals("object", nameDependencySchema.type, "Schema dependency type should be object")
        assertEquals(listOf("\"category"), nameDependencySchema.required, "Schema dependency required property missing")
    }

    @Test
    fun `parse recursive external references populates context correctly`() {
        val root = readYaml("src/test/resources/parser/schemas/references/main.yaml")
        parser.parseMap(root.mandatory("components").mandatory("schemas"))
        val modelPaths = asyncApiContext.modelRepository.getModelsByPath()
        val expectedLevel2Path = "level2.root.components.schemas.Level2Object"
        assertTrue(
            modelPaths.containsKey(expectedLevel2Path),
            "Context should contain Level2Object from recursive load"
        )

        val level2Schema = modelPaths[expectedLevel2Path] as Schema
        assertEquals("\"I am level 2 deep", level2Schema.description)
    }

    @Test
    fun `parse recursive composition (allOf inside oneOf)`() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml")
        val schemaMap = parser.parseMap(root.mandatory("components").mandatory("schemas"))

        val recursiveSchema = (schemaMap["RecursiveComposition"] as SchemaInterface.SchemaInline).schema
        assertNotNull(recursiveSchema.oneOf, "oneOf should not be null")
        assertEquals(2, recursiveSchema.oneOf.size, "oneOf should have 2 elements")

        // First element is an inline schema with allOf
        val firstOption = recursiveSchema.oneOf[0]
        assertTrue(firstOption is SchemaInterface.SchemaInline, "First oneOf option should be inline")
        val nestedAllOfSchema = (firstOption as SchemaInterface.SchemaInline).schema

        assertNotNull(nestedAllOfSchema.allOf, "Nested schema should have allOf")
        assertEquals(2, nestedAllOfSchema.allOf.size, "Nested allOf should have 2 elements")

        // Check registration of nested inline object
        val nestedInline = nestedAllOfSchema.allOf[0] as SchemaInterface.SchemaInline
        assertTrue(
            asyncApiContext.modelRepository.getModelsByInstance().containsKey(nestedInline.schema),
            "Nested inline schema in allOf should be registered"
        )
    }

    @Test
    fun `parse mixed references and inline schemas in anyOf`() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml")
        val schemaMap = parser.parseMap(root.mandatory("components").mandatory("schemas"))

        val mixedSchema = (schemaMap["MixedComposition"] as SchemaInterface.SchemaInline).schema
        assertNotNull(mixedSchema.anyOf, "anyOf should not be null")
        assertEquals(2, mixedSchema.anyOf.size)

        val refOption = mixedSchema.anyOf[0]
        assertTrue(refOption is SchemaInterface.SchemaReference, "First option should be a reference")

        val inlineOption = mixedSchema.anyOf[1]
        assertTrue(inlineOption is SchemaInterface.SchemaInline, "Second option should be inline")
        assertEquals("Inline boolean schema", (inlineOption as SchemaInterface.SchemaInline).schema.description)
    }

    @Test
    fun `parse empty allOf`() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml")
        val schemaMap = parser.parseMap(root.mandatory("components").mandatory("schemas"))

        val emptyAllOfSchema = (schemaMap["EmptyAllOf"] as SchemaInterface.SchemaInline).schema
        assertNotNull(
            emptyAllOfSchema.allOf,
            "allOf should be present (even if empty list in source, parser might coerce to empty list)"
        )
        assertTrue(emptyAllOfSchema.allOf.isEmpty(), "allOf should be empty")
    }

    @Test
    fun `parse untyped oneOf`() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml")
        val schemaMap = parser.parseMap(root.mandatory("components").mandatory("schemas"))

        val untypedSchema = (schemaMap["UntypedOneOf"] as SchemaInterface.SchemaInline).schema
        assertEquals(null, untypedSchema.type, "Type should be null for untyped oneOf parent")
        assertNotNull(untypedSchema.oneOf, "oneOf should be present")
    }

    @Test
    fun `parse Schema with invalid enum type throws InvalidValue`() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val schemaNode = root.mandatory("components").mandatory("schemas").mandatory("InvalidEnumSchema")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseElement(schemaNode)
        }
    }

    @Test
    fun `parse Schema with invalid dependencies type (list instead of map) throws InvalidValue`() {
        val root = readYaml("src/test/resources/parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val schemaNode = root.mandatory("components").mandatory("schemas").mandatory("MissingDependenciesObject")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseElement(schemaNode)
        }
    }
}
