package com.tietoevry.banking.asyncapi.generator.core.validator.bindings

import com.tietoevry.banking.asyncapi.generator.core.model.bindings.Binding
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults

interface ProtocolValidator {

    fun validate(protocol: String, bindingData: Map<String, Any?>, binding: Binding, results: ValidationResults)
}
