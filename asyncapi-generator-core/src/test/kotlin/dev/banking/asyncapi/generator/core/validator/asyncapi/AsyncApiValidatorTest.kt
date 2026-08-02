package dev.banking.asyncapi.generator.core.validator.asyncapi

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_DEFAULT_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.DOCUMENT_ID_FORMAT
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.ValidationStage
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
    fun `validates parsed document through validator stage contract`() {
        val asyncApiDocument = parse("validator/info/asyncapi_validator_info_valid_simple.yaml")
        val validationStage: ValidationStage = asyncApiValidator

        val validationResults = validationStage.validate(asyncApiDocument)

        assertNoFindings(validationResults)
    }

    @Test
    fun `accepts an uppercase URN scheme and a specific media type with parameters`() {
        val validationResults = validate("validator/asyncapi/asyncapi_validator_document_formats_valid.yaml")

        assertNoFindings(validationResults)
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
}
