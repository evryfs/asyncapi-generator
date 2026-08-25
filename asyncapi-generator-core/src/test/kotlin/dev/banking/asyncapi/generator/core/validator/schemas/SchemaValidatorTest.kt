package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ARRAY_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ANNOTATION_IGNORED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_CONST_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEFAULT_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DEPENDENCY_ARRAY_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DIALECT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_PROPERTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_DISCRIMINATOR_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_KEYWORD_UNSUPPORTED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_MULTIPLE_OF
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_NUMERIC_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_OBJECT_SIZE_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNDECLARED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_STRING_LENGTH_RANGE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_NONEMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_TYPE_ARRAY_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchemaValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `valid simple schema passes validation`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_valid_simple.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `exact schema values and valid keyword combinations pass validation`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_exact_values_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `exact Java-compatible pattern passes validation`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_pattern_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `source incompatible Draft 7 constructs pass semantic validation`() {
        val results = validate("generator/source-incompatible-schema-features.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `type arrays enum equality and explicit null values use exact semantics`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_exact_values_invalid.yaml")

        assertEquals(7, results.errors.size)
        val emptyType = results.findings.single {
            it.code == SCHEMA_TYPE_ARRAY_NONEMPTY.code
        }
        assertEquals(SCHEMA_TYPE_ARRAY_NONEMPTY.code, emptyType.code)
        assertEquals(SCHEMA_TYPE_ARRAY_NONEMPTY.severity, emptyType.severity)
        assertEquals(SCHEMA_TYPE_ARRAY_NONEMPTY.concern, emptyType.concern)
        assertEquals(
            "asyncapi_validator_schema_exact_values_invalid.root.components.schemas.EmptyTypeArray.type",
            emptyType.path,
        )
        assertEquals(8, emptyType.line)

        val typeUnique = results.findings.single {
            it.code == SCHEMA_TYPE_ARRAY_UNIQUE.code
        }
        assertEquals(SCHEMA_TYPE_ARRAY_UNIQUE.code, typeUnique.code)
        assertEquals(SCHEMA_TYPE_ARRAY_UNIQUE.severity, typeUnique.severity)
        assertEquals(SCHEMA_TYPE_ARRAY_UNIQUE.concern, typeUnique.concern)
        assertEquals(
            "asyncapi_validator_schema_exact_values_invalid.root.components.schemas.DuplicateTypeArray.type",
            typeUnique.path,
        )
        assertEquals(10, typeUnique.line)

        val explicitStringType = results.findings.single {
            it.code == SCHEMA_TYPE.code &&
                it.path ==
                "asyncapi_validator_schema_exact_values_invalid.root.components.schemas.UnnormalizedType.type"
        }
        assertEquals(SCHEMA_TYPE.code, explicitStringType.code)
        assertEquals(SCHEMA_TYPE.severity, explicitStringType.severity)
        assertEquals(SCHEMA_TYPE.concern, explicitStringType.concern)
        assertEquals(12, explicitStringType.line)

        val enumUnique = results.findings.single {
            it.code == SCHEMA_ENUM_UNIQUE.code
        }
        assertEquals(SCHEMA_ENUM_UNIQUE.code, enumUnique.code)
        assertEquals(SCHEMA_ENUM_UNIQUE.severity, enumUnique.severity)
        assertEquals(SCHEMA_ENUM_UNIQUE.concern, enumUnique.concern)
        assertEquals(
            "asyncapi_validator_schema_exact_values_invalid.root.components.schemas.DuplicateNumericEnum.enum",
            enumUnique.path,
        )
        assertEquals(15, enumUnique.line)

        val constType = results.findings.single {
            it.code == SCHEMA_CONST_TYPE.code
        }
        assertEquals(SCHEMA_CONST_TYPE.code, constType.code)
        assertEquals(SCHEMA_CONST_TYPE.severity, constType.severity)
        assertEquals(SCHEMA_CONST_TYPE.concern, constType.concern)
        assertEquals(
            "asyncapi_validator_schema_exact_values_invalid.root.components.schemas.NullConstForString.const",
            constType.path,
        )
        assertEquals(18, constType.line)

        val defaultType = results.findings.single {
            it.code == SCHEMA_DEFAULT_TYPE.code
        }
        assertEquals(SCHEMA_DEFAULT_TYPE.code, defaultType.code)
        assertEquals(SCHEMA_DEFAULT_TYPE.severity, defaultType.severity)
        assertEquals(SCHEMA_DEFAULT_TYPE.concern, defaultType.concern)
        assertEquals(
            "asyncapi_validator_schema_exact_values_invalid.root.components.schemas.NullDefaultForString.default",
            defaultType.path,
        )
        assertEquals(21, defaultType.line)

        val explicitNullType = results.findings.single {
            it.code == SCHEMA_TYPE.code &&
                it.path ==
                "asyncapi_validator_schema_exact_values_invalid.root.components.schemas.ExplicitNullType.type"
        }
        assertEquals(SCHEMA_TYPE.code, explicitNullType.code)
        assertEquals(SCHEMA_TYPE.severity, explicitNullType.severity)
        assertEquals(SCHEMA_TYPE.concern, explicitNullType.concern)
        assertEquals(25, explicitNullType.line)
    }

    @Test
    fun `supported keywords and extension properties pass validation`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_keyword_compatible.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)

        val components = (document.components as ComponentInterface.ComponentInline).component
        val payload = components.schemas?.getValue("CompatiblePayload") as SchemaInterface.SchemaInline
        val optionalName = payload.schema.properties?.getValue("optionalName") as SchemaInterface.SchemaInline
        assertEquals(
            "Preserved extension metadata",
            optionalName.schema.extensions?.get("x-generator-note"),
        )
    }

    @Test
    fun `supported AsyncAPI Draft 7 and native schema formats pass ordinary validation`() {
        val results = validate("parser/schemas/asyncapi_parser_schema_format_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `recursive schema structures and boolean schemas pass validation`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_recursive_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `recursive schemas and property dependencies produce source-aware findings`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_recursive_invalid.yaml")

        assertEquals(4, results.errors.size)

        val propertyType = results.findings.single {
            it.code == SCHEMA_TYPE.code &&
            it.path ==
                "asyncapi_validator_schema_recursive_invalid.root.components.schemas.Recursive.patternProperties.^x-.type"
        }
        assertEquals(SCHEMA_TYPE.code, propertyType.code)
        assertEquals(SCHEMA_TYPE.severity, propertyType.severity)
        assertEquals(SCHEMA_TYPE.concern, propertyType.concern)
        assertEquals("asyncapi_validator_schema_recursive_invalid.yaml", propertyType.sourceLocation?.file?.name)
        assertEquals(11, propertyType.line)

        val duplicateDependency = results.findings.single {
            it.code == SCHEMA_DEPENDENCY_ARRAY_UNIQUE.code &&
            it.path ==
                "asyncapi_validator_schema_recursive_invalid.root.components.schemas.Recursive.dependencies.duplicate[1]"
        }
        assertEquals(SCHEMA_DEPENDENCY_ARRAY_UNIQUE.code, duplicateDependency.code)
        assertEquals(SCHEMA_DEPENDENCY_ARRAY_UNIQUE.severity, duplicateDependency.severity)
        assertEquals(SCHEMA_DEPENDENCY_ARRAY_UNIQUE.concern, duplicateDependency.concern)
        assertEquals(16, duplicateDependency.line)

        val nestedType = results.findings.single {
            it.code == SCHEMA_TYPE.code &&
            it.path ==
                "asyncapi_validator_schema_recursive_invalid.root.components.schemas.Recursive.dependencies.nested.properties.value.type"
        }
        assertEquals(SCHEMA_TYPE.code, nestedType.code)
        assertEquals(SCHEMA_TYPE.severity, nestedType.severity)
        assertEquals(SCHEMA_TYPE.concern, nestedType.concern)
        assertEquals("asyncapi_validator_schema_recursive_invalid.yaml", nestedType.sourceLocation?.file?.name)
        assertEquals(21, nestedType.line)

        val tupleItemType = results.findings.single {
            it.code == SCHEMA_TYPE.code &&
            it.path ==
                "asyncapi_validator_schema_recursive_invalid.root.components.schemas.TupleItems.items[1].type"
        }
        assertEquals(SCHEMA_TYPE.code, tupleItemType.code)
        assertEquals(SCHEMA_TYPE.severity, tupleItemType.severity)
        assertEquals(SCHEMA_TYPE.concern, tupleItemType.concern)
        assertEquals(26, tupleItemType.line)
    }

    @Test
    fun `unsupported schema keywords and explicit dialect mismatch produce structured diagnostics`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_keyword_diagnostics.yaml")

        assertEquals(1, results.warnings.size)

        val nullable = results.findings.singleOrNull {
            it.code == SCHEMA_KEYWORD_UNSUPPORTED.code &&
                it.path ==
                "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.UnsupportedNullable.nullable"
        }
        assertNotNull(nullable)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.code, nullable.code)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.severity, nullable.severity)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.concern, nullable.concern)
        assertEquals("asyncapi_validator_schema_keyword_diagnostics.yaml", nullable.sourceLocation?.file?.name)
        assertEquals(9, nullable.line)

        val newerDefinition = results.findings.singleOrNull {
            it.code == SCHEMA_KEYWORD_UNSUPPORTED.code &&
                it.path ==
                "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.NewerDefinitionKeyword.\$defs"
        }
        assertNotNull(newerDefinition)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.code, newerDefinition.code)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.severity, newerDefinition.severity)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.concern, newerDefinition.concern)
        assertEquals(13, newerDefinition.line)

        val unevaluated = results.findings.singleOrNull {
            it.code == SCHEMA_KEYWORD_UNSUPPORTED.code &&
                it.path ==
                "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.UnsupportedStructuralKeyword.unevaluatedProperties"
        }
        assertNotNull(unevaluated)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.code, unevaluated.code)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.severity, unevaluated.severity)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.concern, unevaluated.concern)
        assertEquals(19, unevaluated.line)

        val minLenght = results.findings.singleOrNull {
            it.code == SCHEMA_KEYWORD_UNSUPPORTED.code &&
                it.path ==
                "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.UnknownKeyword.minLenght"
        }
        assertNotNull(minLenght)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.code, minLenght.code)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.severity, minLenght.severity)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.concern, minLenght.concern)
        assertEquals(23, minLenght.line)

        val dialect = results.findings.singleOrNull {
            it.code == SCHEMA_DIALECT.code
        }
        assertNotNull(dialect)
        assertEquals(SCHEMA_DIALECT.code, dialect.code)
        assertEquals(SCHEMA_DIALECT.severity, dialect.severity)
        assertEquals(SCHEMA_DIALECT.concern, dialect.concern)
        assertEquals(26, dialect.line)

        val annotation = results.findings.singleOrNull {
            it.code == SCHEMA_ANNOTATION_IGNORED.code
        }
        assertNotNull(annotation)
        assertEquals(SCHEMA_ANNOTATION_IGNORED.code, annotation.code)
        assertEquals(SCHEMA_ANNOTATION_IGNORED.severity, annotation.severity)
        assertEquals(SCHEMA_ANNOTATION_IGNORED.concern, annotation.concern)
        assertEquals(
            "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.IgnoredAnnotation.example",
            annotation.path,
        )
        assertEquals(31, annotation.line)

        val unsupportedNullableReference = results.findings.single {
            it.code == SCHEMA_KEYWORD_UNSUPPORTED.code &&
                it.path ==
                "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.NullableReference.nullable"
        }
        assertNotNull(unsupportedNullableReference)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.code, unsupportedNullableReference.code)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.severity, unsupportedNullableReference.severity)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.concern, unsupportedNullableReference.concern)
        assertEquals(35, unsupportedNullableReference.line)
    }

    @Test
    fun `foreign container does not change compatible external schema semantics`() {
        val document = parse("validator/schemas/external/asyncapi_external_compatible.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `foreign component schemas resolve from all composition keywords`() {
        val document = parse("validator/schemas/external/asyncapi_external_compositions.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `components schemas in a document path does not change the reference target`() {
        val document = parse("validator/schemas/external/asyncapi_external_schema_map_path.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `unreferenced incompatible schemas in a foreign container are not validated`() {
        val document = parse("validator/schemas/external/asyncapi_external_selected_schema.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `referenced transitive schemas use the same compatibility policy`() {
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            parse("validator/schemas/external/asyncapi_external_transitive_invalid.yaml")
        }

        val finding = exception.errors.single()
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.code, finding.code)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.concern, finding.concern)
        assertEquals(ERROR, finding.severity)
        assertTrue(
            finding.message.contains(
                "keyword 'nullable' is not supported by the AsyncAPI 3.0 Schema Object semantics",
            ),
        )
        assertEquals("foreign_container_transitive_invalid.yaml", finding.sourceLocation?.file?.name)
        assertEquals(
            "foreign_container_transitive_invalid.root.components.schemas." +
                "InvalidDependency.properties.optionalName.nullable",
            finding.path,
        )
        assertEquals(18, finding.line)
    }

    @Test
    fun `external schema keyword diagnostics point to the external source`() {
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            parse("validator/schemas/external/asyncapi_external_nullable.yaml")
        }

        val finding = exception.errors.single()
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.code, finding.code)
        assertEquals(SCHEMA_KEYWORD_UNSUPPORTED.concern, finding.concern)
        assertEquals(ERROR, finding.severity)
        assertTrue(
            finding.message.contains(
                "keyword 'nullable' is not supported by the AsyncAPI 3.0 Schema Object semantics",
            ),
        )
        assertEquals("foreign_container_nullable.yaml", finding.sourceLocation?.file?.name)
        assertEquals(
            "foreign_container_nullable.root.components.schemas.ExternalPayload.properties.optionalName.nullable",
            finding.path,
        )
        assertEquals(13, finding.line)
    }

    @Test
    fun `external json schema explicit null type reports its original source`() {
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            parse("validator/schemas/external/asyncapi_external_null_type.yaml")
        }

        val finding = exception.errors.single()
        assertEquals(SCHEMA_TYPE.code, finding.code)
        assertEquals(SCHEMA_TYPE.concern, finding.concern)
        assertEquals(SCHEMA_TYPE.severity, finding.severity)
        assertEquals(
            "foreign_container_null_type.root.components.schemas.ExternalPayload.type",
            finding.path,
        )
        assertEquals("foreign_container_null_type.json", finding.sourceLocation?.file?.name)
        assertEquals(12, finding.line)
    }

    @Test
    fun `numeric and string constraints require exact positive or nonnegative integer values`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_invalid_constraints.yaml")

        assertEquals(5, results.errors.size)
        assertEquals(5, results.findings.size)

        val minimum = results.findings.single {
            it.code == SCHEMA_NUMERIC_RANGE.code &&
                it.path == "asyncapi_validator_schema_invalid_constraints.root.components.schemas.InvalidNumericRange.minimum"
        }
        assertEquals(SCHEMA_NUMERIC_RANGE.code, minimum.code)
        assertEquals(SCHEMA_NUMERIC_RANGE.severity, minimum.severity)
        assertEquals(SCHEMA_NUMERIC_RANGE.concern, minimum.concern)
        assertEquals(9, minimum.line)

        val multipleOf = results.findings.single {
            it.code == SCHEMA_MULTIPLE_OF.code
        }
        assertEquals(SCHEMA_MULTIPLE_OF.code, multipleOf.code)
        assertEquals(SCHEMA_MULTIPLE_OF.severity, multipleOf.severity)
        assertEquals(SCHEMA_MULTIPLE_OF.concern, multipleOf.concern)
        assertEquals(14, multipleOf.line)

        val minLength = results.findings.single {
            it.code == SCHEMA_STRING_LENGTH.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_constraints.root.components.schemas.InvalidStringLength.minLength"
        }
        assertEquals(SCHEMA_STRING_LENGTH.code, minLength.code)
        assertEquals(SCHEMA_STRING_LENGTH.severity, minLength.severity)
        assertEquals(SCHEMA_STRING_LENGTH.concern, minLength.concern)
        assertEquals(18, minLength.line)

        val maxLength = results.findings.single {
            it.code == SCHEMA_STRING_LENGTH.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_constraints.root.components.schemas.InvalidStringLength.maxLength"
        }
        assertEquals(SCHEMA_STRING_LENGTH.code, maxLength.code)
        assertEquals(SCHEMA_STRING_LENGTH.severity, maxLength.severity)
        assertEquals(SCHEMA_STRING_LENGTH.concern, maxLength.concern)
        assertEquals(19, maxLength.line)

        val lengthRange = results.findings.single {
            it.code == SCHEMA_STRING_LENGTH_RANGE.code
        }
        assertEquals(SCHEMA_STRING_LENGTH_RANGE.code, lengthRange.code)
        assertEquals(SCHEMA_STRING_LENGTH_RANGE.severity, lengthRange.severity)
        assertEquals(SCHEMA_STRING_LENGTH_RANGE.concern, lengthRange.concern)
        assertEquals(23, lengthRange.line)
    }

    @Test
    fun `invalid discriminators report missing local property and required declarations`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_invalid_discriminator.yaml")

        assertEquals(4, results.errors.size)

        val requiredMissing = results.findings.single {
            it.code == SCHEMA_DISCRIMINATOR_REQUIRED.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_discriminator.root.components.schemas.MissingRequiredDisc.discriminator"
        }
        assertEquals(SCHEMA_DISCRIMINATOR_REQUIRED.code, requiredMissing.code)
        assertEquals(SCHEMA_DISCRIMINATOR_REQUIRED.severity, requiredMissing.severity)
        assertEquals(SCHEMA_DISCRIMINATOR_REQUIRED.concern, requiredMissing.concern)
        assertEquals(13, requiredMissing.line)

        val propertyMissing = results.findings.single {
            it.code == SCHEMA_DISCRIMINATOR_PROPERTY.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_discriminator.root.components.schemas.MissingPropertyDisc.discriminator"
        }
        assertEquals(SCHEMA_DISCRIMINATOR_PROPERTY.code, propertyMissing.code)
        assertEquals(SCHEMA_DISCRIMINATOR_PROPERTY.severity, propertyMissing.severity)
        assertEquals(SCHEMA_DISCRIMINATOR_PROPERTY.concern, propertyMissing.concern)
        assertEquals(22, propertyMissing.line)

        val both = results.findings.filter {
            it.path ==
                "asyncapi_validator_schema_invalid_discriminator.root.components.schemas.MissingCollections.discriminator"
        }
        assertEquals(2, both.size)
        assertEquals(
            28,
            both.single { it.code == SCHEMA_DISCRIMINATOR_REQUIRED.code }.line,
        )
        assertEquals(28, both.single { it.code == SCHEMA_DISCRIMINATOR_PROPERTY.code }.line)
    }

    @Test
    fun `required property declarations follow compositions conditionals and reference cycles`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_property_declarations.yaml")

        assertEquals(1, results.warnings.size)
        val required = results.findings.single { it.code == SCHEMA_REQUIRED_UNDECLARED.code }
        assertEquals(SCHEMA_REQUIRED_UNDECLARED.code, required.code)
        assertEquals(SCHEMA_REQUIRED_UNDECLARED.severity, required.severity)
        assertEquals(SCHEMA_REQUIRED_UNDECLARED.concern, required.concern)
        assertEquals(
            "asyncapi_validator_schema_property_declarations.root.components.schemas." +
                "MissingDeclaration.required",
            required.path,
        )
        assertEquals(55, required.line)
    }

    @Test
    fun `external allOf properties satisfy inline required declarations`() {
        val results = validate("parser/openapi/asyncapi-single-allof-example.yml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `schema with incompatible default value throws validation errors`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_invalid_default.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(3, results.errors.size)
    }

    @Test
    fun `schema with incompatible const value throws validation errors`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_invalid_const.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(3, results.errors.size)
    }

    @Test
    fun `combined collection rules report expected errors`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_collection_rules_invalid.yaml")

        assertEquals(2, results.errors.size)
        assertEquals(0, results.warnings.size)

        val enumEmpty = results.findings.single {
            it.code == SCHEMA_ENUM_EMPTY.code
        }
        assertEquals(SCHEMA_ENUM_EMPTY.code, enumEmpty.code)
        assertEquals(SCHEMA_ENUM_EMPTY.severity, enumEmpty.severity)
        assertEquals(SCHEMA_ENUM_EMPTY.concern, enumEmpty.concern)
        assertEquals("asyncapi_validator_schema_collection_rules_invalid.root.components.schemas.EmptyEnumString.enum", enumEmpty.path)
        assertEquals(10, enumEmpty.line)

        val requiredUnique = results.findings.single {
            it.code == SCHEMA_REQUIRED_UNIQUE.code
        }
        assertEquals(SCHEMA_REQUIRED_UNIQUE.code, requiredUnique.code)
        assertEquals(SCHEMA_REQUIRED_UNIQUE.severity, requiredUnique.severity)
        assertEquals(SCHEMA_REQUIRED_UNIQUE.concern, requiredUnique.concern)
        assertEquals(
            "asyncapi_validator_schema_collection_rules_invalid.root.components.schemas.DuplicateRequiredObject.required",
            requiredUnique.path,
        )
        assertEquals(18, requiredUnique.line)
    }

    @Test
    fun `schema with invalid array or object structure throws validation errors`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_invalid_structure.yaml")

        assertEquals(6, results.errors.size)

        val invalidMinItems = results.findings.single {
            it.code == SCHEMA_ARRAY_SIZE.code &&
                it.path == "asyncapi_validator_schema_invalid_structure.root.components.schemas.InvalidArray.minItems"
        }
        assertEquals(SCHEMA_ARRAY_SIZE.code, invalidMinItems.code)
        assertEquals(SCHEMA_ARRAY_SIZE.severity, invalidMinItems.severity)
        assertEquals(SCHEMA_ARRAY_SIZE.concern, invalidMinItems.concern)
        assertEquals(9, invalidMinItems.line)

        val invalidMaxItems = results.findings.single {
            it.code == SCHEMA_ARRAY_SIZE.code &&
                it.path == "asyncapi_validator_schema_invalid_structure.root.components.schemas.InvalidArray.maxItems"
        }
        assertEquals(SCHEMA_ARRAY_SIZE.code, invalidMaxItems.code)
        assertEquals(SCHEMA_ARRAY_SIZE.severity, invalidMaxItems.severity)
        assertEquals(SCHEMA_ARRAY_SIZE.concern, invalidMaxItems.concern)
        assertEquals(10, invalidMaxItems.line)

        val arrayRange = results.findings.single {
            it.code == SCHEMA_ARRAY_SIZE_RANGE.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_structure.root.components.schemas.InvalidArrayRange.minItems"
        }
        assertEquals(SCHEMA_ARRAY_SIZE_RANGE.code, arrayRange.code)
        assertEquals(SCHEMA_ARRAY_SIZE_RANGE.severity, arrayRange.severity)
        assertEquals(SCHEMA_ARRAY_SIZE_RANGE.concern, arrayRange.concern)
        assertEquals(14, arrayRange.line)

        val minProperties = results.findings.single {
            it.code == SCHEMA_OBJECT_SIZE.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_structure.root.components.schemas.InvalidObject.minProperties"
        }
        assertEquals(SCHEMA_OBJECT_SIZE.code, minProperties.code)
        assertEquals(SCHEMA_OBJECT_SIZE.severity, minProperties.severity)
        assertEquals(SCHEMA_OBJECT_SIZE.concern, minProperties.concern)
        assertEquals(19, minProperties.line)

        val maxProperties = results.findings.single {
            it.code == SCHEMA_OBJECT_SIZE.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_structure.root.components.schemas.InvalidObject.maxProperties"
        }
        assertEquals(SCHEMA_OBJECT_SIZE.code, maxProperties.code)
        assertEquals(SCHEMA_OBJECT_SIZE.severity, maxProperties.severity)
        assertEquals(SCHEMA_OBJECT_SIZE.concern, maxProperties.concern)
        assertEquals(20, maxProperties.line)

        val objectRange = results.findings.single {
            it.code == SCHEMA_OBJECT_SIZE_RANGE.code &&
                it.path ==
                "asyncapi_validator_schema_invalid_structure.root.components.schemas.InvalidObjectRange.minProperties"
        }
        assertEquals(SCHEMA_OBJECT_SIZE_RANGE.code, objectRange.code)
        assertEquals(SCHEMA_OBJECT_SIZE_RANGE.severity, objectRange.severity)
        assertEquals(SCHEMA_OBJECT_SIZE_RANGE.concern, objectRange.concern)
        assertEquals(24, objectRange.line)
    }

}
