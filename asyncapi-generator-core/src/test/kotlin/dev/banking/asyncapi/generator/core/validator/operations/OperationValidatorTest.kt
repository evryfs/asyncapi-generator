package dev.banking.asyncapi.generator.core.validator.operations

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_ACTION_VALUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_TARGET
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_CHANNEL_REFERENCE_SCOPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_ADDRESS_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_ADDRESS
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_CHANNEL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.OPERATION_REPLY_MESSAGE_REFERENCE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_UNRESOLVED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OperationValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `accepts root and component operations that reference messages through their channels`() {
        val results = validate("validator/operations/asyncapi_validator_operation_boundaries_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `enforces operation and reply ownership and message subsets`() {
        val results = validate("validator/operations/asyncapi_validator_operation_boundaries_invalid.yaml")

        assertEquals(8, results.errors.size)
        assertEquals(0, results.warnings.size)

        val scope = results.findings.single { it.code == OPERATION_CHANNEL_REFERENCE_SCOPE.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            scope.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidRootChannelScope.channel",
            scope.path,
        )
        assertEquals(33, scope.line)

        val messageSubset = results.findings.single { it.code == OPERATION_MESSAGE_REFERENCE.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            messageSubset.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidMessageSubset.messages[0]",
            messageSubset.path,
        )
        assertEquals(40, messageSubset.line)

        val replyChannelScope = results.findings.single { it.code == OPERATION_REPLY_CHANNEL_REFERENCE.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            replyChannelScope.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyChannelScope.reply.channel",
            replyChannelScope.path,
        )
        assertEquals(46, replyChannelScope.line)

        val replyMessageSubset =
            results.findings.single { it.code == OPERATION_REPLY_MESSAGE_REFERENCE.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            replyMessageSubset.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyMessageSubset.reply.messages[0]",
            replyMessageSubset.path,
        )
        assertEquals(56, replyMessageSubset.line)

        val replyChannelAddress = results.findings.single { it.code == OPERATION_REPLY_CHANNEL_ADDRESS.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            replyChannelAddress.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyChannelAddress.reply.channel",
            replyChannelAddress.path,
        )
        assertEquals(64, replyChannelAddress.line)

        val missingReplyChannel = results.findings.single { it.code == OPERATION_REPLY_CHANNEL_REQUIRED.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            missingReplyChannel.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.MissingReplyChannel.reply.messages",
            missingReplyChannel.path,
        )
        assertEquals(71, missingReplyChannel.line)

        val replyAddressFormat = results.findings.single { it.code == OPERATION_REPLY_ADDRESS_FORMAT.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            replyAddressFormat.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.InvalidReplyAddress.reply.address.location",
            replyAddressFormat.path,
        )
        assertEquals(79, replyAddressFormat.line)

        val missingReplyAddressLocation =
            results.findings.single { it.code == OPERATION_REPLY_ADDRESS_REQUIRED.code }
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.yaml",
            missingReplyAddressLocation.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operation_boundaries_invalid.root.operations.MissingReplyAddressLocation.reply.address.location",
            missingReplyAddressLocation.path,
        )
        assertEquals(92, missingReplyAddressLocation.line)
    }

    @Test
    fun `validation fails for operation with invalid action`() {
        val document = parse("validator/operations/asyncapi_validator_operations_invalid_action.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertEquals(2, validationResults.errors.size, "Expected invalid and empty action errors.")
        val value = validationResults.findings.single { it.code == OPERATION_ACTION_VALUE.code }
        assertEquals(
            "asyncapi_validator_operations_invalid_action.yaml",
            value.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operations_invalid_action.root.operations.testOperation.action",
            value.path,
        )
        assertEquals(18, value.line)

        val required = validationResults.findings.single { it.code == OPERATION_ACTION_REQUIRED.code }
        assertEquals(
            "asyncapi_validator_operations_invalid_action.yaml",
            required.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operations_invalid_action.root.operations.emptyAction.action",
            required.path,
        )
        assertEquals(24, required.line)
    }

    @Test
    fun `validation fails for operation with broken channel reference`() {
        val document = parse("validator/operations/asyncapi_validator_operations_broken_channel_ref.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertEquals(1, validationResults.errors.size, "Expected 1 error for broken channel reference.")
        val unresolved = validationResults.findings.single()
        assertEquals(REFERENCE_UNRESOLVED.code, unresolved.code)
        assertEquals(REFERENCE_UNRESOLVED.severity, unresolved.severity)
        assertEquals(REFERENCE_UNRESOLVED.concern, unresolved.concern)
        assertEquals(
            "asyncapi_validator_operations_broken_channel_ref.yaml",
            unresolved.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operations_broken_channel_ref.root.operations.testOperation.channel",
            unresolved.path,
        )
        assertEquals(9, unresolved.line)
    }

    @Test
    fun `validation fails for operation channel reference type mismatch`() {
        val document = parse("validator/operations/asyncapi_validator_operations_channel_ref_type_mismatch.yaml")
        val validationResults = asyncApiValidator.validate(document)

        assertEquals(1, validationResults.errors.size, "Expected 1 error: channel type mismatch.")
        val channelTarget = validationResults.findings.single()
        assertEquals(OPERATION_CHANNEL_TARGET.code, channelTarget.code)
        assertEquals(OPERATION_CHANNEL_TARGET.severity, channelTarget.severity)
        assertEquals(OPERATION_CHANNEL_TARGET.concern, channelTarget.concern)
        assertEquals(
            "asyncapi_validator_operations_channel_ref_type_mismatch.yaml",
            channelTarget.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_operations_channel_ref_type_mismatch.root.operations.testOperation.channel",
            channelTarget.path,
        )
        assertEquals(18, channelTarget.line)
    }
}
