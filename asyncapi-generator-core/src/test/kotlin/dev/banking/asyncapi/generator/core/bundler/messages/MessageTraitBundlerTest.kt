package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MessageTraitBundlerTest {
    private val walk = BundlingWalk()

    @Test
    fun `bundle bundles and inlines an unvisited message trait reference`() {
        val bindingReference =
            Reference(
                "#/components/messageBindings/kafka",
                model = Binding(content = emptyMap()),
            )
        val trait =
            MessageTrait(
                bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
            )
        val traitReference = Reference("#/components/messageTraits/audit", model = trait)
        val traitInterface = MessageTraitInterface.MessageTraitReference(traitReference)

        val bundled =
            assertIs<MessageTraitInterface.MessageTraitReference>(
                walk.member(traitInterface, walk.messageTrait, BundlingRun().rootScope()),
            )

        assertTrue(bundled.reference.inline)
        val bundledTrait = assertIs<MessageTrait>(bundled.reference.model)
        assertTrue(assertIs<BindingInterface.BindingReference>(bundledTrait.bindings!!["kafka"]).reference.inline)
        assertFalse(traitReference.inline, "the input reference is not changed")
        assertFalse(bindingReference.inline, "the input reference is not changed")
    }
}
