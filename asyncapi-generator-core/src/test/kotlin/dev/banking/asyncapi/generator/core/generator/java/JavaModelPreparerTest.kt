package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JavaModelPreparerTest {
    private val preparer = JavaModelPreparer()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `prepare maps generation input into Java model items`() {
        val items =
            preparer.prepare(
                input = fixtures.generationInputWithObjectEnumAndPrimitive(),
                packageName = "com.example.model",
                annotation =
                    QualifiedTypeName.fromConfigurationValue(
                        value = "com.example.GeneratedPayload",
                        path = "modelConfig.modelAnnotation",
                    ),
            )

        assertEquals(listOf("User", "Status"), items.map { it.name })

        val user = items.filterIsInstance<GeneratorItem.ClassModel>().single()
        assertEquals("com.example.model", user.packageName)
        assertEquals(listOf("Command", "Serializable"), user.implementsInterfaces)
        assertEquals(listOf("@GeneratedPayload"), user.classAnnotations)
        assertEquals(listOf("com.example.GeneratedPayload"), user.classAnnotationImports)
        assertEquals("id", user.properties.single().name)
        assertEquals("String", user.properties.single().typeName)

        val status = items.filterIsInstance<GeneratorItem.EnumModel>().single()
        assertEquals("com.example.model", status.packageName)
        assertEquals(listOf("ACTIVE", "INACTIVE"), status.values)
    }

    @Test
    fun `prepare headers maps message headers into Java model items`() {
        val items =
            preparer.prepareHeaders(
                input = fixtures.generationInputWithObjectEnumAndPrimitive(),
                asyncApiDocument = fixtures.documentWithMessageHeaders(),
                packageName = "com.example.client.header",
            )

        val header = items.filterIsInstance<GeneratorItem.ClassModel>().single()
        assertEquals("TopicUserEventsHeadersUserSignup", header.name)
        assertEquals("com.example.client.header", header.packageName)
        assertEquals(
            listOf("correlationId", "applicationInstanceId"),
            header.properties.map { it.name },
        )
        assertEquals(listOf("String", "String"), header.properties.map { it.typeName })
    }
}
