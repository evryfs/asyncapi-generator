package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.avro.NativeAvroSchemaParser
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeyModelSelector
import dev.banking.asyncapi.generator.core.generator.kafka.KafkaKeySchemaResolver
import dev.banking.asyncapi.generator.core.generator.kafka.spring.SpringKafkaClientContractValidator
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.MissingSchemaGenerationInput
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.NativeAvroModelPackageMismatch
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedPayloadSchemaFormat
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedSchemaGenerationInput
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema

/**
 * Validates that planned generator outputs can consume the prepared generator input.
 */
class GenerationInputCompatibilityValidator(
    private val nativeAvroSchemaParser: NativeAvroSchemaParser = NativeAvroSchemaParser(),
) {

    fun validate(
        generationInput: GenerationInput,
        generationPlan: GenerationPlan,
    ) {
        val hasAvroProjection =
            generationPlan.tasks.any { task -> task is GenerationTask.AvroSchemaArtifacts }
        val hasNativeAvro =
            generationPlan.tasks.any { task -> task is GenerationTask.NativeAvroArtifacts }
        val hasNativeProtobuf =
            generationPlan.tasks.any { task -> task is GenerationTask.NativeProtobufArtifacts }
        val hasJsonSchema =
            generationPlan.tasks.any { task -> task is GenerationTask.JsonSchemaArtifacts }
        val springKafkaHandledNativeSchemaNames =
            collectSpringKafkaHandledNativeSchemaNames(
                generationInput = generationInput,
                generationPlan = generationPlan,
            )

        generationPlan.tasks.forEach { task ->
            when (task) {
                is GenerationTask.ModelArtifacts -> {
                    rejectMultiFormatSchemas(
                        output = "Model generation",
                        multiFormatSchemas = generationInput.multiFormatSchemas,
                    )
                    SourceSchemaCompatibilityValidator.validate(
                        output = "Model generation",
                        schemas = generationInput.schemas,
                        checkStructuralModels = true,
                        checkJavaPatterns = true,
                    )
                }
                is GenerationTask.SpringKafkaClient -> {
                    SpringKafkaClientContractValidator.validate(
                        channels = generationInput.channels,
                        task = task,
                    )
                    if (task.generateProducers || task.generateConsumers) {
                        rejectUnsupportedMultiFormatMessages(
                            output = "Spring Kafka client generation",
                            generationInput = generationInput,
                        )
                        validateSpringKafkaKeyPatterns(generationInput)
                    }
                }
                is GenerationTask.AvroSchemaArtifacts -> {
                    rejectUnplannedAvroProjectionFormats(
                        multiFormatSchemas = generationInput.multiFormatSchemas,
                        hasNativeAvro = hasNativeAvro,
                        hasNativeProtobuf = hasNativeProtobuf,
                    )
                    SourceSchemaCompatibilityValidator.validate(
                        output = "Avro projection",
                        schemas = generationInput.schemas,
                        checkStructuralModels = true,
                        checkJavaPatterns = false,
                    )
                }
                is GenerationTask.NativeAvroArtifacts -> {
                    if (
                        task.generateSpecificRecords ||
                        !hasAvroProjection ||
                        generationInput.schemas.isEmpty()
                    ) {
                        requireNativeSchema(
                            output = "Native Avro generation",
                            generationInput = generationInput,
                            supportedInput = "native Avro schemas",
                            isSupported = { schema -> schema.format.isNativeAvro },
                        )
                    }
                    if (task.generateSpecificRecords && task.modelPackageName != null) {
                        validateNativeAvroModelPackage(
                            generationInput = generationInput,
                            configuredPackage = task.modelPackageName,
                        )
                    }
                }
                is GenerationTask.NativeProtobufArtifacts -> {
                    requireNativeSchema(
                        output = "Native Protobuf generation",
                        generationInput = generationInput,
                        supportedInput = "native Protobuf schemas",
                        isSupported = { schema -> schema.format.isNativeProtobuf },
                    )
                }
                is GenerationTask.JsonSchemaArtifacts ->
                    requireJsonSchema(generationInput)
                is GenerationTask.KafkaKeyModelArtifacts ->
                    SourceSchemaCompatibilityValidator.validate(
                        output = "Kafka key model generation",
                        schemas = KafkaKeyModelSelector.select(generationInput),
                        checkStructuralModels = true,
                        checkJavaPatterns = true,
                    )
                is GenerationTask.DocumentArtifact,
                -> Unit
            }
        }

        rejectUnhandledExplicitFormats(
            generationInput = generationInput,
            hasNativeAvro = hasNativeAvro,
            hasNativeProtobuf = hasNativeProtobuf,
            hasJsonSchema = hasJsonSchema,
            springKafkaHandledNativeSchemaNames = springKafkaHandledNativeSchemaNames,
        )
    }

    private fun validateSpringKafkaKeyPatterns(generationInput: GenerationInput) {
        generationInput.channels.forEach { channel ->
            channel.messages.forEach { message ->
                message.keySchema?.let { keySchema ->
                    val schema = KafkaKeySchemaResolver.resolve(message.messageName, keySchema).schema
                    SourceSchemaCompatibilityValidator.validateRootJavaPattern(
                        output = "Spring Kafka client generation",
                        rootSchemaName = message.messageName,
                        schema = schema,
                    )
                }
            }
            channel.multiFormatMessages.forEach { message ->
                message.keySchema?.let { keySchema ->
                    val schema = KafkaKeySchemaResolver.resolve(message.messageName, keySchema).schema
                    SourceSchemaCompatibilityValidator.validateRootJavaPattern(
                        output = "Spring Kafka client generation",
                        rootSchemaName = message.messageName,
                        schema = schema,
                    )
                }
            }
        }
    }

    private fun requireNativeSchema(
        output: String,
        generationInput: GenerationInput,
        supportedInput: String,
        isSupported: (MultiFormatSchema) -> Boolean,
    ) {
        if (generationInput.multiFormatSchemas.values.any(isSupported)) {
            return
        }

        val firstSchema =
            generationInput.multiFormatSchemas.entries.firstOrNull()
        if (firstSchema != null) {
            throw UnsupportedSchemaGenerationInput(
                output = output,
                payloadName = firstSchema.key,
                inputFormat = "schemaFormat '${firstSchema.value.schemaFormat}'",
                supportedInput = supportedInput,
            )
        }

        val payloadName = generationInput.schemas.keys.firstOrNull()
        if (payloadName == null) {
            throw MissingSchemaGenerationInput(
                output = output,
                supportedInput = supportedInput,
            )
        }
        throw UnsupportedSchemaGenerationInput(
            output = output,
            payloadName = payloadName,
            inputFormat = "an AsyncAPI Schema Object",
            supportedInput = supportedInput,
        )
    }

    private fun validateNativeAvroModelPackage(
        generationInput: GenerationInput,
        configuredPackage: String,
    ) {
        generationInput.multiFormatSchemas
            .filterValues { schema -> schema.format.isNativeAvro }
            .forEach { (payloadName, schema) ->
                val schemaNamespace =
                    nativeAvroSchemaParser
                        .parse(payloadName, schema)
                        .namespace
                if (schemaNamespace != configuredPackage) {
                    throw NativeAvroModelPackageMismatch(
                        payloadName = payloadName,
                        configuredPackage = configuredPackage,
                        schemaNamespace = schemaNamespace,
                    )
                }
            }
    }

    private fun requireJsonSchema(generationInput: GenerationInput) {
        val incompatibleSchema =
            generationInput.multiFormatSchemas.entries
                .firstOrNull { (_, schema) -> !schema.format.isJsonSchemaDraft07 }
        if (incompatibleSchema != null) {
            throw UnsupportedSchemaGenerationInput(
                output = "JSON Schema generation",
                payloadName = incompatibleSchema.key,
                inputFormat = "schemaFormat '${incompatibleSchema.value.schemaFormat}'",
                supportedInput = "AsyncAPI Schema Objects, Boolean schemas, and native JSON Schema Draft 07 schemas",
            )
        }

        if (
            generationInput.schemaDeclarations.asyncApiSchemas.isNotEmpty() ||
            generationInput.schemaDeclarations.multiFormatSchemas.isNotEmpty() ||
            generationInput.schemaDeclarations.booleanSchemas.isNotEmpty()
        ) {
            return
        }

        throw MissingSchemaGenerationInput(
            output = "JSON Schema generation",
            supportedInput = "AsyncAPI Schema Objects, Boolean schemas, and native JSON Schema Draft 07 schemas",
        )
    }

    private fun rejectMultiFormatSchemas(
        output: String,
        multiFormatSchemas: Map<String, MultiFormatSchema>,
    ) {
        val firstSchema = multiFormatSchemas.entries.firstOrNull() ?: return
        throw UnsupportedPayloadSchemaFormat(
            output = output,
            payloadName = firstSchema.key,
            schemaFormat = firstSchema.value.schemaFormat,
        )
    }

    private fun rejectUnplannedAvroProjectionFormats(
        multiFormatSchemas: Map<String, MultiFormatSchema>,
        hasNativeAvro: Boolean,
        hasNativeProtobuf: Boolean,
    ) {
        val firstUnsupportedSchema =
            multiFormatSchemas.entries.firstOrNull { (_, schema) ->
                when {
                    schema.format.isNativeAvro -> !hasNativeAvro
                    schema.format.isNativeProtobuf -> !hasNativeProtobuf
                    else -> true
                }
            } ?: return

        throw UnsupportedPayloadSchemaFormat(
            output = "Avro Projection",
            payloadName = firstUnsupportedSchema.key,
            schemaFormat = firstUnsupportedSchema.value.schemaFormat,
        )
    }

    private fun rejectUnsupportedMultiFormatMessages(
        output: String,
        generationInput: GenerationInput,
    ) {
        val firstMessage =
            generationInput.channels
                .asSequence()
                .flatMap { channel -> channel.multiFormatMessages.asSequence() }
                .filterNot { message -> message.schema.format.isNativeAvro || message.schema.format.isNativeProtobuf }
                .firstOrNull() ?: return

        throw UnsupportedPayloadSchemaFormat(
            output = output,
            payloadName = firstMessage.payloadName,
            schemaFormat = firstMessage.schema.schemaFormat,
        )
    }

    private fun rejectUnhandledExplicitFormats(
        generationInput: GenerationInput,
        hasNativeAvro: Boolean,
        hasNativeProtobuf: Boolean,
        hasJsonSchema: Boolean,
        springKafkaHandledNativeSchemaNames: Set<String>,
    ) {
        val firstUnhandled =
            generationInput.multiFormatSchemas.entries.firstOrNull { (schemaName, schema) ->
                when {
                    schema.format.isNativeAvro ->
                        hasNativeAvro || schemaName in springKafkaHandledNativeSchemaNames
                    schema.format.isNativeProtobuf ->
                        hasNativeProtobuf || schemaName in springKafkaHandledNativeSchemaNames
                    schema.format.isJsonSchemaDraft07 -> hasJsonSchema
                    else -> false
                }.not()
            } ?: return

        throw UnsupportedPayloadSchemaFormat(
            output = "Generation",
            payloadName = firstUnhandled.key,
            schemaFormat = firstUnhandled.value.schemaFormat,
        )
    }

    private fun collectSpringKafkaHandledNativeSchemaNames(
        generationInput: GenerationInput,
        generationPlan: GenerationPlan,
    ): Set<String> {
        val hasActiveSpringKafka =
            generationPlan.tasks.any { task ->
                task is GenerationTask.SpringKafkaClient &&
                    (task.generateProducers || task.generateConsumers)
            }
        if (!hasActiveSpringKafka) return emptySet()

        return generationInput.channels
            .asSequence()
            .flatMap { channel -> channel.multiFormatMessages.asSequence() }
            .filter { message -> message.schema.format.isNativeAvro || message.schema.format.isNativeProtobuf }
            .map { message -> message.payloadName }
            .toSet()
    }
}
