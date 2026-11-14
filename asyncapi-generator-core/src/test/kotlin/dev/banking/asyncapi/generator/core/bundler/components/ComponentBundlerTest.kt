package dev.banking.asyncapi.generator.core.bundler.components

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ComponentBundlerTest {

    private val bundler = ComponentBundler()

    @Test
    fun `bundleComponent traverses correlation IDs parameters and server variables`() {
        val correlationIdReference = Reference("#/components/correlationIds/user")
        val parameterReference = Reference("#/components/parameters/userId")
        val serverVariableReference = Reference("#/components/serverVariables/environment")
        val component = Component(
            correlationIds = mapOf(
                "user" to CorrelationIdInterface.CorrelationIdReference(correlationIdReference),
            ),
            parameters = mapOf(
                "userId" to ParameterInterface.ParameterReference(parameterReference),
            ),
            serverVariables = mapOf(
                "environment" to ServerVariableInterface.ServerVariableReference(serverVariableReference),
            ),
        )

        val bundled = bundler.bundleComponent(component, BundlingContext.empty())

        assertTrue(assertIs<CorrelationIdInterface.CorrelationIdReference>(bundled.correlationIds!!["user"]).reference.inline)
        assertTrue(assertIs<ParameterInterface.ParameterReference>(bundled.parameters!!["userId"]).reference.inline)
        assertTrue(
            assertIs<ServerVariableInterface.ServerVariableReference>(bundled.serverVariables!!["environment"]).reference.inline,
        )
        assertFalse(correlationIdReference.inline, "the input reference is not changed")
    }

    @Test
    fun `bundleComponents bundles and inlines an unvisited component reference`() {
        val schemaReference = Reference("#/components/schemas/User", model = Schema(type = "object"))
        val component = Component(
            schemas = mapOf("User" to SchemaInterface.SchemaReference(schemaReference)),
        )
        val componentReference = Reference("#/components", model = component)
        val componentInterface = ComponentInterface.ComponentReference(componentReference)

        val bundled = assertIs<ComponentInterface.ComponentReference>(
            bundler.bundleComponents(componentInterface, BundlingContext.empty()),
        )

        assertTrue(bundled.reference.inline)
        val bundledComponent = assertIs<Component>(bundled.reference.model)
        val bundledSchema = assertIs<SchemaInterface.SchemaReference>(bundledComponent.schemas!!["User"])
        assertFalse(bundledSchema.reference.inline, "a component schema reference keeps its \$ref")
        assertFalse(componentReference.inline, "the input reference is not changed")
    }
}
