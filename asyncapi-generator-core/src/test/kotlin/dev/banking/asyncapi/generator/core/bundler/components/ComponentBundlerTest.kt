package dev.banking.asyncapi.generator.core.bundler.components

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ComponentBundlerTest {
    private val walk = BundlingWalk()

    @Test
    fun `bundleComponent traverses correlation IDs parameters and server variables`() {
        val correlationIdReference =
            Reference("#/components/correlationIds/user", model = CorrelationId(location = "\$message.header#/id"))
        val parameterReference = Reference("#/components/parameters/userId", model = Parameter())
        val serverVariableReference = Reference("#/components/serverVariables/environment", model = ServerVariable())
        val component =
            Component(
                correlationIds =
                    mapOf(
                        "user" to CorrelationIdInterface.CorrelationIdReference(correlationIdReference),
                    ),
                parameters =
                    mapOf(
                        "userId" to ParameterInterface.ParameterReference(parameterReference),
                    ),
                serverVariables =
                    mapOf(
                        "environment" to ServerVariableInterface.ServerVariableReference(serverVariableReference),
                    ),
            )

        val bundled = walk.components.bundle(component, BundlingRun().rootScope())

        assertTrue(
            assertIs<CorrelationIdInterface.CorrelationIdReference>(bundled.correlationIds!!["user"]).reference.inline,
        )
        assertTrue(assertIs<ParameterInterface.ParameterReference>(bundled.parameters!!["userId"]).reference.inline)
        assertTrue(
            assertIs<ServerVariableInterface.ServerVariableReference>(
                bundled.serverVariables!!["environment"],
            ).reference.inline,
        )
        assertFalse(correlationIdReference.inline, "the input reference is not changed")
    }
}
