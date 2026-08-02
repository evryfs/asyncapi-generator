package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OperationTraitBundlerTest {

    private val bundler = OperationTraitBundler()

    @Test
    fun `bundle bundles and inlines an unvisited operation trait reference`() {
        val bindingReference = Reference("#/components/operationBindings/kafka")
        val trait = OperationTrait(
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val traitReference = Reference("#/components/operationTraits/audit", model = trait)
        val traitInterface = OperationTraitInterface.OperationTraitReference(traitReference)

        val bundled = bundler.bundle(traitInterface, BundlingContext.empty())

        assertThat(bundled).isSameAs(traitInterface)
        assertThat(traitReference.inline).isTrue()
        assertThat(traitReference.model).isInstanceOf(OperationTrait::class.java)
        assertThat((traitReference.model as OperationTrait).bindings).containsKey("kafka")
        assertThat(bindingReference.inline).isTrue()
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

        assertThat(bundled).isInstanceOf(OperationTraitInterface.OperationTraitInline::class.java)
        val bundledTrait = (bundled as OperationTraitInterface.OperationTraitInline).operationTrait
        assertThat(bundledTrait.security).hasSize(1)
        assertThat(securityReference.inline).isTrue()
    }

    @Test
    fun `bundle keeps a visited operation trait reference unchanged`() {
        val trait = OperationTrait(title = "Audit")
        val traitReference = Reference("#/components/operationTraits/audit", model = trait)
        val traitInterface = OperationTraitInterface.OperationTraitReference(traitReference)

        val bundled = bundler.bundle(traitInterface, BundlingContext.empty().enter(traitReference))

        assertThat(bundled).isSameAs(traitInterface)
        assertThat(traitReference.inline).isFalse()
        assertThat(traitReference.model).isSameAs(trait)
    }
}
