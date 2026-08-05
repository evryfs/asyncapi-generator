package dev.banking.asyncapi.generator.core.validator.servers

import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_CONTAINS_PROTOCOL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_HOST_REQUIRED
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_NAME_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_PROTOCOL_REQUIRED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_DEFAULT_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_ENUM_UNIQUE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_EXAMPLES_ENUM
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.SERVER_VARIABLE_UNDEFINED
import dev.banking.asyncapi.generator.core.validator.AbstractValidatorTest
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ServerValidatorTest : AbstractValidatorTest() {

    private val asyncApiValidator = AsyncApiValidator(asyncApiContext)

    @Test
    fun `invalid server definitions trigger errors and warnings`() {
        val document = parse("validator/servers/asyncapi_validator_server_invalid.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(2, results.errors.size, "Expected 2 validation errors.")
        assertEquals(3, results.warnings.size, "Expected 3 validation warnings.")

        val hostRequired = results.findings.single {
            it.code == SERVER_HOST_REQUIRED.code
        }
        assertEquals(SERVER_HOST_REQUIRED.code, hostRequired.code)
        assertEquals(SERVER_HOST_REQUIRED.severity, hostRequired.severity)
        assertEquals(SERVER_HOST_REQUIRED.concern, hostRequired.concern)
        assertEquals("asyncapi_validator_server_invalid.yaml", hostRequired.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_server_invalid.root.servers.emptyHostServer.host", hostRequired.path)
        assertEquals(7, hostRequired.line)

        val protocolRequired = results.findings.single {
            it.code == SERVER_PROTOCOL_REQUIRED.code
        }
        assertEquals(SERVER_PROTOCOL_REQUIRED.code, protocolRequired.code)
        assertEquals(SERVER_PROTOCOL_REQUIRED.severity, protocolRequired.severity)
        assertEquals(SERVER_PROTOCOL_REQUIRED.concern, protocolRequired.concern)
        assertEquals("asyncapi_validator_server_invalid.yaml", protocolRequired.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_server_invalid.root.servers.emptyProtocolServer.protocol",
            protocolRequired.path,
        )
        assertEquals(13, protocolRequired.line)

        val defaultEnum = results.findings.single {
            it.code == SERVER_VARIABLE_DEFAULT_ENUM.code
        }
        assertEquals(SERVER_VARIABLE_DEFAULT_ENUM.code, defaultEnum.code)
        assertEquals(SERVER_VARIABLE_DEFAULT_ENUM.severity, defaultEnum.severity)
        assertEquals(SERVER_VARIABLE_DEFAULT_ENUM.concern, defaultEnum.concern)
        assertEquals("asyncapi_validator_server_invalid.yaml", defaultEnum.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_server_invalid.root.servers.invalidVariableServer.variables.env.default",
            defaultEnum.path,
        )
        assertEquals(27, defaultEnum.line)

        val hostProtocol = results.findings.single {
            it.code == SERVER_HOST_CONTAINS_PROTOCOL.code
        }
        assertEquals(SERVER_HOST_CONTAINS_PROTOCOL.code, hostProtocol.code)
        assertEquals(SERVER_HOST_CONTAINS_PROTOCOL.severity, hostProtocol.severity)
        assertEquals(SERVER_HOST_CONTAINS_PROTOCOL.concern, hostProtocol.concern)
        assertEquals("asyncapi_validator_server_invalid.yaml", hostProtocol.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_server_invalid.root.servers.invalidHostServer.host",
            hostProtocol.path,
        )
        assertEquals(17, hostProtocol.line)

        val examplesEnum = results.findings.single {
            it.code == SERVER_VARIABLE_EXAMPLES_ENUM.code
        }
        assertEquals(SERVER_VARIABLE_EXAMPLES_ENUM.code, examplesEnum.code)
        assertEquals(SERVER_VARIABLE_EXAMPLES_ENUM.severity, examplesEnum.severity)
        assertEquals(SERVER_VARIABLE_EXAMPLES_ENUM.concern, examplesEnum.concern)
        assertEquals("asyncapi_validator_server_invalid.yaml", examplesEnum.sourceLocation?.file?.name)
        assertEquals(
            "asyncapi_validator_server_invalid.root.servers.invalidVariableServer.variables.env.examples",
            examplesEnum.path,
        )
        assertEquals(28, examplesEnum.line)
    }

    @Test
    fun `undefined server variables trigger an error`() {
        val document = parse("validator/servers/asyncapi_validator_server_variable_mismatch.yaml")
        val results = asyncApiValidator.validate(document)

        assertEquals(2, results.errors.size)

        val missingHostVariable = results.findings.single {
            it.code == SERVER_VARIABLE_UNDEFINED.code &&
                it.path ==
                "asyncapi_validator_server_variable_mismatch.root.servers.missingVariableDefServer.host"
        }
        assertEquals(SERVER_VARIABLE_UNDEFINED.code, missingHostVariable.code)
        assertEquals(SERVER_VARIABLE_UNDEFINED.severity, missingHostVariable.severity)
        assertEquals(SERVER_VARIABLE_UNDEFINED.concern, missingHostVariable.concern)
        assertEquals(8, missingHostVariable.line)
        assertEquals("asyncapi_validator_server_variable_mismatch.yaml", missingHostVariable.sourceLocation?.file?.name)

        val missingPathnameVariable = results.findings.single {
            it.code == SERVER_VARIABLE_UNDEFINED.code &&
                it.path ==
                "asyncapi_validator_server_variable_mismatch.root.servers.missingPathnameVariableDefServer.pathname"
        }
        assertEquals(SERVER_VARIABLE_UNDEFINED.code, missingPathnameVariable.code)
        assertEquals(SERVER_VARIABLE_UNDEFINED.severity, missingPathnameVariable.severity)
        assertEquals(SERVER_VARIABLE_UNDEFINED.concern, missingPathnameVariable.concern)
        assertEquals(14, missingPathnameVariable.line)
        assertEquals(
            "asyncapi_validator_server_variable_mismatch.yaml",
            missingPathnameVariable.sourceLocation?.file?.name,
        )
    }

    @Test
    fun `server variables may omit defaults`() {
        val results = validate("validator/servers/asyncapi_validator_server_optional_default.yaml")

        assertEquals(emptyList(), results.findings)
    }

    @Test
    fun `server variable enum values must be unique`() {
        val results = validate("validator/servers/asyncapi_validator_server_variable_duplicate_enum.yaml")

        assertEquals(1, results.errors.size)
        val duplicateEnum = results.findings.single {
            it.code == SERVER_VARIABLE_ENUM_UNIQUE.code
        }
        assertEquals(SERVER_VARIABLE_ENUM_UNIQUE.code, duplicateEnum.code)
        assertEquals(SERVER_VARIABLE_ENUM_UNIQUE.severity, duplicateEnum.severity)
        assertEquals(SERVER_VARIABLE_ENUM_UNIQUE.concern, duplicateEnum.concern)
        assertEquals(
            "asyncapi_validator_server_variable_duplicate_enum.yaml",
            duplicateEnum.sourceLocation?.file?.name,
        )
        assertEquals(
            "asyncapi_validator_server_variable_duplicate_enum.root.servers.production.variables.environment.enum",
            duplicateEnum.path,
        )
        assertEquals(11, duplicateEnum.line)
    }

    @Test
    fun `server names use the specification key format`() {
        val results = validate("validator/servers/asyncapi_validator_server_name_invalid.yaml")

        assertEquals(1, results.errors.size)
        val invalidName = results.findings.single {
            it.code == SERVER_NAME_FORMAT.code
        }
        assertEquals(SERVER_NAME_FORMAT.code, invalidName.code)
        assertEquals(SERVER_NAME_FORMAT.severity, invalidName.severity)
        assertEquals(SERVER_NAME_FORMAT.concern, invalidName.concern)
        assertEquals("asyncapi_validator_server_name_invalid.yaml", invalidName.sourceLocation?.file?.name)
        assertEquals("asyncapi_validator_server_name_invalid.root.servers[\"invalid.name\"]", invalidName.path)
        assertEquals(6, invalidName.line)
    }

    @Test
    fun `recognizes server variables used in pathname`() {
        val server = Server(
            host = "api.example.com",
            pathname = "/{environment}",
            protocol = "https",
            variables = mapOf(
                "environment" to ServerVariableInterface.ServerVariableInline(
                    ServerVariable(),
                ),
            ),
        )
        val collector = ValidationCollector()

        ServerValidator(asyncApiContext).validateInterface(
            ServerInterface.ServerInline(server),
            "Server 'production'",
            collector,
        )

        assertEquals(emptyList(), collector.report().findings)
    }
}
