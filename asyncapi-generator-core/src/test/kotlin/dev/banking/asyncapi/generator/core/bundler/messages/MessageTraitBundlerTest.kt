package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MessageTraitBundlerTest {

    private val bundler = MessageTraitBundler()

    @Test
    fun `bundle bundles and inlines an unvisited message trait reference`() {
        val bindingReference = Reference(
            "#/components/messageBindings/kafka",
            model = Binding(content = emptyMap()),
        )
        val trait = MessageTrait(
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val traitReference = Reference("#/components/messageTraits/audit", model = trait)
        val traitInterface = MessageTraitInterface.ReferenceMessageTrait(traitReference)

        val bundled = bundler.bundle(traitInterface, BundlingContext.empty())

        assertSame(traitInterface, bundled)
        assertTrue(traitReference.inline)
        assertIs<MessageTrait>(traitReference.model)
        assertTrue((traitReference.model as MessageTrait).bindings!!.containsKey("kafka"))
        assertTrue(bindingReference.inline)
    }
}
