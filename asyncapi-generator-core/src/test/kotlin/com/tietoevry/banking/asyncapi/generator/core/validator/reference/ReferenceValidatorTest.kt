package com.tietoevry.banking.asyncapi.generator.core.validator.reference

//class ReferenceValidatorTest : AbstractParserTest() {
//
//    private val parser = AsyncApiParser(asyncApiContext)
//    private val validator = AsyncApiValidator(asyncApiContext)
//
//    @Test
//    fun `validation fails for unresolved schema reference`() {
//        val root = readYaml("src/test/resources/validator/asyncapi_broken_ref.yaml")
//        val document = parser.parse(root)
//
//        val results = validator.validate(document)
//
//        // We need to access the errors. Assuming ValidationResults has an accessor or we can check logic.
//        // If ValidationResults doesn't expose list, we might need to add it or use reflection for testing.
//        // Based on previous snippets, it has private lists. Let's assume we added an accessor 'errors' or 'getErrors()'.
//        // Or we rely on throwErrors() and catch exception?
//        // Better to have an accessor for testing without exceptions.
//
//        // Assuming we modify ValidationResults to expose: fun getErrors(): List<ValidationError>
//        // For now, let's try to catch the exception from throwErrors()
//
//        var exceptionThrown = false
//        try {
//            results.throwErrors()
//        } catch (e: Exception) {
//            exceptionThrown = true
//            val msg = e.message ?: ""
//            assertTrue(msg.contains("Could not resolve reference"), "Error message should mention resolution failure: $msg")
//            assertTrue(msg.contains("#/components/schemas/NonExistentAddress"), "Error message should contain the broken ref")
//        }
//
//        assertTrue(exceptionThrown, "Validation should have thrown errors for broken reference")
//    }
//}
