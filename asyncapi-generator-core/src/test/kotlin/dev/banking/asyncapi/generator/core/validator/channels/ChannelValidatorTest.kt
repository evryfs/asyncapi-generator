package dev.banking.asyncapi.generator.core.validator.channels

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_ADDRESS_QUERY_OR_FRAGMENT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNDEFINED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.CHANNEL_PARAMETER_UNUSED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChannelValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `channel address parameter missing definition throws validation error`() {
        val document = parse("validator/channels/asyncapi_validator_channel_parameter_mismatch.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertEquals(2, validationResults.errors.size)
        val missing = validationResults.findings.single { finding ->
            finding.code == CHANNEL_PARAMETER_UNDEFINED.code
        }
        assertEquals(
            "asyncapi_validator_channel_parameter_mismatch.yaml",
            missing.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_channel_parameter_mismatch.root.channels.userUpdates.address",
            missing.path,
        )
        assertEquals(7, missing.line)
        val unused = validationResults.findings.single { finding ->
            finding.code == CHANNEL_PARAMETER_UNUSED.code
        }
        assertEquals(CHANNEL_PARAMETER_UNUSED.severity, unused.severity)
        assertEquals(CHANNEL_PARAMETER_UNUSED.concern, unused.concern)
        assertEquals(
            "asyncapi_validator_channel_parameter_mismatch.yaml",
            unused.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_channel_parameter_mismatch.root.channels.userUpdates.parameters",
            unused.path,
        )
        assertEquals(12, unused.line)
    }

    @Test
    fun `channel definition with unused parameter triggers specification error`() {
        val document = parse("validator/channels/asyncapi_validator_channel_unused_parameter.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertEquals(1, validationResults.errors.size)
        val unused = validationResults.findings.single()
        assertEquals(CHANNEL_PARAMETER_UNUSED.code, unused.code)
        assertEquals(CHANNEL_PARAMETER_UNUSED.severity, unused.severity)
        assertEquals(CHANNEL_PARAMETER_UNUSED.concern, unused.concern)
        assertEquals(
            "asyncapi_validator_channel_unused_parameter.yaml",
            unused.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_channel_unused_parameter.root.channels.userUpdates.parameters",
            unused.path,
        )
        assertEquals(12, unused.line)
    }

    @Test
    fun `channels may omit messages and use an unknown address`() {
        val results = validate("validator/channels/asyncapi_validator_channel_optional_fields_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `empty channel collections are accepted`() {
        val results = validate("validator/channels/asyncapi_validator_channel_empty_collections_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `parameters property requires a channel address expression even when empty`() {
        val results = validate("validator/channels/asyncapi_validator_channel_parameters_without_address.yaml")

        assertEquals(1, results.errors.size)
        val unused = results.findings.single()
        assertEquals(CHANNEL_PARAMETER_UNUSED.code, unused.code)
        assertEquals(CHANNEL_PARAMETER_UNUSED.severity, unused.severity)
        assertEquals(CHANNEL_PARAMETER_UNUSED.concern, unused.concern)
        assertEquals(
            "asyncapi_validator_channel_parameters_without_address.yaml",
            unused.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_channel_parameters_without_address.root.channels.dynamic.parameters",
            unused.path,
        )
        assertEquals(7, unused.line)
    }

    @Test
    fun `channel addresses reject query and fragment suffixes`() {
        val results = validate("validator/channels/asyncapi_validator_channel_address_suffix.yaml")

        assertEquals(2, results.errors.size)
        val suffixErrors = results.findings.filter { it.code == CHANNEL_ADDRESS_QUERY_OR_FRAGMENT.code }
        assertEquals(2, suffixErrors.size)
        val queryError = suffixErrors.single {
            it.path == "asyncapi_validator_channel_address_suffix.root.channels.query.address"
        }
        assertEquals(
            "asyncapi_validator_channel_address_suffix.yaml",
            queryError.sourceLocation?.file?.name,
        )
        assertEquals("asyncapi_validator_channel_address_suffix.root.channels.query.address", queryError.path)
        assertEquals(7, queryError.line)
        val fragmentError = suffixErrors.single {
            it.path == "asyncapi_validator_channel_address_suffix.root.channels.fragment.address"
        }
        assertEquals(
            "asyncapi_validator_channel_address_suffix.yaml",
            fragmentError.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_channel_address_suffix.root.channels.fragment.address",
            fragmentError.path,
        )
        assertEquals(9, fragmentError.line)
    }
}
