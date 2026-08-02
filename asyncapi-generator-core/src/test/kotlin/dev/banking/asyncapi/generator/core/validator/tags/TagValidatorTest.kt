package dev.banking.asyncapi.generator.core.validator.tags

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TagValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid tags trigger errors and warnings`() {
        val document = parse("validator/tags/asyncapi_validator_tag_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error (empty name).")
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'name' is required and cannot be empty",
            sourceFile = "asyncapi_validator_tag_invalid.yaml",
            path = "asyncapi_validator_tag_invalid.root.components.tags.InvalidTag.name",
            line = 9,
        )
    }
}
