package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SchemaParserTest : ParserTestSupport() {

    private val parser = SchemaParser(asyncApiContext)

    @Test
    fun parseSchemas_parser_data_classes() {
        val schemasNode = readNode(
            "parser/schemas/asyncapi_parser_schema_valid.yaml",
            "components",
            "schemas",
        )
        val result = parser.parseMap(schemasNode)

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
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "SimpleObject",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = simpleObject()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_NestedObject_asyncapi_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "NestedObject",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = nestedObject()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_ArrayOfObjects_asyncapi_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "ArrayOfObjects",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = arrayOfObjects()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_EnumAndConst_asyncapi_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "EnumAndConst",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = enumAndConst()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_AllosAll_and_DenyAll_asyncapi_schema_parser_assertion_yaml() {
        val allowAllNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "AllowAll",
        )
        val allowAll = parser.parseElement(allowAllNode) as SchemaInterface.BooleanSchema
        val expectedAllowAll = SchemaInterface.BooleanSchema(value = true)
        assertThat(allowAll)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedAllowAll)

        val denyAllNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "DenyAll",
        )
        val denyAll = parser.parseElement(denyAllNode) as SchemaInterface.BooleanSchema
        val expectedDenyAll = SchemaInterface.BooleanSchema(value = false)
        assertThat(denyAll)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedDenyAll)
    }

    @Test
    fun parseSchemas_parser_Combined_asyncapi_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "Combined",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = combined()
        assertThat(nestedObjectSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(expectedSchema)
    }

    @Test
    fun parseSchemas_parser_Conditional_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "Conditional",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = conditional()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_ObjectWithDeps_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "ObjectWithDeps",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = objectWithDeps()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_ArrayWithContains_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "ArrayWithContains",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = arrayWithContains()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_FlexibleObject_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "FlexibleObject",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaInline).schema
        val expectedSchema = flexibleObject()
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_UserRef_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "UserRef",
        )
        val nestedObjectSchema = (parser.parseElement(schemaNode) as SchemaInterface.SchemaReference).reference
        val expectedSchema = Reference(ref = "#/components/schemas/SimpleObject", referenceCategoryKey = SCHEMA)
        assertThat(expectedSchema)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(nestedObjectSchema)
    }

    @Test
    fun parseSchemas_parser_InvalidCoercions_schema_parser_assertion_yaml() {
        val schemaNode = readNode(
            "schemas/asyncapi_schema_parser_assertion.yaml",
            "components",
            "schemas",
            "InvalidCoercions",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Number",
            "found String \"10\"",
            "quoted numbers are strings in YAML",
            "asyncapi_schema_parser_assertion.yaml",
            "asyncapi_schema_parser_assertion.root.components.schemas.InvalidCoercions.maxLength",
        ) {
            parser.parseElement(schemaNode)
        }
    }

    @Test
    fun `parse schema with property and schema dependencies`() {
        val productNode = readNode(
            "parser/schemas/asyncapi_parser_schema_dependencies.yaml",
            "components",
            "schemas",
            "Product",
        )
        val productSchema = (parser.parseElement(productNode) as SchemaInterface.SchemaInline).schema

        assertNotNull(productSchema.dependencies, "Product schema should have dependencies")
        assertEquals(2, productSchema.dependencies.size, "Product schema should have 2 dependencies")

        val creditCardDependency = productSchema.dependencies["credit_card"]
        assertNotNull(creditCardDependency, "Credit card dependency should exist")
        assertTrue(creditCardDependency is List<*>, "Credit card dependency should be a list of strings")
        assertEquals(listOf("billing_address"), creditCardDependency)

        val nameDependency = productSchema.dependencies["name"]
        assertNotNull(nameDependency, "Name dependency (schema) should exist")
        assertTrue(
            nameDependency is SchemaInterface.SchemaInline,
            "Name dependency should be a SchemaInterface.SchemaInline"
        ) // This will fail!

        val nameDependencySchema = (nameDependency as SchemaInterface.SchemaInline).schema
        assertEquals("object", nameDependencySchema.type, "Schema dependency type should be object")
        assertEquals(listOf("category"), nameDependencySchema.required, "Schema dependency required property missing")
    }

    @Test
    fun `parse recursive external references populates context correctly`() {
        val schemasNode = readNode(
            "parser/schemas/references/main.yaml",
            "components",
            "schemas",
        )
        parser.parseMap(schemasNode)
        val modelPaths = asyncApiContext.modelRepository.getModelsByPath()
        val expectedLevel2Path = "level2.root.components.schemas.Level2Object"
        assertTrue(
            modelPaths.containsKey(expectedLevel2Path),
            "Context should contain Level2Object from recursive load"
        )

        val level2Schema = modelPaths[expectedLevel2Path] as Schema
        assertEquals("I am level 2 deep", level2Schema.description)

        val mainSchema = modelPaths["main.root.components.schemas.MainObject"] as Schema
        val mainLocation = assertNotNull(asyncApiContext.getSourceLocation(mainSchema))
        assertEquals("main.yaml", mainLocation.file.name)
        assertEquals("main.root.components.schemas.MainObject", mainLocation.path)
        assertEquals(7, mainLocation.line)

        val level2Location = assertNotNull(asyncApiContext.getSourceLocation(level2Schema))
        assertEquals("level2.yaml", level2Location.file.name)
        assertEquals("level2.root.components.schemas.Level2Object", level2Location.path)
        assertEquals(7, level2Location.line)

        val level2DescriptionLocation = assertNotNull(
            asyncApiContext.getSourceLocation(level2Schema, level2Schema::description)
        )
        assertEquals("level2.yaml", level2DescriptionLocation.file.name)
        assertEquals("level2.root.components.schemas.Level2Object.description", level2DescriptionLocation.path)
        assertEquals(9, level2DescriptionLocation.line)
        assertEquals(9, asyncApiContext.getLine(level2Schema, level2Schema::description))

        assertTrue(
            asyncApiContext.pathSnippet(mainLocation.path).contains("main.yaml"),
            "Main schema snippet should use the main file after external files have been loaded"
        )
        assertTrue(
            asyncApiContext.pathSnippet(level2DescriptionLocation.path).contains("level2.yaml"),
            "External schema snippet should use the external file"
        )
    }

    @Test
    fun `parse recursive composition (allOf inside oneOf)`() {
        val schemasNode = readNode(
            "parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml",
            "components",
            "schemas",
        )
        val schemaMap = parser.parseMap(schemasNode)

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
        val schemasNode = readNode(
            "parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml",
            "components",
            "schemas",
        )
        val schemaMap = parser.parseMap(schemasNode)

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
        val schemasNode = readNode(
            "parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml",
            "components",
            "schemas",
        )
        val schemaMap = parser.parseMap(schemasNode)

        val emptyAllOfSchema = (schemaMap["EmptyAllOf"] as SchemaInterface.SchemaInline).schema
        assertNotNull(
            emptyAllOfSchema.allOf,
            "allOf should be present (even if empty list in source, parser might coerce to empty list)"
        )
        assertTrue(emptyAllOfSchema.allOf.isEmpty(), "allOf should be empty")
    }

    @Test
    fun `parse untyped oneOf`() {
        val schemasNode = readNode(
            "parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml",
            "components",
            "schemas",
        )
        val schemaMap = parser.parseMap(schemasNode)

        val untypedSchema = (schemaMap["UntypedOneOf"] as SchemaInterface.SchemaInline).schema
        assertEquals(null, untypedSchema.type, "Type should be null for untyped oneOf parent")
        assertNotNull(untypedSchema.oneOf, "oneOf should be present")
    }

    @Test
    fun `parse Schema with invalid enum type throws InvalidValue`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_negative_test.yaml",
            "components",
            "schemas",
            "InvalidEnumSchema",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected List",
            "asyncapi_parser_schema_negative_test.yaml",
            "asyncapi_parser_schema_negative_test.root.components.schemas.InvalidEnumSchema.enum",
        ) {
            parser.parseElement(schemaNode)
        }
    }

    @Test
    fun `parse Schema with invalid dependencies type (list instead of map) throws InvalidValue`() {
        val schemaNode = readNode(
            "parser/schemas/asyncapi_parser_schema_negative_test.yaml",
            "components",
            "schemas",
            "MissingDependenciesObject",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_schema_negative_test.yaml",
            "asyncapi_parser_schema_negative_test.root.components.schemas.MissingDependenciesObject.dependencies",
        ) {
            parser.parseElement(schemaNode)
        }
    }
}
