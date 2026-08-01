package dev.banking.asyncapi.generator.core.validator.asyncapi

import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
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
        validationResults.logWarnings()
        validationResults.throwErrors()
    }

    @Test
    fun `validates parsed document through validator stage contract`() {
        val asyncApiDocument = parse("validator/info/asyncapi_validator_info_valid_simple.yaml")
        val validationStage: ValidationStage = asyncApiValidator

        val validationResults = validationStage.validate(asyncApiDocument)

        assertNoFindings(validationResults)
    }

    @Test
    fun `validation findings include source locations for top level document diagnostics`() {
        val validationResults = validate("validator/asyncapi/asyncapi_validator_document_invalid.yaml")

        assertEquals(2, validationResults.errors.size)
        assertEquals(2, validationResults.findings.size)
        assertFinding(
            validationResults,
            severity = ERROR,
            messageContains = "The 'id' field must conform to the URI format",
            sourceFile = "asyncapi_validator_document_invalid.yaml",
            path = "asyncapi_validator_document_invalid.root.id",
            line = 2,
        )
        assertFinding(
            validationResults,
            severity = ERROR,
            messageContains = "Invalid 'defaultContentType' format",
            sourceFile = "asyncapi_validator_document_invalid.yaml",
            path = "asyncapi_validator_document_invalid.root.defaultContentType",
            line = 3,
        )
    }
}
