package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.BINDING_PROTOCOL_TYPE
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * Validates the protocol bindings of a Binding Object and walks the schemas of its Kafka key.
 *
 * @param asyncApiContext the load the bindings belong to
 */
internal class BindingRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Binding> {
    private val kafkaBindingValidator = KafkaBindingValidator(asyncApiContext)

    override fun check(
        node: Binding,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        if (node.protocolBindings.isEmpty()) {
            node.kafkaKeySchema?.let { schema -> walk.schema(schema, path.child("Kafka key")) }
            return
        }

        node.protocolBindings.forEach { protocolBinding ->
            validateProtocol(protocolBinding, path, walk)
        }
    }

    private fun validateProtocol(
        binding: ProtocolBinding,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val properties = binding.content as? Map<*, *>
        if (properties == null || properties.keys.any { it !is String }) {
            walk.results.error(
                BINDING_PROTOCOL_TYPE,
                "$path '${binding.protocol}' binding must be an object with string property names.",
                sourceLocation = asyncApiContext.modelTracking.getSourceLocation(binding),
            )
            return
        }

        @Suppress("UNCHECKED_CAST")
        val stringProperties = properties as Map<String, Any?>
        if (binding.protocol == "kafka") {
            kafkaBindingValidator.validate(binding, stringProperties, path, walk)
        }
    }
}
