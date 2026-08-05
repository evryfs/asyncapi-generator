package dev.banking.asyncapi.generator.core.validator.asyncapi

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_URN_RECOMMENDED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SCHEMA_REQUIRED_UNDECLARED
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

        assertEquals(1, validationResults.warnings.size)
        val required = validationResults.findings.single { it.code == SCHEMA_REQUIRED_UNDECLARED.code }
        assertEquals(SCHEMA_REQUIRED_UNDECLARED.severity, required.severity)
        assertEquals(SCHEMA_REQUIRED_UNDECLARED.concern, required.concern)
        assertEquals("asyncapi_kafka_single_file_example.yaml", required.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_kafka_single_file_example.root.components.schemas.turnOnOffPayload.required",
            required.path,
        )
        assertEquals(382, required.line)
    }

    @Test
    fun `accepts an uppercase URN scheme and a specific media type with parameters`() {
        val validationResults = validate("validator/asyncapi/asyncapi_validator_document_formats_valid.yaml")

        assertEquals(emptyList(), validationResults.findings)
    }

    @Test
    fun `valid non-URN document identifier produces a source-aware advisory`() {
        val results = validate("validator/asyncapi/asyncapi_validator_document_advisory.yaml")

        assertEquals(1, results.warnings.size)
        val advisory = results.findings.single()
        assertEquals(DOCUMENT_ID_URN_RECOMMENDED.code, advisory.code)
        assertEquals(DOCUMENT_ID_URN_RECOMMENDED.severity, advisory.severity)
        assertEquals(DOCUMENT_ID_URN_RECOMMENDED.concern, advisory.concern)
        assertEquals("asyncapi_validator_document_advisory.root.id", advisory.path)
        assertEquals(2, advisory.line)
    }

    @Test
    fun `validation findings include source locations for top level document diagnostics`() {
        val validationResults = validate("validator/asyncapi/asyncapi_validator_document_invalid.yaml")

        assertEquals(2, validationResults.errors.size)
        assertEquals(2, validationResults.findings.size)

        val idFormat = validationResults.findings.single { it.code == DOCUMENT_ID_FORMAT.code }
        assertEquals(DOCUMENT_ID_FORMAT.severity, idFormat.severity)
        assertEquals(DOCUMENT_ID_FORMAT.concern, idFormat.concern)
        assertEquals("asyncapi_validator_document_invalid.yaml", idFormat.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_document_invalid.root.id", idFormat.path)
        assertEquals(2, idFormat.line)

        val defaultContentType =
            validationResults.findings.single { it.code == DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT.code }
        assertEquals(DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT.severity, defaultContentType.severity)
        assertEquals(DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT.concern, defaultContentType.concern)
        assertEquals(
            "asyncapi_validator_document_invalid.yaml",
            defaultContentType.sourceLocation?.file?.name,
        )
        assertEquals("asyncapi_validator_document_invalid.root.defaultContentType", defaultContentType.path)
        assertEquals(3, defaultContentType.line)
    }

    @Test
    fun `equivalent YAML and JSON values produce the same semantic findings`() {
        val yaml = validate("validator/asyncapi/asyncapi_validator_document_invalid.yaml")
        val json = validate("validator/asyncapi/asyncapi_validator_document_invalid_json.json")

        assertEquals(
            yaml.findings.map { Triple(it.code, it.concern, it.severity) },
            json.findings.map { Triple(it.code, it.concern, it.severity) },
        )

        val idFormat =
            json.findings.single { it.code == DOCUMENT_ID_FORMAT.code }
        assertEquals(DOCUMENT_ID_FORMAT.severity, idFormat.severity)
        assertEquals(DOCUMENT_ID_FORMAT.concern, idFormat.concern)
        assertEquals("asyncapi_validator_document_invalid_json.json", idFormat.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_document_invalid_json.root.id", idFormat.path)
        assertEquals(3, idFormat.line)

        val defaultContentType =
            json.findings.single { it.code == DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT.code }
        assertEquals(DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT.severity, defaultContentType.severity)
        assertEquals(DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT.concern, defaultContentType.concern)
        assertEquals(
            "asyncapi_validator_document_invalid_json.json",
            defaultContentType.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_document_invalid_json.root.defaultContentType",
            defaultContentType.path,
        )
        assertEquals(4, defaultContentType.line)
    }
}
