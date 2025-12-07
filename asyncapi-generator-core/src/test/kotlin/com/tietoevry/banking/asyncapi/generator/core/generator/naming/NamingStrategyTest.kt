package com.tietoevry.banking.asyncapi.generator.core.generator.naming

import com.tietoevry.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class NamingStrategyTest : AbstractKotlinGeneratorClass() {

    @Test
    fun `should preserve snake_case property names`() {
        val yamlContent = """
   asyncapi: 3.0.0
   info: { title: Naming, version: 1.0.0 }
   components:
     schemas:
       SnakeUser:
         type: object
         properties:
           user_id: { type: string }
           created_at: { type: string }
           """.trimIndent()

        val yamlFile = File("target/test-output/naming/snake.yaml")
        yamlFile.parentFile.mkdirs()
        yamlFile.writeText(yamlContent)

        val generated = generateElement(
            yaml = yamlFile,
            generated = "SnakeUser.kt",
            modelPackage = "com.example.naming"
        )
        assertTrue(generated.contains("val user_id: String"), "user_id was renamed!")
        assertTrue(generated.contains("val created_at: String"), "created_at was renamed!")
    }
}
