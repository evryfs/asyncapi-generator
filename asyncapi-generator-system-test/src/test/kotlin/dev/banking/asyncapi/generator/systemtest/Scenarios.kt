package dev.banking.asyncapi.generator.systemtest

/** Every scenario exercised end to end by [ScenarioSystemTest]. */
internal val scenarios: List<Scenario> = listOf(
    Scenario(
        name = "spring-kafka-contract",
        config = GenerationConfig(
            generatorName = "kotlin",
            modelPackage = "com.example.contract.model",
            clientPackage = "com.example.contract.client",
            client = ClientConfig(
                producerAdditionalPayloadTypes = listOf("byte-array", "string"),
                topicParameterProperties = mapOf("environment" to "kafka.environment"),
                clientContractValidationAnnotation = "org.springframework.validation.annotation.Validated",
                payloadParameterValidationAnnotation = "jakarta.validation.Valid",
            ),
        ),
    ),
)
