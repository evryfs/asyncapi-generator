package dev.banking.asyncapi.generator.systemtest

private const val VALIDATED = "org.springframework.validation.annotation.Validated"
private const val VALID = "jakarta.validation.Valid"

/**
 * Every scenario exercised end to end by [ScenarioSystemTest].
 *
 * Set `-Dsystem-test.only=<name>,<name>` to run only some of them while developing.
 */
internal val scenarios: List<Scenario>
    get() {
        val selected = System.getProperty("system-test.only")?.split(",")?.map(String::trim)
        return allScenarios.filter { selected == null || it.name in selected }
    }

private val allScenarios: List<Scenario> = listOf(
    Scenario(
        name = "spring-kafka-contract",
        executions = listOf(
            Execution(
                id = "contract",
                generatorName = "kotlin",
                modelPackage = "com.example.contract.model",
                clientPackage = "com.example.contract.client",
            ),
        ),
        client = ClientConfig(
            producerAdditionalPayloadTypes = listOf("byte-array", "string"),
            topicParameterProperties = mapOf("environment" to "kafka.environment"),
            clientContractValidationAnnotation = VALIDATED,
            payloadParameterValidationAnnotation = VALID,
        ),
    ),
    Scenario(
        name = "bundling",
        executions = listOf(
            Execution(
                id = "channel-fragment",
                generatorName = "kotlin",
                inputSpec = "channel-fragment/main.yaml",
                outputFile = "bundles/channel-fragment.yaml",
            ),
            Execution(
                id = "schema-fragment",
                generatorName = "kotlin",
                inputSpec = "schema-fragment/main.yaml",
                outputFile = "bundles/schema-fragment.yaml",
            ),
            Execution(
                id = "separate-directory",
                generatorName = "kotlin",
                inputSpec = "separate-directory/main.yaml",
                outputFile = "bundles/separate-directory.yaml",
            ),
            Execution(
                id = "yaml-document",
                generatorName = "asyncapi-yaml",
                inputSpec = "single-file/asyncapi.yaml",
                outputFile = "bundles/single-file.yaml",
            ),
            Execution(
                id = "nested-output-file",
                generatorName = "kotlin",
                inputSpec = "single-file/asyncapi.yaml",
                outputFile = "bundles/nested/directories/single-file.yaml",
            ),
        ),
    ),
    Scenario(
        name = "external-references",
        executions = listOf(
            Execution(
                id = "reference-forms",
                generatorName = "kotlin",
                inputSpec = "reference-forms/asyncapi.yaml",
                modelPackage = "com.example.references.model",
                clientPackage = "com.example.references.client",
            ),
            Execution(
                id = "external-message",
                generatorName = "kotlin",
                inputSpec = "external-message/asyncapi.yaml",
                modelPackage = "com.example.message.model",
                clientPackage = "com.example.message.client",
            ),
            Execution(
                id = "message-models",
                generatorName = "kotlin",
                inputSpec = "message-models/main.yaml",
                outputFile = "bundles/message-models.yaml",
                modelPackage = "com.example.external.model",
                clientPackage = "com.example.external.client",
            ),
            Execution(
                id = "recursive-kotlin",
                generatorName = "kotlin",
                inputSpec = "recursive/asyncapi.yaml",
                outputFile = "bundles/recursive-kotlin.yaml",
                modelPackage = "com.example.recursive.kotlin",
            ),
            Execution(
                id = "recursive-java",
                generatorName = "java",
                inputSpec = "recursive/asyncapi.yaml",
                outputFile = "bundles/recursive-java.yaml",
                modelPackage = "com.example.recursive.java",
            ),
        ),
        client = ClientConfig(
            topicParameterProperties = mapOf("environment" to "kafka.environment"),
            clientContractValidationAnnotation = VALIDATED,
            payloadParameterValidationAnnotation = VALID,
        ),
    ),
    Scenario(
        name = "complete-configuration",
        executions = listOf(
            Execution(
                id = "kotlin-client",
                generatorName = "kotlin",
                outputDirectory = "generated/kotlin",
                outputFile = "bundles/asyncapi.yaml",
                modelPackage = "com.example.complete.kotlin.model",
                clientPackage = "com.example.complete.kotlin.client",
                modelAnnotation = "com.example.codegen.GeneratedPayload",
            ),
            Execution(
                id = "java-records",
                generatorName = "java",
                outputDirectory = "generated/java",
                modelPackage = "com.example.complete.java.model",
                modelType = "java-record",
                modelAnnotation = "com.example.codegen.GeneratedPayload",
            ),
            Execution(
                id = "native-avro",
                generatorName = "java",
                inputSpec = "native-avro.yaml",
                outputDirectory = "generated/avro",
                modelPackage = "com.example.complete.avro",
                modelType = "avro-specific-record",
            ),
            Execution(
                id = "native-protobuf",
                generatorName = "kotlin",
                inputSpec = "native-protobuf.yaml",
                outputDirectory = "generated/protobuf",
                modelPackage = "com.example.complete.protobuf",
                modelType = "protobuf-message",
            ),
            Execution(
                id = "avro-schema",
                generatorName = "avro-schema",
                outputDirectory = "generated/schemas/avro",
                schemaPackage = "com.example.complete.schema.avro",
            ),
            Execution(
                id = "protobuf-schema",
                generatorName = "protobuf-schema",
                inputSpec = "native-protobuf.yaml",
                outputDirectory = "generated/schemas/protobuf",
                schemaPackage = "com.example.complete.schema.protobuf",
            ),
            Execution(
                id = "json-schema",
                generatorName = "json-schema",
                outputDirectory = "generated/schemas/json",
                schemaPackage = "com.example.complete.schema.json",
            ),
            Execution(
                id = "asyncapi-yaml",
                generatorName = "asyncapi-yaml",
                outputFile = "documents/asyncapi.yaml",
            ),
            Execution(
                id = "asyncapi-json",
                generatorName = "asyncapi-json",
                outputFile = "documents/asyncapi.json",
            ),
        ),
        client = ClientConfig(
            topicParameterProperties = mapOf("environment" to "kafka.environment"),
            clientContractValidationAnnotation = VALIDATED,
            payloadParameterValidationAnnotation = VALID,
        ),
    ),
    Scenario(
        name = "output-locations",
        executions = listOf(
            Execution(
                id = "custom-sources",
                generatorName = "kotlin",
                outputDirectory = "custom/sources",
                modelPackage = "com.example.codegen",
            ),
            Execution(
                id = "custom-resources",
                generatorName = "avro-schema",
                outputDirectory = "custom/resources",
                schemaPackage = "com.example.avro",
            ),
            Execution(
                id = "contract-a",
                generatorName = "kotlin",
                inputSpec = "contract-a.yaml",
                modelPackage = "com.example.a.model",
                clientPackage = "com.example.a.client",
            ),
            Execution(
                id = "contract-b",
                generatorName = "avro-schema",
                inputSpec = "contract-b.yaml",
                schemaPackage = "com.example.b.schema",
            ),
        ),
        client = ClientConfig(),
    ),
    Scenario(
        name = "shared-client-config",
        executions = listOf(
            Execution(
                id = "client",
                generatorName = "kotlin",
                modelPackage = "com.example.shared.model",
                clientPackage = "com.example.shared.client",
            ),
            Execution(id = "models", generatorName = "kotlin", modelPackage = "com.example.models"),
            Execution(id = "schemas", generatorName = "avro-schema", schemaPackage = "com.example.schemas"),
        ),
        client = ClientConfig(),
        sharedClientConfig = true,
        expectedWarnings = listOf("unused-client-config"),
    ),
    Scenario(
        name = "suppressed-warnings",
        executions = listOf(
            Execution(
                id = "client",
                generatorName = "kotlin",
                modelPackage = "com.example.shared.model",
                clientPackage = "com.example.shared.client",
            ),
            Execution(
                id = "models",
                generatorName = "kotlin",
                modelPackage = "com.example.models",
                suppressedWarnings = listOf("unused-client-config"),
            ),
            Execution(
                id = "schemas",
                generatorName = "avro-schema",
                schemaPackage = "com.example.schemas",
                suppressedWarnings = listOf("unused-client-config"),
            ),
        ),
        client = ClientConfig(),
        sharedClientConfig = true,
        absentWarnings = listOf("unused-client-config"),
    ),
    Scenario(
        name = "generated-code",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.codegen.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.codegen.java"),
        ),
    ),
    Scenario(
        name = "avro-schemas",
        executions = listOf(
            Execution(id = "avro", generatorName = "avro-schema", schemaPackage = "com.example.avroschemas"),
        ),
    ),
    Scenario(
        name = "java-client",
        executions = listOf(
            Execution(
                id = "java-client",
                generatorName = "java",
                modelPackage = "com.example.java.model",
                clientPackage = "com.example.java.client",
            ),
            Execution(
                id = "java-protobuf",
                generatorName = "java",
                inputSpec = "native-protobuf.yaml",
                modelPackage = "com.example.java.protobuf",
                modelType = "protobuf-message",
            ),
        ),
        client = ClientConfig(),
    ),
    Scenario(
        name = "producer-only",
        executions = listOf(
            Execution(
                id = "producer",
                generatorName = "kotlin",
                modelPackage = "com.example.producer.model",
                clientPackage = "com.example.producer.client",
            ),
        ),
        client = ClientConfig(consumerEnabled = false),
    ),
    diagnostic(
        name = "missing-output",
        execution = Execution(id = "no-output", generatorName = "kotlin"),
        expected = ExpectedFailure(
            messages = listOf(
                "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
                    "schemaPackage with a schema generator, or outputFile.",
            ),
        ),
    ),
    diagnostic(
        name = "missing-input-spec",
        execution = Execution(id = "missing-input", generatorName = "kotlin", inputSpec = "missing.yaml"),
        expected = ExpectedFailure(
            messages = listOf("missing.yaml", "does not exist"),
            absent = listOf("No generator output is configured"),
        ),
    ),
    diagnostic(
        name = "unknown-generator",
        execution = Execution(id = "unknown-generator", generatorName = "cobol", modelPackage = "com.example.model"),
        expected = ExpectedFailure(messages = listOf("cobol", "asyncapi-json")),
    ),
    diagnostic(
        name = "unknown-model-type",
        execution = Execution(
            id = "unknown-model-type",
            generatorName = "kotlin",
            modelPackage = "com.example.model",
            modelType = "punch-card",
        ),
        expected = ExpectedFailure(messages = listOf("punch-card", "protobuf-message")),
    ),
    diagnostic(
        name = "document-without-output-file",
        execution = Execution(id = "document", generatorName = "asyncapi-yaml"),
        expected = ExpectedFailure(messages = listOf("outputFile is required when generatorName is asyncapi-yaml")),
    ),
    diagnostic(
        name = "unqualified-validation-annotation",
        execution = Execution(
            id = "unqualified-annotation",
            generatorName = "kotlin",
            modelPackage = "com.example.model",
            clientPackage = "com.example.client",
        ),
        client = ClientConfig(clientContractValidationAnnotation = "Validated"),
        expected = ExpectedFailure(
            messages = listOf("validationAnnotations.clientContract must be a fully qualified type name"),
        ),
    ),
    diagnostic(
        name = "client-package-without-client-config",
        execution = Execution(
            id = "client-without-config",
            generatorName = "kotlin",
            modelPackage = "com.example.model",
            clientPackage = "com.example.client",
        ),
        expected = ExpectedFailure(messages = listOf("clientConfig is required when clientPackage is configured")),
    ),
    invalidContract(
        name = "reference-cycle",
        expected = ExpectedFailure(messages = listOf("form a cycle", "#/components/messages/accountEvent")),
    ),
    Scenario(
        name = "diagnostics-invalid-avro-name",
        executions = listOf(Execution(id = "avro", generatorName = "avro-schema", schemaPackage = "com.example.avro")),
        expectedFailure = ExpectedFailure(messages = listOf("'account-id'", "not a valid Avro name")),
    ),
    invalidContract(
        name = "recursive-yaml-alias",
        expected = ExpectedFailure(messages = listOf("Recursive YAML alias", "next")),
    ),
    invalidContract(
        name = "inline-name-collision",
        expected = ExpectedFailure(
            messages = listOf(
                "'Address'",
                "components.schemas.Customer.properties.address",
                "components.schemas.Supplier.properties.address",
            ),
        ),
    ),
    Scenario(
        name = "generated-code-edge-cases",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.edgecases.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.edgecases.java"),
        ),
        expectedWarnings = listOf("union members 'left_twin', 'right_twin' declare the same property names"),
    ),
    Scenario(
        name = "inline-union-properties",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.inlineunions.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.inlineunions.java"),
        ),
    ),
    Scenario(
        name = "inline-union-members",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.unionmembers.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.unionmembers.java"),
        ),
    ),
    Scenario(
        name = "titled-open-objects",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.titledobjects.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.titledobjects.java"),
        ),
    ),
    Scenario(
        name = "integer-enums",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.integerenums.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.integerenums.java"),
        ),
    ),
    Scenario(
        name = "literal-values",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.literals.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.literals.java"),
        ),
    ),
    Scenario(
        name = "union-discriminator-values",
        executions = listOf(
            Execution(id = "kotlin-models", generatorName = "kotlin", modelPackage = "com.example.discriminators.kotlin"),
            Execution(id = "java-models", generatorName = "java", modelPackage = "com.example.discriminators.java"),
        ),
    ),
    Scenario(
        name = "kafka-payload-types",
        executions = listOf(
            Execution(
                id = "kotlin-client",
                generatorName = "kotlin",
                modelPackage = "com.example.payloads.kotlin.model",
                clientPackage = "com.example.payloads.kotlin.client",
            ),
            Execution(
                id = "java-client",
                generatorName = "java",
                modelPackage = "com.example.payloads.java.model",
                clientPackage = "com.example.payloads.java.client",
            ),
        ),
        client = ClientConfig(),
    ),
    Scenario(
        name = "kafka-scalar-payloads",
        executions = listOf(
            Execution(
                id = "kotlin-client",
                generatorName = "kotlin",
                modelPackage = "com.example.scalars.kotlin.model",
                clientPackage = "com.example.scalars.kotlin.client",
            ),
            Execution(
                id = "java-client",
                generatorName = "java",
                modelPackage = "com.example.scalars.java.model",
                clientPackage = "com.example.scalars.java.client",
            ),
        ),
        client = ClientConfig(),
    ),
    Scenario(
        name = "kafka-composed-headers",
        executions = listOf(
            Execution(
                id = "kotlin-client",
                generatorName = "kotlin",
                modelPackage = "com.example.headers.kotlin.model",
                clientPackage = "com.example.headers.kotlin.client",
            ),
            Execution(
                id = "java-client",
                generatorName = "java",
                modelPackage = "com.example.headers.java.model",
                clientPackage = "com.example.headers.java.client",
            ),
        ),
        client = ClientConfig(),
    ),
    Scenario(
        name = "avro-schema-types",
        executions = listOf(
            Execution(id = "avro", generatorName = "avro-schema", schemaPackage = "com.example.avrotypes"),
        ),
    ),
    Scenario(
        name = "avro-union-collections",
        executions = listOf(
            Execution(id = "avro", generatorName = "avro-schema", schemaPackage = "com.example.avrounions"),
        ),
    ),
    Scenario(
        name = "diagnostics-property-name-collision",
        executions = listOf(
            Execution(id = "property-name-collision", generatorName = "kotlin", modelPackage = "com.example.model"),
        ),
        expectedFailure = ExpectedFailure(messages = listOf("'Account'", "'account-id'", "'accountId'")),
    ),
    Scenario(
        name = "diagnostics-reserved-model-name",
        executions = listOf(
            Execution(id = "reserved-model-name", generatorName = "kotlin", modelPackage = "com.example.model"),
        ),
        expectedFailure = ExpectedFailure(messages = listOf("Model 'UUID'", "generated Kotlin code already uses")),
    ),
    Scenario(
        name = "diagnostics-header-name-collision",
        executions = listOf(
            Execution(
                id = "header-name-collision",
                generatorName = "kotlin",
                modelPackage = "com.example.model",
                clientPackage = "com.example.client",
            ),
        ),
        client = ClientConfig(),
        expectedFailure = ExpectedFailure(messages = listOf("AccountOpened", "'payload'", "receivedTopic, receivedKey, and messageKey")),
    ),
    Scenario(
        name = "bundle-round-trip",
        executions = listOf(
            Execution(
                id = "contract",
                generatorName = "kotlin",
                outputFile = "bundles/contract.yaml",
                modelPackage = "com.example.roundtrip.model",
            ),
            Execution(
                id = "tuple-bundle",
                generatorName = "kotlin",
                inputSpec = "tuple/asyncapi.yaml",
                outputFile = "bundles/tuple.yaml",
            ),
        ),
    ),
    invalidContract(
        name = "ref-operation-channel-scope",
        expected = ExpectedFailure(messages = listOf("Operation 'sendHidden'", "root 'channels' object")),
    ),
    invalidContract(
        name = "invalid-component-name",
        expected = ExpectedFailure(messages = listOf("Bad name!", "Component messages key")),
    ),
    Scenario(
        name = "dotted-component-names",
        executions = listOf(Execution(id = "dotted-component-names", generatorName = "kotlin", modelPackage = "com.example.model")),
    ),
)

/** A contract every frontend must reject with a clear diagnostic instead of hanging or generating wrong code. */
private fun invalidContract(name: String, expected: ExpectedFailure): Scenario =
    Scenario(
        name = "diagnostics-$name",
        executions = listOf(Execution(id = name, generatorName = "kotlin", modelPackage = "com.example.model")),
        expectedFailure = expected,
    )

/** A configuration mistake every frontend must reject; all diagnostics share one valid contract. */
private fun diagnostic(
    name: String,
    execution: Execution,
    expected: ExpectedFailure,
    client: ClientConfig? = null,
    knownIssues: Map<String, String> = emptyMap(),
) = Scenario(
    name = "diagnostics-$name",
    executions = listOf(execution),
    client = client,
    expectedFailure = expected,
    knownIssues = knownIssues,
)
