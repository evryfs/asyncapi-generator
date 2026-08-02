package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

class KafkaBindingValidator(val asyncApiContext: AsyncApiContext) : ProtocolValidator {

    override fun validate(protocol: String, bindingData: Map<String, Any?>, binding: Binding, results: ValidationCollector) {
        // Future: Add strict Kafka checks here (e.g. check if 'topic' exists)

        // Delegate to generic property validation to maintain current behavior (warnings on nulls/lists)
        bindingData.forEach { (key, value) ->
            BindingValidator.validateGenericProperty(asyncApiContext, protocol, key, value, binding, results)
        }
    }
}
