package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.java.model.PropertyModel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaGeneratorTest {
    @Test
    fun `render returns generation result with artifacts for Java model items`() {
        val generationModel =
            listOf(
                GeneratorItem.ClassModel(
                    name = "User",
                    packageName = "com.example.model",
                    description = emptyList(),
                    properties =
                        listOf(
                            PropertyModel(
                                name = "id",
                                description = emptyList(),
                                typeName = "String",
                                getterName = "getId",
                                setterName = "setId",
                                annotations = emptyList(),
                            ),
                        ),
                ),
                GeneratorItem.EnumModel(
                    name = "Status",
                    packageName = "com.example.model",
                    description = emptyList(),
                    values = listOf("ACTIVE", "INACTIVE"),
                ),
                GeneratorItem.InterfaceModel(
                    name = "Command",
                    packageName = "com.example.model",
                    description = emptyList(),
                ),
                GeneratorItem.KafkaConsumerInterface(
                    name = "IgnoredConsumer",
                    packageName = "com.example.kafka",
                    description = emptyList(),
                    topicAddressConstantName = "IGNORED_TOPIC_ADDRESS",
                    topicAddress = "ignored",
                    methods = emptyList(),
                ),
            )
        val generator =
            JavaGenerator(
                packageName = "com.example.model",
                generationModel = generationModel,
            )

        val result = generator.render()

        assertEquals(
            listOf(
                "com/example/model/User.java",
                "com/example/model/Status.java",
                "com/example/model/Command.java",
            ),
            result.artifacts.map { it.relativePath },
        )
    }

    @Test
    fun `render uses Java record output when configured`() {
        val generationModel =
            listOf(
                GeneratorItem.ClassModel(
                    name = "User",
                    packageName = "com.example.model",
                    description = emptyList(),
                    properties =
                        listOf(
                            PropertyModel(
                                name = "id",
                                description = emptyList(),
                                typeName = "String",
                                getterName = "getId",
                                setterName = "setId",
                                annotations = emptyList(),
                            ),
                        ),
                ),
            )
        val generator =
            JavaGenerator(
                packageName = "com.example.model",
                generationModel = generationModel,
                javaModelType = JavaModelType.RECORD,
            )

        val result = generator.render()

        assertEquals(1, result.artifacts.size)
        assertTrue(result.artifacts.single().content.contains("public record User("))
    }
}
