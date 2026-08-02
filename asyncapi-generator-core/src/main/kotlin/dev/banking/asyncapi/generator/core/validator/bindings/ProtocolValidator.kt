package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

interface ProtocolValidator {

    fun validate(binding: ProtocolBinding, properties: Map<String, Any?>, results: ValidationCollector)
}
