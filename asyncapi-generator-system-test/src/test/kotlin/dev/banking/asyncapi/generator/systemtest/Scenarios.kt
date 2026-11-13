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
        libraries = setOf(Library.SPRING_KAFKA),
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
        libraries = setOf(Library.SPRING_KAFKA),
        knownIssues = mapOf(
            "code generated from the bundled document of 'message-models' matches the original" to
                "bundling renames external component schemas (MyAccountDetails becomes Details)",
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
        libraries = setOf(Library.SPRING_KAFKA, Library.AVRO, Library.PROTOBUF),
        knownIssues = mapOf(
            "code generated from the bundled document of 'kotlin-client' matches the original" to
                "the bundled document fails generation: an inlined message payload collides with its component schema",
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
        libraries = setOf(Library.SPRING_KAFKA, Library.AVRO),
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
        libraries = setOf(Library.SPRING_KAFKA),
        expectedWarnings = listOf("unused-client-config"),
        knownIssues = mapOf(
            "Maven generates, compiles, and passes checks" to
                "Maven rejects shared clientConfig on executions without clientPackage instead of warning",
            "CLI generates" to "the CLI rejects client options without clientPackage instead of warning",
            "all frontends generate identical output" to "Maven and the CLI reject shared clientConfig",
            "every frontend warns 'unused-client-config'" to "unused clientConfig is ignored without a warning",
        ),
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
        knownIssues = mapOf(
            "Maven fails with the expected diagnostic" to
                "Maven validates the configuration before checking that inputSpec exists",
        ) + gradleFailures("Gradle reports 'Unable to read input document' instead of saying the file does not exist"),
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
        name = "client-package-without-client-config",
        execution = Execution(
            id = "client-without-config",
            generatorName = "kotlin",
            modelPackage = "com.example.model",
            clientPackage = "com.example.client",
        ),
        expected = ExpectedFailure(messages = listOf("clientConfig is required when clientPackage is configured")),
        knownIssues = gradleFailures("Gradle reports 'clientConfig.clientType is required' without naming clientPackage"),
    ),
)

/** The same known issue for the failure test of both Gradle DSLs. */
private fun gradleFailures(issue: String): Map<String, String> =
    listOf("Gradle", "Gradle Groovy DSL").associate { "$it fails with the expected diagnostic" to issue }

/** A configuration mistake every frontend must reject; all diagnostics share one valid contract. */
private fun diagnostic(
    name: String,
    execution: Execution,
    expected: ExpectedFailure,
    knownIssues: Map<String, String> = emptyMap(),
) = Scenario(
    name = "diagnostics-$name",
    files = "configuration-diagnostics",
    executions = listOf(execution),
    expectedFailure = expected,
    knownIssues = knownIssues,
)
