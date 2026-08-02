package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiValidateException
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_CONTAINS_PROTOCOL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_PROTOCOL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_EMPTY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_UNDEFINED
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidationProfile
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
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
        assertEquals(2, exception.errors.size, "Expected 2 validation errors.")

        assertTrue(results.hasWarnings(), "Should have warnings.")
        val warnings = results.warnings
        assertEquals(4, warnings.size, "Expected 4 validation warnings.")
        assertRule(
            results,
            rule = SERVER_HOST_REQUIRED,
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.emptyHostServer.host",
            line = 7,
        )
        assertRule(
            results,
            rule = SERVER_PROTOCOL_REQUIRED,
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.emptyProtocolServer.protocol",
            line = 13,
        )
        assertRule(
            results,
            rule = SERVER_VARIABLE_DEFAULT_ENUM,
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.invalidVariableServer.variables.env.default",
            line = 27,
        )
        assertRule(
            results,
            rule = SERVER_HOST_CONTAINS_PROTOCOL,
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.invalidHostServer.host",
            line = 17,
        )
        assertRule(
            results,
            rule = SERVER_VARIABLE_EXAMPLES_ENUM,
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.invalidVariableServer.variables.env.examples",
            line = 28,
        )
        assertRule(
            results,
            rule = SERVER_VARIABLE_EXAMPLES_EMPTY,
            sourceFile = "asyncapi_validator_server_invalid.yaml",
            path = "asyncapi_validator_server_invalid.root.servers.missingDefaultVariableServer.variables.port.examples",
            line = 37,
        )
    }

    @Test
    fun `undefined server variables trigger an error`() {
        val document = parse("validator/servers/asyncapi_validator_server_variable_mismatch.yaml")
        val results = asyncApiValidator.validate(document)
        val exception = assertFailsWith<AsyncApiValidateException.ValidateError> {
            throwErrors(results)
        }
        assertEquals(1, exception.errors.size, "Expected 1 error for missing variable definition.")

        assertEquals(1, results.findings.size)
        assertRule(
            results,
            rule = SERVER_VARIABLE_UNDEFINED,
            sourceFile = "asyncapi_validator_server_variable_mismatch.yaml",
            path = "asyncapi_validator_server_variable_mismatch.root.servers.missingVariableDefServer.host",
            line = 8,
        )
    }

    @Test
    fun `server variables may omit defaults`() {
        val results = validate("validator/servers/asyncapi_validator_server_optional_default.yaml")

        assertNoFindings(results)
    }

    @Test
    fun `server variable enum values must be unique`() {
        val results = validate("validator/servers/asyncapi_validator_server_variable_duplicate_enum.yaml")

        assertEquals(1, results.errors.size)
        assertRule(
            results,
            rule = SERVER_VARIABLE_ENUM_UNIQUE,
            sourceFile = "asyncapi_validator_server_variable_duplicate_enum.yaml",
            path = "asyncapi_validator_server_variable_duplicate_enum.root.servers.production.variables.environment.enum",
            line = 11,
        )
    }

    @Test
    fun `server names use the specification key format`() {
        val results = validate("validator/servers/asyncapi_validator_server_name_invalid.yaml")

        assertEquals(1, results.errors.size)
        assertRule(
            results,
            rule = SERVER_NAME_FORMAT,
            sourceFile = "asyncapi_validator_server_name_invalid.yaml",
            path = "asyncapi_validator_server_name_invalid.root.servers.invalid.name",
            line = 6,
        )
    }

    @Test
    fun `recognizes server variables used in pathname`() {
        val server = Server(
            host = "api.example.com",
            pathName = "/{environment}",
            protocol = "https",
            variables = mapOf(
                "environment" to ServerVariableInterface.ServerVariableInline(ServerVariable()),
            ),
        )
        val collector = ValidationCollector(AsyncApiValidationProfile.V3_0)

        ServerValidator(asyncApiContext).validateInterface(
            ServerInterface.ServerInline(server),
            "Server 'production'",
            collector,
        )

        assertNoFindings(collector.report())
    }
}
