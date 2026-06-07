package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MessageTraitBundlerTest {

    private val bundler = MessageTraitBundler()

    @Test
    fun `bundle bundles and inlines an unvisited message trait reference`() {
        val bindingReference = Reference("#/components/messageBindings/kafka")
        val trait = MessageTrait(
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val traitReference = Reference("#/components/messageTraits/audit", model = trait)
        val traitInterface = MessageTraitInterface.ReferenceMessageTrait(traitReference)

        val bundled = bundler.bundle(traitInterface, BundlingContext.empty())

        assertThat(bundled).isSameAs(traitInterface)
        assertThat(traitReference.inline).isTrue()
        assertThat(traitReference.model).isInstanceOf(MessageTrait::class.java)
        assertThat((traitReference.model as MessageTrait).bindings).containsKey("kafka")
        assertThat(bindingReference.inline).isTrue()
    }

    @Test
    fun `bundle keeps a visited message trait reference unchanged`() {
        val trait = MessageTrait(name = "audit")
        val traitReference = Reference("#/components/messageTraits/audit", model = trait)
        val traitInterface = MessageTraitInterface.ReferenceMessageTrait(traitReference)

        val bundled = bundler.bundle(traitInterface, BundlingContext.empty().enter(traitReference))

        assertThat(bundled).isSameAs(traitInterface)
        assertThat(traitReference.inline).isFalse()
        assertThat(traitReference.model).isSameAs(trait)
    }
}
