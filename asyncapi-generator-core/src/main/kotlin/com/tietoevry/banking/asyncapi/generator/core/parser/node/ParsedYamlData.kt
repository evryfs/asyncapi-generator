package com.tietoevry.banking.asyncapi.generator.core.parser.node

data class ParsedYamlData(
    val data: Map<String, Any?>,
    val lineMappings: Map<String, Int>
)
