package com.tietoevry.banking.asyncapi.generator.core.validator.asyncapi

import com.tietoevry.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class AsyncApiValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun validateAsyncApiDocument() {
        val asyncApiDocument = parse("src/test/resources/asyncapi_kafka_single_file_example.yaml")
        val validationResults = asyncApiValidator.validate(asyncApiDocument)
        validationResults.throwWarnings()
        validationResults.throwErrors()
    }
}
