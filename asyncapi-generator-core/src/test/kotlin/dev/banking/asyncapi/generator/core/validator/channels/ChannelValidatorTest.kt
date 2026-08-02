package dev.banking.asyncapi.generator.core.validator.channels

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_ADDRESS_QUERY_OR_FRAGMENT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_MESSAGES_AMBIGUOUS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNDEFINED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNUSED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_SERVERS_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_BINDINGS_EMPTY
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
        assertEquals(2, exception.errors.size, "Expected missing and unused parameter errors.")
        assertRule(
            validationResults,
            rule = CHANNEL_PARAMETER_UNDEFINED,
            sourceFile = "asyncapi_validator_channel_parameter_mismatch.yaml",
            path = "asyncapi_validator_channel_parameter_mismatch.root.channels.userUpdates.address",
            line = 7,
        )
        assertRule(
            validationResults,
            rule = CHANNEL_PARAMETER_UNUSED,
            sourceFile = "asyncapi_validator_channel_parameter_mismatch.yaml",
            path = "asyncapi_validator_channel_parameter_mismatch.root.channels.userUpdates.parameters",
            line = 12,
        )
    }

    @Test
    fun `channel definition with unused parameter triggers specification error`() {
        val document = parse("validator/channels/asyncapi_validator_channel_unused_parameter.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertEquals(1, validationResults.errors.size)
        assertRule(
            validationResults,
            rule = CHANNEL_PARAMETER_UNUSED,
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
        assertRule(
            validationResults,
            rule = CHANNEL_MESSAGES_AMBIGUOUS,
            sourceFile = "asyncapi_validator_channel_message_ambiguity.yaml",
            path = "asyncapi_validator_channel_message_ambiguity.root.channels.userUpdates.messages",
            line = 8,
        )
    }

    @Test
    fun `channels may omit messages and use an unknown address`() {
        val results = validate("validator/channels/asyncapi_validator_channel_optional_fields_valid.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `empty channel servers and bindings produce distinct advisories`() {
        val results = validate("validator/channels/asyncapi_validator_channel_advisories.yaml")

        assertEquals(2, results.warnings.size)
        assertRule(results, CHANNEL_SERVERS_EMPTY, path = "asyncapi_validator_channel_advisories.root.channels.events.servers", line = 8)
        assertRule(results, CHANNEL_BINDINGS_EMPTY, path = "asyncapi_validator_channel_advisories.root.channels.events.bindings", line = 9)
    }

    @Test
    fun `parameters property requires a channel address expression even when empty`() {
        val results = validate("validator/channels/asyncapi_validator_channel_parameters_without_address.yaml")

        assertEquals(1, results.errors.size)
        assertRule(
            results,
            rule = CHANNEL_PARAMETER_UNUSED,
            sourceFile = "asyncapi_validator_channel_parameters_without_address.yaml",
            path = "asyncapi_validator_channel_parameters_without_address.root.channels.dynamic.parameters",
            line = 7,
        )
    }

    @Test
    fun `channel addresses reject query and fragment suffixes`() {
        val results = validate("validator/channels/asyncapi_validator_channel_address_suffix.yaml")

        assertEquals(2, results.errors.size)
        assertRule(
            results,
            rule = CHANNEL_ADDRESS_QUERY_OR_FRAGMENT,
            sourceFile = "asyncapi_validator_channel_address_suffix.yaml",
            path = "asyncapi_validator_channel_address_suffix.root.channels.query.address",
            line = 7,
        )
        assertRule(
            results,
            rule = CHANNEL_ADDRESS_QUERY_OR_FRAGMENT,
            sourceFile = "asyncapi_validator_channel_address_suffix.yaml",
            path = "asyncapi_validator_channel_address_suffix.root.channels.fragment.address",
            line = 9,
        )
    }
}
