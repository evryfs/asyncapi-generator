package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.ERROR
import dev.banking.asyncapi.generator.core.model.validator.ValidationSeverity.WARNING
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServerValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid server definitions trigger errors and warnings`() {
        val document = parse("validator/servers/asyncapi_validator_server_invalid.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(3, exception.errors.size, "Expected 3 validation errors.")

        assertTrue(results.hasWarnings(), "Should have warnings.")
        val warnings = results.warnings
        assertEquals(1, warnings.size, "Expected 1 validation warning.")
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "must define the 'protocol'",
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.emptyProtocolServer.protocol",
            line = 9,
        )
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "'default' ('staging') is not one of the allowed enum values",
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.invalidVariableServer.variables.env.default",
            line = 23,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "includes scheme/protocol",
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.invalidHostServer.host",
            line = 13,
        )
    }

    @Test
    fun `server variable mismatches trigger errors and warnings`() {
        val document = parse("validator/servers/asyncapi_validator_server_variable_mismatch.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for missing variable definition.")

        assertTrue(results.hasWarnings(), "Should have warnings.")
        val warnings = results.warnings
        assertEquals(1, warnings.size, "Expected 1 warning for missing variable definition.")
        assertFinding(
            results,
            severity = ERROR,
            messageContains = "host uses variables [region]",
            sourceFile = "asyncapi_validator_server_variable_mismatch.yaml",
            path = "asyncapi_validator_server_variable_mismatch.root.servers.missingVariableDefServer.host",
            line = 8,
        )
        assertFinding(
            results,
            severity = WARNING,
            messageContains = "defines variables [region]",
            sourceFile = "asyncapi_validator_server_variable_mismatch.yaml",
            path = "asyncapi_validator_server_variable_mismatch.root.servers.unusedVariableServer.variables",
            line = 16,
        )
    }
}
