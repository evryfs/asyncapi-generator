package com.tietoevry.banking.asyncapi.generator.core.validator.bindings

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.Binding
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults

class KafkaBindingValidator(val asyncApiContext: AsyncApiContext) : ProtocolValidator {

    override fun validate(protocol: String, bindingData: Map<String, Any?>, binding: Binding, results: ValidationResults) {
        // Future: Add strict Kafka checks here (e.g. check if 'topic' exists)

        // Delegate to generic property validation to maintain current behavior (warnings on nulls/lists)
        bindingData.forEach { (key, value) ->
            BindingValidator.validateGenericProperty(asyncApiContext, protocol, key, value, binding, results)
        }
    }
}
