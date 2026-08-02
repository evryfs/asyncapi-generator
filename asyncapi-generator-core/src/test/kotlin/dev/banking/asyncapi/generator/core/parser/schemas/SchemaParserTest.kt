package dev.banking.asyncapi.generator.core.parser.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnosticCategory
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SCHEMA
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.reader.DocumentReaderRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchemaParserTest {

    private val context = AsyncApiContext()
    private val parser = SchemaParser(context)

    @Test
    fun `parse representative component schemas`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemasNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")

        val schemas = parser.parseMap(schemasNode)

        val lightMeasured = assertIs<SchemaInterface.SchemaInline>(schemas["lightMeasuredPayload"]).schema
        assertEquals("object", lightMeasured.type)
        assertEquals(listOf("lumens"), lightMeasured.required)
        assertEquals(2, lightMeasured.examples?.size)
        val lumens = assertIs<SchemaInterface.SchemaInline>(lightMeasured.properties?.get("lumens")).schema
        assertEquals("integer", lumens.type)
        assertEquals(0, lumens.minimum)
        val sentAtReference =
            assertIs<SchemaInterface.SchemaReference>(lightMeasured.properties?.get("sentAt")).reference
        assertEquals("#/components/schemas/sentAt", sentAtReference.ref)
        assertEquals(SCHEMA, sentAtReference.referenceCategoryKey)

        val turnOnOff = assertIs<SchemaInterface.SchemaInline>(schemas["turnOnOffPayload"]).schema
        assertEquals(listOf("command", "myDescription"), turnOnOff.required)
        val command = assertIs<SchemaInterface.SchemaInline>(turnOnOff.properties?.get("command")).schema
        assertEquals(listOf("on", "off"), command.enum)

        val dimLight = assertIs<SchemaInterface.SchemaInline>(schemas["dimLightPayload"]).schema
        assertEquals(true, dimLight.deprecated)
        val percentage = assertIs<SchemaInterface.SchemaInline>(dimLight.properties?.get("percentage")).schema
        assertEquals(0, percentage.minimum)
        assertEquals(100, percentage.maximum)

        val sentAt = assertIs<SchemaInterface.SchemaInline>(schemas["sentAt"]).schema
        assertEquals("string", sentAt.type)
        assertEquals("date-time", sentAt.format)

        val commandPayload = assertIs<SchemaInterface.SchemaInline>(schemas["commandPayload"]).schema
        assertEquals(
            listOf("#/components/schemas/turnOnOffPayload", "#/components/schemas/dimLightPayload"),
            commandPayload.oneOf?.map { assertIs<SchemaInterface.SchemaReference>(it).reference.ref },
        )

        val simpleString = assertIs<SchemaInterface.SchemaInline>(schemas["simpleString"]).schema
        assertEquals("Simple String Example", simpleString.title)
        assertEquals("uuid", simpleString.format)
        assertEquals("abc123", simpleString.default)
        assertEquals(true, simpleString.defaultSet)
        assertEquals("^[a-zA-Z0-9_-]+$", simpleString.pattern)
        assertEquals(36, simpleString.maxLength)
        assertEquals(3, simpleString.minLength)

        val simpleNumber = assertIs<SchemaInterface.SchemaInline>(schemas["simpleNumber"]).schema
        assertEquals(0.5, simpleNumber.multipleOf)
        assertEquals(10, simpleNumber.exclusiveMaximum)
        assertEquals(0, simpleNumber.exclusiveMinimum)

        val numberArray = assertIs<SchemaInterface.SchemaInline>(schemas["numberArray"]).schema
        assertEquals("array", numberArray.type)
        assertEquals(true, numberArray.uniqueItems)
        assertEquals(5, numberArray.maxItems)
        assertEquals(1, numberArray.minItems)
        assertEquals("number", assertIs<SchemaInterface.SchemaInline>(numberArray.items).schema.type)
        assertEquals(3, assertIs<SchemaInterface.SchemaInline>(numberArray.contains).schema.minimum)

        val complex = assertIs<SchemaInterface.SchemaInline>(schemas["complexObject"]).schema
        assertEquals(listOf("name", "age"), complex.required)
        assertEquals(listOf("email"), complex.dependencies?.get("password"))
        assertEquals(false, assertIs<SchemaInterface.BooleanSchema>(complex.additionalProperties).value)
        assertEquals(listOf("string", "null"),
            assertIs<SchemaInterface.SchemaInline>(complex.properties?.get("nickname")).schema.type)

        val composed = assertIs<SchemaInterface.SchemaInline>(schemas["composedSchema"]).schema
        assertEquals(2, composed.allOf?.size)
        assertEquals(2, composed.anyOf?.size)
        assertEquals(2, composed.oneOf?.size)
        assertEquals(null, assertIs<SchemaInterface.SchemaInline>(composed.not).schema.type)

        val conditional = assertIs<SchemaInterface.SchemaInline>(schemas["conditionalExample"]).schema
        val ifSchema = assertIs<SchemaInterface.SchemaInline>(conditional.ifSchema).schema
        val conditionalType = assertIs<SchemaInterface.SchemaInline>(ifSchema.properties?.get("type")).schema
        assertEquals("car", conditionalType.const)
        assertEquals(listOf("wheels"), assertIs<SchemaInterface.SchemaInline>(conditional.thenSchema).schema.required)
        assertEquals(listOf("legs"), assertIs<SchemaInterface.SchemaInline>(conditional.elseSchema).schema.required)

        val asyncApiSpecific = assertIs<SchemaInterface.SchemaInline>(schemas["asyncApiSpecific"]).schema
        assertEquals("type", asyncApiSpecific.discriminator)
        assertEquals(true, asyncApiSpecific.deprecated)
        val externalDocs =
            assertIs<ExternalDocInterface.ExternalDocInline>(asyncApiSpecific.externalDocs).externalDoc
        assertEquals("https://example.com/docs/schema", externalDocs.url)
        val kafkaBinding =
            assertIs<BindingInterface.BindingInline>(asyncApiSpecific.bindings?.get("kafka")).binding
        assertEquals(mapOf("topic" to "my-topic"), kafkaBinding.content)

        val reference = assertIs<SchemaInterface.SchemaReference>(schemas["referencedSchema"]).reference
        assertEquals("#/components/schemas/simpleString", reference.ref)
        assertEquals(SCHEMA, reference.referenceCategoryKey)
        assertEquals(true, assertIs<SchemaInterface.BooleanSchema>(schemas["allowAnything"]).value)
        assertEquals(false, assertIs<SchemaInterface.BooleanSchema>(schemas["allowNothing"]).value)
    }

    @Test
    fun `parse nested composition and dependency schema forms`() {
        val file = TestResources.file("schemas/asyncapi_schema_parser_assertion.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemas = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject()

        val simpleObject = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("SimpleObject")),
        ).schema
        assertEquals("This schema defines the minimal user object used across APIs.", simpleObject.comment)
        assertEquals(listOf("id"), simpleObject.required)
        assertEquals(true,
            assertIs<SchemaInterface.SchemaInline>(simpleObject.properties?.get("active")).schema.default)

        val nestedObject = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("NestedObject")),
        ).schema
        val user = assertIs<SchemaInterface.SchemaInline>(nestedObject.properties?.get("user")).schema
        val profile = assertIs<SchemaInterface.SchemaInline>(user.properties?.get("profile")).schema
        assertEquals(setOf("age", "city"), profile.properties?.keys)

        val array = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("ArrayOfObjects")),
        ).schema
        val item = assertIs<SchemaInterface.SchemaInline>(array.items).schema
        assertEquals(listOf("id", "score"), item.required)
        val score = assertIs<SchemaInterface.SchemaInline>(item.properties?.get("score")).schema
        assertEquals(0, score.minimum)
        assertEquals(100, score.maximum)

        val enumAndConst = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("EnumAndConst")),
        ).schema
        assertEquals(listOf("red", "green", "blue"), enumAndConst.enum)
        assertEquals("red", enumAndConst.const)
        assertEquals(true, enumAndConst.defaultSet)

        assertEquals(
            true,
            assertIs<SchemaInterface.BooleanSchema>(parser.parseElement(schemas.required("AllowAll"))).value,
        )
        assertEquals(
            false,
            assertIs<SchemaInterface.BooleanSchema>(parser.parseElement(schemas.required("DenyAll"))).value,
        )

        val combined = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("Combined")),
        ).schema
        assertEquals(2, combined.allOf?.size)
        assertEquals(2, combined.anyOf?.size)
        assertEquals(listOf(true, false),
            combined.oneOf?.map { assertIs<SchemaInterface.SchemaInline>(it).schema.const })

        val conditional = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("Conditional")),
        ).schema
        val ifSchema = assertIs<SchemaInterface.SchemaInline>(conditional.ifSchema).schema
        val role = assertIs<SchemaInterface.SchemaInline>(ifSchema.properties?.get("role")).schema
        assertEquals("admin", role.const)
        assertEquals(listOf("accessLevel"),
            assertIs<SchemaInterface.SchemaInline>(conditional.thenSchema).schema.required)
        val elseSchema = assertIs<SchemaInterface.SchemaInline>(conditional.elseSchema).schema
        val accessLevel = assertIs<SchemaInterface.SchemaInline>(elseSchema.properties?.get("accessLevel")).schema
        assertEquals(1, accessLevel.const)

        val dependencies = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("ObjectWithDeps")),
        ).schema
        assertEquals(listOf("billing_address"), dependencies.dependencies?.get("credit_card"))
        assertEquals("string",
            assertIs<SchemaInterface.SchemaInline>(dependencies.patternProperties?.get("^S_")).schema.type)

        val withContains = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("ArrayWithContains")),
        ).schema
        assertEquals("integer", assertIs<SchemaInterface.SchemaInline>(withContains.items).schema.type)
        assertEquals(0, assertIs<SchemaInterface.SchemaInline>(withContains.contains).schema.minimum)

        val flexible = assertIs<SchemaInterface.SchemaInline>(
            parser.parseElement(schemas.required("FlexibleObject")),
        ).schema
        assertEquals("string", assertIs<SchemaInterface.SchemaInline>(flexible.additionalProperties).schema.type)
        assertEquals("^[a-zA-Z_][a-zA-Z0-9_]*$",
            assertIs<SchemaInterface.SchemaInline>(flexible.propertyNames).schema.pattern)

        val reference = assertIs<SchemaInterface.SchemaReference>(
            parser.parseElement(schemas.required("UserRef")),
        ).reference
        assertEquals("#/components/schemas/SimpleObject", reference.ref)
        assertEquals(SCHEMA, reference.referenceCategoryKey)
    }

    @Test
    fun `parse schema with property and schema dependencies`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_dependencies.yaml")
        val document = DocumentReaderRegistry.read(file)
        val productNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("Product")

        val product = assertIs<SchemaInterface.SchemaInline>(parser.parseElement(productNode)).schema
        val dependencies = assertNotNull(product.dependencies)

        assertEquals(3, dependencies.size)
        assertEquals(listOf("billing_address"), dependencies["credit_card"])
        val nameDependency = assertIs<SchemaInterface.SchemaInline>(dependencies["name"]).schema
        assertEquals("object", nameDependency.type)
        assertEquals(listOf("category"), nameDependency.required)
        assertEquals(true, assertIs<SchemaInterface.BooleanSchema>(dependencies["unrestricted"]).value)
    }

    @Test
    fun `parse recursive external references with their original source locations`() {
        val file = TestResources.file("parser/schemas/references/main.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemasNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")

        parser.parseMap(schemasNode)
        val models = context.modelRepository.getModelsByPath()
        val main = assertIs<Schema>(models["main.root.components.schemas.MainObject"])
        val level2 = assertIs<Schema>(models["level2.root.components.schemas.Level2Object"])

        assertEquals("I am level 2 deep", level2.description)
        val mainLocation = assertNotNull(context.getSourceLocation(main))
        assertEquals("main.yaml", mainLocation.file.name)
        assertEquals("main.root.components.schemas.MainObject", mainLocation.path)
        assertEquals(7, mainLocation.line)
        val level2Location = assertNotNull(context.getSourceLocation(level2))
        assertEquals("level2.yaml", level2Location.file.name)
        assertEquals(7, level2Location.line)
        val descriptionLocation = assertNotNull(context.getSourceLocation(level2, level2::description))
        assertEquals("level2.root.components.schemas.Level2Object.description", descriptionLocation.path)
        assertEquals(9, descriptionLocation.line)
        assertEquals(9, context.getLine(level2, level2::description))
        assertTrue(context.pathSnippet(mainLocation.path).contains("main.yaml"))
        assertTrue(context.pathSnippet(descriptionLocation.path).contains("level2.yaml"))
    }

    @Test
    fun `parse recursive mixed and empty composition forms`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_composition_edge_cases.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemasNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")

        val schemas = parser.parseMap(schemasNode)

        val recursive = assertIs<SchemaInterface.SchemaInline>(schemas["RecursiveComposition"]).schema
        val recursiveOptions = assertNotNull(recursive.oneOf)
        assertEquals(2, recursiveOptions.size)
        val nested = assertIs<SchemaInterface.SchemaInline>(recursiveOptions[0]).schema
        val nestedAllOf = assertNotNull(nested.allOf)
        assertEquals(2, nestedAllOf.size)
        val registeredInline = assertIs<SchemaInterface.SchemaInline>(nestedAllOf[0]).schema
        assertTrue(context.modelRepository.getModelsByInstance().containsKey(registeredInline))

        val mixed = assertIs<SchemaInterface.SchemaInline>(schemas["MixedComposition"]).schema
        val mixedOptions = assertNotNull(mixed.anyOf)
        assertIs<SchemaInterface.SchemaReference>(mixedOptions[0])
        assertEquals("Inline boolean schema", assertIs<SchemaInterface.SchemaInline>(mixedOptions[1]).schema.description)

        val emptyAllOf = assertIs<SchemaInterface.SchemaInline>(schemas["EmptyAllOf"]).schema
        assertEquals(emptyList(), emptyAllOf.allOf)
        val untyped = assertIs<SchemaInterface.SchemaInline>(schemas["UntypedOneOf"]).schema
        assertNull(untyped.type)
        assertNotNull(untyped.oneOf)
    }

    @Test
    fun `parse schema preserves an explicit null default`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("explicitNullDefault")

        val schema = assertIs<SchemaInterface.SchemaInline>(parser.parseElement(schemaNode)).schema

        assertNull(schema.default)
        assertEquals(true, schema.defaultSet)
    }

    @Test
    fun `parse schema preserves an explicit null const`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("explicitNullConst")

        val schema = assertIs<SchemaInterface.SchemaInline>(parser.parseElement(schemaNode)).schema

        assertNull(schema.const)
        assertEquals(true, schema.constSet)
    }

    @Test
    fun `parse enum without type does not infer a string schema`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("untypedEnum")

        val schema = assertIs<SchemaInterface.SchemaInline>(parser.parseElement(schemaNode)).schema

        assertNull(schema.type)
        assertEquals(listOf("one", 2, null), schema.enum)
    }

    @Test
    fun `parses the remaining supported schema keywords`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_valid.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("keywordCoverage")

        val schema = assertIs<SchemaInterface.SchemaInline>(parser.parseElement(schemaNode)).schema

        assertEquals("https://example.com/schemas/keyword-coverage", schema.id)
        assertEquals("http://json-schema.org/draft-07/schema#", schema.schema)
        assertEquals("base64", schema.contentEncoding)
        assertEquals("application/json", schema.contentMediaType)
        assertEquals(false, assertIs<SchemaInterface.BooleanSchema>(schema.additionalItems).value)
        val identifier = assertIs<SchemaInterface.SchemaInline>(schema.definitions?.get("identifier")).schema
        assertEquals("string", identifier.type)
        assertEquals(4, schema.maxProperties)
        assertEquals(1, schema.minProperties)
        assertEquals(true, schema.readOnly)
        assertEquals(false, schema.writeOnly)
    }

    @Test
    fun `rejects a quoted boolean schema with a source-aware type diagnostic`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("QuotedBooleanSchema")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemaNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals("Boolean", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("true", diagnostic.actualValue)
        assertEquals("root.components.schemas.QuotedBooleanSchema", diagnostic.sourceLocation.path)
    }

    @Test
    fun `retains unsupported tuple items for source-aware validation`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("TupleItemsSchema")

        val schema = assertIs<SchemaInterface.SchemaInline>(parser.parseElement(schemaNode)).schema

        assertNull(schema.items)
        val tupleItems = assertNotNull(schema.tupleItems)
        assertEquals(2, tupleItems.size)
        assertEquals("string", assertIs<SchemaInterface.SchemaInline>(tupleItems[0]).schema.type)
        assertEquals("number", assertIs<SchemaInterface.SchemaInline>(tupleItems[1]).schema.type)
        assertEquals(
            listOf(mapOf("type" to "string"), mapOf("type" to "number")),
            context.getFieldValue(schema, "items"),
        )
        assertEquals(
            "asyncapi_parser_schema_negative_test.root.components.schemas.TupleItemsSchema.items",
            context.getSourceLocation(schema, "items")?.path,
        )
    }

    @Test
    fun `rejects a scalar schema dependency with a source-aware type diagnostic`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("InvalidDependencyScalar")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemaNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("not-a-schema-or-property-list", diagnostic.actualValue)
        assertEquals("root.components.schemas.InvalidDependencyScalar.dependencies.name", diagnostic.sourceLocation.path)
    }

    @Test
    fun `rejects a null schema dependency with a source-aware type diagnostic`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("InvalidDependencyNull")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemaNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals("Map<String, Any?>", diagnostic.expectedType)
        assertEquals(ParserValueType.NULL, diagnostic.actualType)
        assertNull(diagnostic.actualValue)
        assertEquals("root.components.schemas.InvalidDependencyNull.dependencies.name", diagnostic.sourceLocation.path)
    }

    @Test
    fun `rejects a quoted numeric constraint with a source-aware type diagnostic`() {
        val file = TestResources.file("schemas/asyncapi_schema_parser_assertion.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemaNode = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject().required("InvalidCoercions")

        val error = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemaNode)
        }
        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(error.diagnostic)

        assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
        assertEquals("Number", diagnostic.expectedType)
        assertEquals(ParserValueType.STRING, diagnostic.actualType)
        assertEquals("10", diagnostic.actualValue)
        assertEquals("asyncapi_schema_parser_assertion.root.components.schemas.InvalidCoercions.maxLength", diagnostic.path)
        assertEquals("root.components.schemas.InvalidCoercions.maxLength", diagnostic.sourceLocation.path)
        assertEquals("asyncapi_schema_parser_assertion.yaml", diagnostic.sourceLocation.file.name)
    }

    @Test
    fun `parse malformed schema fields reports source-aware diagnostics`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val document = DocumentReaderRegistry.read(file)
        val schemas = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject().required("schemas")
            .expectObject()

        val invalidEnumError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("InvalidEnumSchema"))
        }
        val invalidEnum = assertIs<ParserDiagnostic.UnexpectedValueType>(invalidEnumError.diagnostic)
        assertEquals("List<Any?>", invalidEnum.expectedType)
        assertEquals(ParserValueType.STRING, invalidEnum.actualType)
        assertEquals("not-a-list", invalidEnum.actualValue)
        assertEquals("asyncapi_parser_schema_negative_test.root.components.schemas.InvalidEnumSchema.enum", invalidEnum.path)
        assertEquals("root.components.schemas.InvalidEnumSchema.enum", invalidEnum.sourceLocation.path)

        val invalidTypeError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("InvalidTypeSchema"))
        }
        val invalidType = assertIs<ParserDiagnostic.UnexpectedValueType>(invalidTypeError.diagnostic)
        assertEquals("String", invalidType.expectedType)
        assertEquals(ParserValueType.NUMBER, invalidType.actualType)
        assertEquals(123, invalidType.actualValue)
        assertEquals("asyncapi_parser_schema_negative_test.root.components.schemas.InvalidTypeSchema.type", invalidType.path)

        val invalidTypeEntryError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("InvalidTypeArraySchema"))
        }
        val invalidTypeEntry = assertIs<ParserDiagnostic.UnexpectedValueType>(invalidTypeEntryError.diagnostic)
        assertEquals("String", invalidTypeEntry.expectedType)
        assertEquals(ParserValueType.NUMBER, invalidTypeEntry.actualType)
        assertEquals(7, invalidTypeEntry.actualValue)
        assertEquals("root.components.schemas.InvalidTypeArraySchema.type[1]", invalidTypeEntry.sourceLocation.path)

        val nullReferenceError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("NullReferenceSchema"))
        }
        val nullReference = assertIs<ParserDiagnostic.UnexpectedValueType>(nullReferenceError.diagnostic)
        assertEquals("String", nullReference.expectedType)
        assertEquals(ParserValueType.NULL, nullReference.actualType)
        assertNull(nullReference.actualValue)
        assertEquals("root.components.schemas.NullReferenceSchema.\$ref", nullReference.sourceLocation.path)

        val invalidSchemaUriError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("InvalidSchemaUri"))
        }
        val invalidSchemaUri = assertIs<ParserDiagnostic.UnexpectedValueType>(invalidSchemaUriError.diagnostic)
        assertEquals("String", invalidSchemaUri.expectedType)
        assertEquals(ParserValueType.BOOLEAN, invalidSchemaUri.actualType)
        assertEquals(false, invalidSchemaUri.actualValue)
        assertEquals("root.components.schemas.InvalidSchemaUri.\$schema", invalidSchemaUri.sourceLocation.path)

        val exclusiveMaximumError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("InvalidExclusiveMaximum"))
        }
        val exclusiveMaximum = assertIs<ParserDiagnostic.UnexpectedValueType>(exclusiveMaximumError.diagnostic)
        assertEquals("Number", exclusiveMaximum.expectedType)
        assertEquals(ParserValueType.STRING, exclusiveMaximum.actualType)
        assertEquals("10", exclusiveMaximum.actualValue)
        assertEquals("root.components.schemas.InvalidExclusiveMaximum.exclusiveMaximum", exclusiveMaximum.sourceLocation.path)

        val discriminatorError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("InvalidDiscriminator"))
        }
        val discriminator = assertIs<ParserDiagnostic.UnexpectedValueType>(discriminatorError.diagnostic)
        assertEquals("String", discriminator.expectedType)
        assertEquals(ParserValueType.OBJECT, discriminator.actualType)
        assertEquals(mapOf("propertyName" to "eventType"), discriminator.actualValue)
        assertEquals("root.components.schemas.InvalidDiscriminator.discriminator", discriminator.sourceLocation.path)

        val allOfError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("InvalidAllOf"))
        }
        val allOf = assertIs<ParserDiagnostic.UnexpectedValueType>(allOfError.diagnostic)
        assertEquals("List<Any?>", allOf.expectedType)
        assertEquals(ParserValueType.OBJECT, allOf.actualType)
        assertEquals(mapOf("invalid" to mapOf("type" to "string")), allOf.actualValue)
        assertEquals("root.components.schemas.InvalidAllOf.allOf", allOf.sourceLocation.path)

        listOf(invalidEnum, invalidType, invalidTypeEntry, nullReference, invalidSchemaUri,
            exclusiveMaximum, discriminator, allOf).forEach { diagnostic ->
            assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
            assertEquals("asyncapi_parser_schema_negative_test.yaml", diagnostic.sourceLocation.file.name)
        }
    }

    @Test
    fun `parse malformed schema containers reports source-aware diagnostics`() {
        val file = TestResources.file("parser/schemas/asyncapi_parser_schema_negative_test.yaml")
        val document = DocumentReaderRegistry.read(file)
        val components = ParserNodeFactory.root(document, context)
            .expectObject().required("components")
            .expectObject()

        val schemas = components.required("schemas").expectObject()
        val dependenciesError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseElement(schemas.required("MissingDependenciesObject"))
        }
        val dependencies = assertIs<ParserDiagnostic.UnexpectedValueType>(dependenciesError.diagnostic)
        assertEquals("Map<String, Any?>", dependencies.expectedType)
        assertEquals(ParserValueType.ARRAY, dependencies.actualType)
        assertEquals(listOf("wrong", "type"), dependencies.actualValue)
        assertEquals("root.components.schemas.MissingDependenciesObject.dependencies", dependencies.sourceLocation.path)

        val cases = components.required("schemaCases").expectObject()
        val mapError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseMap(cases.required("ArrayInsteadOfMap"))
        }
        val mapDiagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(mapError.diagnostic)
        assertEquals("Map<String, Any?>", mapDiagnostic.expectedType)
        assertEquals(ParserValueType.ARRAY, mapDiagnostic.actualType)
        assertEquals(listOf(mapOf("type" to "string")), mapDiagnostic.actualValue)
        assertEquals("root.components.schemaCases.ArrayInsteadOfMap", mapDiagnostic.sourceLocation.path)

        val listError = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parser.parseList(cases.required("ObjectInsteadOfList"))
        }
        val listDiagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(listError.diagnostic)
        assertEquals("List<Any?>", listDiagnostic.expectedType)
        assertEquals(ParserValueType.OBJECT, listDiagnostic.actualType)
        assertEquals(mapOf("schema" to mapOf("type" to "string")), listDiagnostic.actualValue)
        assertEquals("root.components.schemaCases.ObjectInsteadOfList", listDiagnostic.sourceLocation.path)

        listOf(dependencies, mapDiagnostic, listDiagnostic).forEach { diagnostic ->
            assertEquals(ParserDiagnosticCategory.UNEXPECTED_VALUE_TYPE, diagnostic.category)
            assertEquals("asyncapi_parser_schema_negative_test.yaml", diagnostic.sourceLocation.file.name)
        }
    }

}
