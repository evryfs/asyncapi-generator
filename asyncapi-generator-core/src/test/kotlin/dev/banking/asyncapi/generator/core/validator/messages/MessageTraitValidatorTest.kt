package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_CONTENT
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class MessageTraitValidatorTest : AbstractValidatorTest() {
    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid message traits trigger errors only`() {
        val document = parse("validator/messages/asyncapi_validator_messagetrait_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(2, results.errors.size)
        val contentType = results.findings.single { it.code == MESSAGE_CONTENT_TYPE.code }
        assertEquals(MESSAGE_CONTENT_TYPE.severity, contentType.severity)
        assertEquals(MESSAGE_CONTENT_TYPE.concern, contentType.concern)
        assertEquals("asyncapi_validator_messagetrait_invalid.yaml", contentType.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_messagetrait_invalid.root.components.messageTraits.InvalidContentType.contentType",
            contentType.path,
        )
        assertEquals(10, contentType.line)

        val example =
            results.findings.single {
                it.code == MESSAGE_EXAMPLE_CONTENT.code
            }
        assertEquals(MESSAGE_EXAMPLE_CONTENT.severity, example.severity)
        assertEquals(MESSAGE_EXAMPLE_CONTENT.concern, example.concern)
        assertEquals("asyncapi_validator_messagetrait_invalid.yaml", example.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_messagetrait_invalid.root.components.messageTraits.InvalidExample.examples[0]",
            example.path,
        )
        assertEquals(18, example.line)
    }

    @Test
    fun `accepts valid message traits including documentation-only traits and explicit null examples`() {
        val document = parse("validator/messages/asyncapi_validator_messagetrait_headers_ref_valid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `message trait headers broken ref triggers validation error`() {
        val document = parse("validator/messages/asyncapi_validator_messagetrait_headers_ref_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(1, results.errors.size)
    }
}
