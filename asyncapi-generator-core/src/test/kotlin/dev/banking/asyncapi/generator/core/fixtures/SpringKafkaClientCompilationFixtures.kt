package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import java.nio.file.Path

/**
 * Compiles generated Spring Kafka contracts together with focused framework stubs.
 *
 * The fixture verifies source-language rules without adding Spring Kafka as a
 * runtime dependency of the generator core module.
 */
internal class SpringKafkaClientCompilationFixtures(
    private val compiler: GeneratedKotlinCompiler = GeneratedKotlinCompiler(),
    private val javaCompiler: GeneratedJavaCompiler = GeneratedJavaCompiler(),
) {
    fun compileKotlinConsumerTopicConstant(
        consumerSource: String,
        contractName: String,
        topicAddressConstantName: String,
        payloadName: String,
        workspace: Path,
        keyModelName: String? = null,
    ) {
        compiler.compile(
            artifacts =
                listOf(
                    kotlinSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/consumer/" +
                                "$contractName.kt",
                        content = consumerSource,
                    ),
                    kotlinSource(
                        relativePath = "org/springframework/kafka/support/KafkaHeaders.kt",
                        content =
                            """
                            package org.springframework.kafka.support

                            object KafkaHeaders {
                                const val RECEIVED_TOPIC: String = "kafka_receivedTopic"
                                const val RECEIVED_KEY: String = "kafka_receivedMessageKey"
                            }
                            """.trimIndent(),
                    ),
                    kotlinSource(
                        relativePath = "org/springframework/messaging/handler/annotation/MessageAnnotations.kt",
                        content =
                            """
                            package org.springframework.messaging.handler.annotation

                            @Target(AnnotationTarget.VALUE_PARAMETER)
                            annotation class Header(
                                val name: String,
                                val required: Boolean = true,
                            )

                            @Target(AnnotationTarget.VALUE_PARAMETER)
                            annotation class Payload
                            """.trimIndent(),
                    ),
                    kotlinSource(
                        relativePath = "com/example/account/client/consumer/ConsumerListener.kt",
                        content =
                            """
                            package ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE}.consumer

                            @Target(AnnotationTarget.FUNCTION)
                            annotation class KafkaListener(val topics: Array<String>)

                            class ConsumerListener {
                                @KafkaListener(topics = [$contractName.$topicAddressConstantName])
                                fun listen() = Unit
                            }
                            """.trimIndent(),
                    ),
                ) +
                    (listOf(payloadName) + listOfNotNull(keyModelName)).map { modelName ->
                        kotlinSource(
                            relativePath =
                                "${SpringKafkaClientOutputFixtures.MODEL_PACKAGE.replace('.', '/')}/$modelName.kt",
                            content =
                                """
                                package ${SpringKafkaClientOutputFixtures.MODEL_PACKAGE}

                                class $modelName
                                """.trimIndent(),
                        )
                    },
            workspace = workspace,
        )
    }

    fun compileJavaConsumerTopicConstant(
        consumerSource: String,
        contractName: String,
        topicAddressConstantName: String,
        payloadName: String,
        workspace: Path,
        keyModelName: String? = null,
    ) {
        javaCompiler.compile(
            artifacts =
                listOf(
                    javaSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/consumer/" +
                                "$contractName.java",
                        content = consumerSource,
                    ),
                    javaSource(
                        relativePath = "org/springframework/kafka/support/KafkaHeaders.java",
                        content =
                            """
                            package org.springframework.kafka.support;

                            public final class KafkaHeaders {
                                public static final String RECEIVED_TOPIC = "kafka_receivedTopic";
                                public static final String RECEIVED_KEY = "kafka_receivedMessageKey";
                            }
                            """.trimIndent(),
                    ),
                    javaAnnotation(
                        relativePath = "org/springframework/messaging/handler/annotation/Header.java",
                        packageName = "org.springframework.messaging.handler.annotation",
                        name = "Header",
                        members =
                            """
                            String name();
                            boolean required() default true;
                            """.trimIndent(),
                    ),
                    javaAnnotation(
                        relativePath = "org/springframework/messaging/handler/annotation/Payload.java",
                        packageName = "org.springframework.messaging.handler.annotation",
                        name = "Payload",
                    ),
                    javaAnnotation(
                        relativePath = "jakarta/validation/Valid.java",
                        packageName = "jakarta.validation",
                        name = "Valid",
                    ),
                    javaAnnotation(
                        relativePath = "jakarta/validation/constraints/NotNull.java",
                        packageName = "jakarta.validation.constraints",
                        name = "NotNull",
                    ),
                    javaAnnotation(
                        relativePath = "org/springframework/lang/Nullable.java",
                        packageName = "org.springframework.lang",
                        name = "Nullable",
                    ),
                    javaAnnotation(
                        relativePath = "org/springframework/validation/annotation/Validated.java",
                        packageName = "org.springframework.validation.annotation",
                        name = "Validated",
                        target = "ElementType.TYPE",
                    ),
                    javaAnnotation(
                        relativePath = "com/example/account/client/consumer/KafkaListener.java",
                        packageName = "com.example.account.client.consumer",
                        name = "KafkaListener",
                        members = "String[] topics();",
                        target = "ElementType.METHOD",
                    ),
                    javaSource(
                        relativePath = "com/example/account/client/consumer/ConsumerListener.java",
                        content =
                            """
                            package ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE}.consumer;

                            public final class ConsumerListener {
                                @KafkaListener(topics = $contractName.$topicAddressConstantName)
                                public void listen() {}
                            }
                            """.trimIndent(),
                    ),
                ) +
                    (listOf(payloadName) + listOfNotNull(keyModelName)).map { modelName ->
                        javaSource(
                            relativePath =
                                "${SpringKafkaClientOutputFixtures.MODEL_PACKAGE.replace('.', '/')}/$modelName.java",
                            content =
                                """
                                package ${SpringKafkaClientOutputFixtures.MODEL_PACKAGE};

                                public final class $modelName {}
                                """.trimIndent(),
                        )
                    },
            workspace = workspace,
        )
    }

    fun compileKotlinContracts(
        producerSource: String,
        consumerSource: String,
        producerName: String,
        consumerName: String,
        payloadNames: List<String>,
        workspace: Path,
        keyModelNames: List<String> = emptyList(),
    ) {
        compiler.compile(
            artifacts =
                listOf(
                    kotlinSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/producer/" +
                                "$producerName.kt",
                        content = producerSource,
                    ),
                    kotlinSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/consumer/" +
                                "$consumerName.kt",
                        content = consumerSource,
                    ),
                    kotlinSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/PartialContracts.kt",
                        content =
                            """
                            package ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE}

                            import ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE}.consumer.$consumerName
                            import ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE}.producer.$producerName

                            class PartialContracts : $producerName, $consumerName
                            """.trimIndent(),
                    ),
                    kotlinSource(
                        relativePath = "org/apache/kafka/clients/producer/RecordMetadata.kt",
                        content =
                            """
                            package org.apache.kafka.clients.producer

                            class RecordMetadata
                            """.trimIndent(),
                    ),
                    kotlinSource(
                        relativePath = "org/springframework/kafka/support/KafkaHeaders.kt",
                        content =
                            """
                            package org.springframework.kafka.support

                            object KafkaHeaders {
                                const val RECEIVED_TOPIC: String = "kafka_receivedTopic"
                                const val RECEIVED_KEY: String = "kafka_receivedMessageKey"
                            }
                            """.trimIndent(),
                    ),
                    kotlinSource(
                        relativePath = "org/springframework/messaging/handler/annotation/MessageAnnotations.kt",
                        content =
                            """
                            package org.springframework.messaging.handler.annotation

                            @Target(AnnotationTarget.VALUE_PARAMETER)
                            annotation class Header(
                                val name: String,
                                val required: Boolean = true,
                            )

                            @Target(AnnotationTarget.VALUE_PARAMETER)
                            annotation class Payload
                            """.trimIndent(),
                    ),
                    kotlinSource(
                        relativePath = "jakarta/validation/constraints/NumericConstraints.kt",
                        content =
                            """
                            package jakarta.validation.constraints

                            @Target(AnnotationTarget.VALUE_PARAMETER)
                            annotation class Min(val value: Long)

                            @Target(AnnotationTarget.VALUE_PARAMETER)
                            annotation class Max(val value: Long)
                            """.trimIndent(),
                    ),
                ) +
                    (payloadNames + keyModelNames).distinct().map { modelName ->
                        kotlinSource(
                            relativePath =
                                "${SpringKafkaClientOutputFixtures.MODEL_PACKAGE.replace('.', '/')}/$modelName.kt",
                            content =
                                """
                                package ${SpringKafkaClientOutputFixtures.MODEL_PACKAGE}

                                class $modelName
                                """.trimIndent(),
                        )
                    },
            workspace = workspace,
        )
    }

    fun compileJavaContracts(
        producerSource: String,
        consumerSource: String,
        producerName: String,
        consumerName: String,
        payloadNames: List<String>,
        workspace: Path,
        keyModelNames: List<String> = emptyList(),
    ) {
        javaCompiler.compile(
            artifacts =
                listOf(
                    javaSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/producer/" +
                                "$producerName.java",
                        content = producerSource,
                    ),
                    javaSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/consumer/" +
                                "$consumerName.java",
                        content = consumerSource,
                    ),
                    javaSource(
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE.replace('.', '/')}/PartialContracts.java",
                        content =
                            """
                            package ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE};

                            import ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE}.consumer.$consumerName;
                            import ${SpringKafkaClientOutputFixtures.CLIENT_PACKAGE}.producer.$producerName;

                            public final class PartialContracts implements $producerName, $consumerName {}
                            """.trimIndent(),
                    ),
                    javaSource(
                        relativePath = "org/apache/kafka/clients/producer/RecordMetadata.java",
                        content =
                            """
                            package org.apache.kafka.clients.producer;

                            public final class RecordMetadata {}
                            """.trimIndent(),
                    ),
                    javaSource(
                        relativePath = "org/springframework/kafka/support/KafkaHeaders.java",
                        content =
                            """
                            package org.springframework.kafka.support;

                            public final class KafkaHeaders {
                                public static final String RECEIVED_TOPIC = "kafka_receivedTopic";
                                public static final String RECEIVED_KEY = "kafka_receivedMessageKey";
                            }
                            """.trimIndent(),
                    ),
                    javaAnnotation(
                        relativePath = "org/springframework/messaging/handler/annotation/Header.java",
                        packageName = "org.springframework.messaging.handler.annotation",
                        name = "Header",
                        members =
                            """
                            String name();
                            boolean required() default true;
                            """.trimIndent(),
                    ),
                    javaAnnotation(
                        relativePath = "org/springframework/messaging/handler/annotation/Payload.java",
                        packageName = "org.springframework.messaging.handler.annotation",
                        name = "Payload",
                    ),
                    javaAnnotation(
                        relativePath = "jakarta/validation/constraints/NotNull.java",
                        packageName = "jakarta.validation.constraints",
                        name = "NotNull",
                    ),
                    javaAnnotation(
                        relativePath = "jakarta/validation/constraints/Min.java",
                        packageName = "jakarta.validation.constraints",
                        name = "Min",
                        members = "long value();",
                    ),
                    javaAnnotation(
                        relativePath = "jakarta/validation/constraints/Max.java",
                        packageName = "jakarta.validation.constraints",
                        name = "Max",
                        members = "long value();",
                    ),
                    javaAnnotation(
                        relativePath = "org/springframework/lang/Nullable.java",
                        packageName = "org.springframework.lang",
                        name = "Nullable",
                    ),
                ) +
                    (payloadNames + keyModelNames).distinct().map { modelName ->
                        javaSource(
                            relativePath =
                                "${SpringKafkaClientOutputFixtures.MODEL_PACKAGE.replace('.', '/')}/$modelName.java",
                            content =
                                """
                                package ${SpringKafkaClientOutputFixtures.MODEL_PACKAGE};

                                public final class $modelName {}
                                """.trimIndent(),
                        )
                    },
            workspace = workspace,
        )
    }

    private fun kotlinSource(
        relativePath: String,
        content: String,
    ): GeneratedArtifact =
        GeneratedArtifact(
            relativePath = relativePath,
            content = content,
            kind = GeneratedArtifactKind.SOURCE,
        )

    private fun javaAnnotation(
        relativePath: String,
        packageName: String,
        name: String,
        members: String = "",
        target: String = "ElementType.PARAMETER",
    ): GeneratedArtifact =
        javaSource(
            relativePath = relativePath,
            content =
                """
                package $packageName;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;

                @Target($target)
                public @interface $name {
                ${members.prependIndent("    ")}
                }
                """.trimIndent(),
        )

    private fun javaSource(
        relativePath: String,
        content: String,
    ): GeneratedArtifact =
        GeneratedArtifact(
            relativePath = relativePath,
            content = content,
            kind = GeneratedArtifactKind.JAVA_SOURCE,
        )
}
