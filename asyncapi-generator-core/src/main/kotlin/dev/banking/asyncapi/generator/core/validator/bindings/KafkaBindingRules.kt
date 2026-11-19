package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk

/**
 * The rules of Kafka bindings, by the Kafka binding specification versions 0.4.0 and 0.5.0.
 *
 * @param asyncApiContext the load whose models are checked
 */
internal class KafkaBindingRules(
    private val asyncApiContext: AsyncApiContext,
) : ProtocolBindingRules {
    override val protocol: String = "kafka"

    override fun check(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val fields = KafkaBindingFields(asyncApiContext, binding, properties, path, walk.results)
        val version = KafkaBindingVersion.resolve(binding, fields)
        binding.schemaFields.forEach { (fieldName, schema) ->
            walk.schema(schema, path.named("Kafka binding", fieldName))
        }

        when (binding.location) {
            BindingLocation.SERVER -> KafkaServerBindingRules.check(fields)
            BindingLocation.CHANNEL -> KafkaChannelBindingRules.check(fields, version)
            BindingLocation.OPERATION -> KafkaOperationBindingRules.check(fields)
            BindingLocation.MESSAGE -> KafkaMessageBindingRules.check(fields)
            BindingLocation.SCHEMA,
            BindingLocation.UNKNOWN,
            -> Unit
        }
    }
}
