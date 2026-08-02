package dev.banking.asyncapi.generator.core.validator.channels

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChannelValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `channel address parameter missing definition throws validation error`() {
        val document = parse("validator/channels/asyncapi_validator_channel_parameter_mismatch.yaml")
        val validationResults = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(validationResults)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for missing parameter definition.")
        assertFinding(
            validationResults,
            severity = ERROR,
            messageContains = "address uses parameters [userId]",
            sourceFile = "asyncapi_validator_channel_parameter_mismatch.yaml",
            path = "asyncapi_validator_channel_parameter_mismatch.root.channels.userUpdates.address",
            line = 7,
        )
    }

    @Test
    fun `channel definition with unused parameter triggers warning`() {
        val document = parse("validator/channels/asyncapi_validator_channel_unused_parameter.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertFalse(validationResults.hasErrors(), "Should not have errors for unused parameter (only warning).")
        assertTrue(validationResults.hasWarnings(), "Should have warnings.")

        val warnings = validationResults.warnings.map { it.message }
        assertEquals(1, warnings.size, "Expected 1 warning for unused parameter.")
        assertFinding(
            validationResults,
            severity = WARNING,
            messageContains = "defines parameters [userId]",
            sourceFile = "asyncapi_validator_channel_unused_parameter.yaml",
            path = "asyncapi_validator_channel_unused_parameter.root.channels.userUpdates.parameters",
            line = 12,
        )
    }

    @Test
    fun `channel with ambiguous message references triggers warning`() {
        val document = parse("validator/channels/asyncapi_validator_channel_message_ambiguity.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertFalse(validationResults.hasErrors(), "Ambiguity should not be a hard error.")
        assertTrue(validationResults.hasWarnings(), "Should trigger a warning for ambiguous messages.")

        val warnings = validationResults.warnings.map { it.message }
        assertEquals(1, warnings.size, "Expected 1 warnings.")
        assertFinding(
            validationResults,
            severity = WARNING,
            messageContains = "contains ambiguous messages",
            sourceFile = "asyncapi_validator_channel_message_ambiguity.yaml",
            path = "asyncapi_validator_channel_message_ambiguity.root.channels.userUpdates.messages",
            line = 8,
        )
    }
}
