package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SchemaValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `valid simple schema passes validation`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_valid_simple.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)
    }

    @Test
    fun `supported keywords and extension properties pass validation`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_keyword_compatible.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)

        val components = (document.components as ComponentInterface.ComponentInline).component
        val payload = components.schemas?.getValue("CompatiblePayload") as SchemaInterface.SchemaInline
        val optionalName = payload.schema.properties?.getValue("optionalName") as SchemaInterface.SchemaInline
        assertEquals(
            "Preserved extension metadata",
            optionalName.schema.extensions?.get("x-generator-note"),
        )
    }

    @Test
    fun `unsupported schema keywords and explicit dialect mismatch produce structured diagnostics`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_keyword_diagnostics.yaml")

        assertEquals(7, results.errors.size)
        assertEquals(1, results.warnings.size)

        assertFinding(
            results,
            severity = ERROR,
            messageContains = "keyword 'nullable' is not supported by the AsyncAPI 3.0 Schema Object semantics",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path =
                "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.UnsupportedNullable.nullable",
            line = 9,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "keyword '\$defs' is not supported under the generator's JSON Schema Draft 7 semantics",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path = "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.NewerDefinitionKeyword.\$defs",
            line = 13,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains =
                "keyword 'unevaluatedProperties' is not supported under the generator's " +
                    "JSON Schema Draft 7 semantics",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path =
                "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas." +
                    "UnsupportedStructuralKeyword.unevaluatedProperties",
            line = 19,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "keyword 'minLenght' is not supported by the generator",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path = "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.UnknownKeyword.minLenght",
            line = 23,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "declares schema dialect 'https://json-schema.org/draft/2020-12/schema'",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path = "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.NewerDialect.\$schema",
            line = 26,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "keyword 'example' is an unsupported annotation",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path = "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.IgnoredAnnotation.example",
            line = 31,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "keyword 'nullable' is not supported by the AsyncAPI 3.0 Schema Object semantics",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path = "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.NullableReference.nullable",
            line = 35,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "keyword 'items' does not support tuple validation",
            sourceFile = "asyncapi_validator_schema_keyword_diagnostics.yaml",
            path = "asyncapi_validator_schema_keyword_diagnostics.root.components.schemas.TupleItems.items",
            line = 39,
        )
    }

    @Test
    fun `foreign container does not change compatible external schema semantics`() {
        val document = parse("validator/schemas/external/asyncapi_external_compatible.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)
    }

    @Test
    fun `foreign component schemas resolve from all composition keywords`() {
        val document = parse("validator/schemas/external/asyncapi_external_compositions.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)
    }

    @Test
    fun `components schemas in a document path does not change the reference target`() {
        val document = parse("validator/schemas/external/asyncapi_external_schema_map_path.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)
    }

    @Test
    fun `unreferenced incompatible schemas in a foreign container are not validated`() {
        val document = parse("validator/schemas/external/asyncapi_external_selected_schema.yaml")
        val results = asyncApiValidator.validate(document)

        assertNoFindings(results)
    }

    @Test
    fun `referenced transitive schemas use the same compatibility policy`() {
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            parse("validator/schemas/external/asyncapi_external_transitive_invalid.yaml")
        }

        assertEquals(1, exception.errors.size)
        val finding = exception.errors.single()
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

        assertEquals(1, exception.errors.size)
        val finding = exception.errors.single()
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
    fun `schema with structurally invalid type is rejected during parsing`() {
        val exception = assertFailsWith<AsyncApiParseException.ParserDiagnosticFailure> {
            parse("validator/schemas/asyncapi_validator_schema_invalid_type.yaml")
        }

        val diagnostic = assertIs<ParserDiagnostic.UnexpectedValueType>(exception.diagnostic)
        assertEquals("String", diagnostic.expectedType)
        assertEquals(ParserValueType.NUMBER, diagnostic.actualType)
        assertEquals(123, diagnostic.actualValue)
        assertEquals(
            "asyncapi_validator_schema_invalid_type.root.components.schemas.InvalidTypeSchema.type",
            diagnostic.path,
        )
    }

    @Test
    fun `schema with invalid numeric and string constraints throws validation errors`() {
        val document =
            parse("validator/schemas/asyncapi_validator_schema_invalid_constraints.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors for invalid constraints.")
    }

    @Test
    fun `schema validation findings include source locations for nested schema errors`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_invalid_constraints.yaml")

        assertEquals(3, results.errors.size)
        assertEquals(3, results.findings.size)

        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'minimum' (10.0) cannot be greater than 'maximum' (5.0)",
            sourceFile = "asyncapi_validator_schema_invalid_constraints.yaml",
            path = "asyncapi_validator_schema_invalid_constraints.root.components.schemas.InvalidNumericRange.minimum",
            line = 9,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'multipleOf' must be greater than zero",
            sourceFile = "asyncapi_validator_schema_invalid_constraints.yaml",
            path = "asyncapi_validator_schema_invalid_constraints.root.components.schemas.InvalidMultipleOf.multipleOf",
            line = 14,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'minLength' (10) cannot be greater than 'maxLength' (5)",
            sourceFile = "asyncapi_validator_schema_invalid_constraints.yaml",
            path = "asyncapi_validator_schema_invalid_constraints.root.components.schemas.InvalidStringLength.minLength",
            line = 18,
        )
    }

    @Test
    fun `schema with invalid discriminator definition throws validation errors`() {
        val document =
            parse("validator/schemas/asyncapi_validator_schema_invalid_discriminator.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(2, exception.errors.size, "Expected 2 errors for invalid discriminator definitions.")
    }

    @Test
    fun `schema with incompatible default value throws validation errors`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_invalid_default.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors for incompatible default values.")
    }

    @Test
    fun `schema with incompatible const value throws validation errors`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_invalid_const.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors for incompatible const values.")
    }

    @Test
    fun `schema with ambiguous composition or empty fields triggers warnings`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_warnings.yaml")
        val results = asyncApiValidator.validate(document)
        assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertTrue(results.hasErrors(), "Should have errors.")
        assertTrue(results.hasWarnings(), "Should have warnings.")

        val warnings = results.warnings
        assertEquals(2, warnings.size, "Expected 2 warnings for incompatible composition values.")
    }

    @Test
    fun `schema validation findings include source locations for nested schema warnings`() {
        val results = validate("validator/schemas/asyncapi_validator_schema_warnings.yaml")

        assertEquals(2, results.warnings.size)

        assertFinding(
            results,
            severity = WARNING,
            messageContains = "uses multiple composition keywords",
            sourceFile = "asyncapi_validator_schema_warnings.yaml",
            path = "asyncapi_validator_schema_warnings.root.components.schemas.AmbiguousComposition.allOf",
            line = 9,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "defines an empty 'required' list",
            sourceFile = "asyncapi_validator_schema_warnings.yaml",
            path = "asyncapi_validator_schema_warnings.root.components.schemas.EmptyRequiredObject.required",
            line = 17,
        )
    }

    @Test
    fun `schema with invalid array or object structure throws validation errors`() {
        val document = parse("validator/schemas/asyncapi_validator_schema_invalid_structure.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(2, exception.errors.size)
    }

    @Test
    fun `schema with required property default null throws validation error`() {
        val document =
            parse("validator/schemas/asyncapi_validator_schema_default_null_required.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            results.throwErrors()
        }
        assertEquals(1, exception.errors.size)
    }
}
