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
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.MODEL_PACKAGE.replace('.', '/')}/$payloadName.kt",
                        content =
                            """
                            package ${SpringKafkaClientOutputFixtures.MODEL_PACKAGE}

                            class $payloadName
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
                ),
            workspace = workspace,
        )
    }

    fun compileJavaConsumerTopicConstant(
        consumerSource: String,
        contractName: String,
        topicAddressConstantName: String,
        payloadName: String,
        workspace: Path,
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
                        relativePath =
                            "${SpringKafkaClientOutputFixtures.MODEL_PACKAGE.replace('.', '/')}/$payloadName.java",
                        content =
                            """
                            package ${SpringKafkaClientOutputFixtures.MODEL_PACKAGE};

                            public final class $payloadName {}
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
                ),
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
