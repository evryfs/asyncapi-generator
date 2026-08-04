package dev.banking.asyncapi.generator.core.validator.asyncapi

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_URN_RECOMMENDED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AsyncApiValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun validateAsyncApiDocument() {
        val asyncApiDocument = parse("asyncapi_kafka_single_file_example.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)
        logWarnings(validationResults)
        throwErrors(validationResults)
    }

    @Test
    fun `accepts an uppercase URN scheme and a specific media type with parameters`() {
        val validationResults = validate("validator/asyncapi/asyncapi_validator_document_formats_valid.yaml")

        assertNoFindings(validationResults)
    }

    @Test
    fun `valid non-URN document identifier produces a source-aware advisory`() {
        val results = validate("validator/asyncapi/asyncapi_validator_document_advisory.yaml")

        assertEquals(1, results.warnings.size)
        assertRule(
            results,
            DOCUMENT_ID_URN_RECOMMENDED,
            path = "asyncapi_validator_document_advisory.root.id",
            line = 2,
        )
    }

    @Test
    fun `validation findings include source locations for top level document diagnostics`() {
        val validationResults = validate("validator/asyncapi/asyncapi_validator_document_invalid.yaml")

        assertEquals(2, validationResults.errors.size)
        assertEquals(2, validationResults.findings.size)
        assertRule(
            validationResults,
            rule = DOCUMENT_ID_FORMAT,
            sourceFile = "asyncapi_validator_document_invalid.yaml",
            path = "asyncapi_validator_document_invalid.root.id",
            line = 2,
        )
        assertRule(
            validationResults,
            rule = DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT,
            sourceFile = "asyncapi_validator_document_invalid.yaml",
            path = "asyncapi_validator_document_invalid.root.defaultContentType",
            line = 3,
        )
    }

    @Test
    fun `equivalent YAML and JSON values produce the same semantic findings`() {
        val yaml = validate("validator/asyncapi/asyncapi_validator_document_invalid.yaml")
        val json = validate("validator/asyncapi/asyncapi_validator_document_invalid_json.json")

        assertEquals(
            yaml.findings.map { Triple(it.code, it.concern, it.severity) },
            json.findings.map { Triple(it.code, it.concern, it.severity) },
        )
        assertRule(
            json,
            DOCUMENT_ID_FORMAT,
            sourceFile = "asyncapi_validator_document_invalid_json.json",
            path = "asyncapi_validator_document_invalid_json.root.id",
            line = 3,
        )
        assertRule(
            json,
            DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT,
            sourceFile = "asyncapi_validator_document_invalid_json.json",
            path = "asyncapi_validator_document_invalid_json.root.defaultContentType",
            line = 4,
        )
    }
}
