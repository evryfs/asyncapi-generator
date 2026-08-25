package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OperationTraitBundlerTest {

    private val bundler = OperationTraitBundler()

    @Test
    fun `bundle bundles and inlines an unvisited operation trait reference`() {
        val bindingReference = Reference(
            "#/components/operationBindings/kafka",
            model = Binding(content = emptyMap()),
        )
        val trait = OperationTrait(
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val traitReference = Reference("#/components/operationTraits/audit", model = trait)
        val traitInterface = OperationTraitInterface.OperationTraitReference(traitReference)

        val bundled = bundler.bundle(traitInterface, BundlingContext.empty())

        assertSame(traitInterface, bundled)
        assertTrue(traitReference.inline)
        assertIs<OperationTrait>(traitReference.model)
        assertTrue((traitReference.model as OperationTrait).bindings!!.containsKey("kafka"))
        assertTrue(bindingReference.inline)
    }

    @Test
    fun `bundle traverses an operation trait security list`() {
        val securityReference = Reference(
            "#/components/securitySchemes/userPassword",
            model = SecurityScheme(type = "userPassword"),
        )
        val traitInterface = OperationTraitInterface.OperationTraitInline(
            OperationTrait(
                security = listOf(SecuritySchemeInterface.SecuritySchemeReference(securityReference)),
            ),
        )

        val bundled = bundler.bundle(traitInterface, BundlingContext.empty())

        assertIs<OperationTraitInterface.OperationTraitInline>(bundled)
        val bundledTrait = (bundled as OperationTraitInterface.OperationTraitInline).operationTrait
        assertEquals(1, bundledTrait.security!!.size)
        assertTrue(securityReference.inline)
    }
}
