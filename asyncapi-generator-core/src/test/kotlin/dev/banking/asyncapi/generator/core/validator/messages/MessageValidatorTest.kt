package dev.banking.asyncapi.generator.core.validator.messages

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_CONTENT_TYPE_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_CONTENT_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_FORMAT_UNVALIDATED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_EXAMPLE_SCHEMA_MISMATCH
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.MESSAGE_HEADER_FORMAT_UNSUPPORTED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_UNRESOLVED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MessageValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `validates message content type and example structure`() {
        val results = validate("validator/messages/asyncapi_validator_message_invalid.yaml")

        assertEquals(7, results.errors.size)
        val contentType = results.findings.single {
            it.code == MESSAGE_CONTENT_TYPE_FORMAT.code
        }
        assertEquals(MESSAGE_CONTENT_TYPE_FORMAT.severity, contentType.severity)
        assertEquals(MESSAGE_CONTENT_TYPE_FORMAT.concern, contentType.concern)
        assertEquals(
            "asyncapi_validator_message_invalid.yaml",
            contentType.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_message_invalid.root.components.messages.InvalidContentType.contentType",
            contentType.path,
        )
        assertEquals(8, contentType.line)

        val missingExample = results.findings.single {
            it.code == MESSAGE_EXAMPLE_CONTENT_REQUIRED.code
        }
        assertEquals(MESSAGE_EXAMPLE_CONTENT_REQUIRED.severity, missingExample.severity)
        assertEquals(MESSAGE_EXAMPLE_CONTENT_REQUIRED.concern, missingExample.concern)
        assertEquals(
            "asyncapi_validator_message_invalid.yaml",
            missingExample.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_message_invalid.root.components.messages.InvalidExample.examples[0]",
            missingExample.path,
        )
        assertEquals(11, missingExample.line)

        val exampleSchemaMismatch = results.findings.filter {
            it.code == MESSAGE_EXAMPLE_SCHEMA_MISMATCH.code
        }
        assertEquals(5, exampleSchemaMismatch.size)
        assertEquals(
            "asyncapi_validator_message_invalid.root.components.messages.InvalidSchemaExample.examples[0].headers.traceId",
            exampleSchemaMismatch[0].path,
        )
        assertEquals(39, exampleSchemaMismatch[0].line)
        assertEquals(
            "asyncapi_validator_message_invalid.root.components.messages.InvalidSchemaExample.examples[0].payload.id",
            exampleSchemaMismatch[1].path,
        )
        assertEquals(41, exampleSchemaMismatch[1].line)
        assertEquals(
            "asyncapi_validator_message_invalid.root.components.messages.InvalidSchemaExample.examples[0].payload.profile",
            exampleSchemaMismatch[2].path,
        )
        assertEquals(42, exampleSchemaMismatch[2].line)
        assertEquals(
            "asyncapi_validator_message_invalid.root.components.messages.InvalidSchemaExample.examples[0].payload.tags[1]",
            exampleSchemaMismatch[3].path,
        )
        assertEquals(43, exampleSchemaMismatch[3].line)
        assertEquals(
            "asyncapi_validator_message_invalid.root.components.messages.RejectedByBooleanSchema.examples[0].payload",
            exampleSchemaMismatch[4].path,
        )
        assertEquals(47, exampleSchemaMismatch[4].line)
    }

    @Test
    fun `accepts specific content types and examples containing headers or an explicit null payload`() {
        val results = validate("validator/messages/asyncapi_validator_message_valid.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `reports Multi Format Schema examples as an explicit validation limitation`() {
        val results = validate("validator/messages/asyncapi_validator_message_format_example.yaml")

        assertEquals(1, results.warnings.size)
        val warning = results.findings.single()
        assertEquals(MESSAGE_EXAMPLE_FORMAT_UNVALIDATED.code, warning.code)
        assertEquals(MESSAGE_EXAMPLE_FORMAT_UNVALIDATED.severity, warning.severity)
        assertEquals(MESSAGE_EXAMPLE_FORMAT_UNVALIDATED.concern, warning.concern)
        assertEquals(
            "asyncapi_validator_message_format_example.yaml",
            warning.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_message_format_example.root.components.messages.AvroPayload.examples[0].payload",
            warning.path,
        )
        assertEquals(17, warning.line)
    }

    @Test
    fun `reports Multi Format Schema message headers as an explicit validation limitation`() {
        val results = validate("validator/messages/asyncapi_validator_message_header_format.yaml")

        assertEquals(1, results.warnings.size)
        val warning = results.findings.single()
        assertEquals(MESSAGE_HEADER_FORMAT_UNSUPPORTED.code, warning.code)
        assertEquals(MESSAGE_HEADER_FORMAT_UNSUPPORTED.severity, warning.severity)
        assertEquals(MESSAGE_HEADER_FORMAT_UNSUPPORTED.concern, warning.concern)
        assertEquals(
            "asyncapi_validator_message_header_format.yaml",
            warning.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_message_header_format.root.components.messages.AvroHeader.headers",
            warning.path,
        )
        assertEquals(8, warning.line)
    }

    @Test
    fun `message headers ref to component schema passes validation`() {
        val document = parse("validator/messages/asyncapi_validator_message_headers_ref_valid.yaml")
        val results = asyncApiValidator.validate(document)

        assertFalse(results.hasErrors())
        assertFalse(results.hasWarnings())
    }

    @Test
    fun `message headers broken ref triggers validation error`() {
        val document = parse("validator/messages/asyncapi_validator_message_headers_ref_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(1, results.errors.size)
        val unresolvedRef = results.findings.single()
        assertEquals(REFERENCE_UNRESOLVED.code, unresolvedRef.code)
        assertEquals(REFERENCE_UNRESOLVED.severity, unresolvedRef.severity)
        assertEquals(REFERENCE_UNRESOLVED.concern, unresolvedRef.concern)
    }
}
