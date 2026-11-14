package com.example.generatedcode

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.example.codegen.java.AccountChanged as JavaAccountChanged
import com.example.codegen.java.AccountStatus as JavaAccountStatus
import com.example.codegen.java.Circle as JavaCircle
import com.example.codegen.java.Shape as JavaShape
import com.example.codegen.kotlin.AccountChanged as KotlinAccountChanged
import com.example.codegen.kotlin.AccountStatus as KotlinAccountStatus
import com.example.codegen.kotlin.Circle as KotlinCircle
import com.example.codegen.kotlin.Shape as KotlinShape
import com.example.codegen.kotlin.Tier as KotlinTier

/**
 * Generated Kotlin and Java models compile for awkward contract content and keep the contract's
 * wire format: enum values, renamed and read-only properties, defaults, and polymorphism.
 */
class GeneratedCodeTest {
    private val kotlinJson = jacksonObjectMapper()
    private val javaJson = ObjectMapper()

    private val message = """
        {"status": "2fa-required", "account-id": "acct-1", "limits": {"daily": {"eur": 5}},
         "shape": {"kind": "circle", "radius": 2.0}, "tags": ["a", "b"], "reference": "ref-1",
         "payment": {"kind": "transfer", "iban": "NO93"}}
    """

    @Test
    fun `Kotlin models read and write the contract's wire format`() {
        val account = kotlinJson.readValue<KotlinAccountChanged>(message)

        assertEquals(KotlinAccountStatus._2FA_REQUIRED, account.status)
        assertNull(account.accountId, "a read-only property is not read from messages")
        assertEquals(mapOf("daily" to mapOf("eur" to 5)), account.limits)
        assertEquals(2.0, assertIs<KotlinCircle>(account.shape).radius)
        assertEquals(listOf("a", "b"), account.tags, "a named array schema is used in place")
        assertEquals("ref-1", account.reference, "a named scalar schema is used in place")
        assertEquals("NO93", assertIs<com.example.codegen.kotlin.Transfer>(account.payment).iban)

        val written = kotlinJson.readTree(kotlinJson.writeValueAsString(account.copy(accountId = "acct-2")))
        assertEquals("2fa-required", written["status"].asText())
        assertEquals("acct-2", written["account-id"].asText())
        assertEquals("circle", written["shape"]["kind"].asText())
        assertTrue(written["shape"] is com.fasterxml.jackson.databind.node.ObjectNode)
    }

    @Test
    fun `Kotlin defaults keep the contract's values`() {
        val account = KotlinAccountChanged(status = KotlinAccountStatus.IN_PROGRESS)

        assertEquals(KotlinTier.SILVER, account.tier)
        assertEquals("say \"hi\" to \$name", account.note)
        assertEquals(1.0, account.ratio)
        assertEquals("silver", kotlinJson.readTree(kotlinJson.writeValueAsString(account))["tier"].asText())
    }

    @Test
    fun `Kotlin unions are deserialized by their discriminator`() {
        assertIs<KotlinCircle>(kotlinJson.readValue<KotlinShape>("""{"kind": "circle", "radius": 1.0}"""))
    }

    @Test
    fun `Java models read and write the contract's wire format`() {
        val account = javaJson.readValue(message, JavaAccountChanged::class.java)

        assertEquals(JavaAccountStatus._2FA_REQUIRED, account.status)
        assertNull(account.accountId, "a read-only property is not read from messages")
        assertEquals(mapOf("daily" to mapOf("eur" to 5)), account.limits)
        assertEquals(2.0, assertIs<JavaCircle>(account.shape).radius)
        assertIs<JavaCircle>(javaJson.readValue("""{"kind": "circle"}""", JavaShape::class.java))
        assertEquals(listOf("a", "b"), account.tags)
        assertEquals("NO93", assertIs<com.example.codegen.java.Transfer>(account.payment).iban)

        account.accountId = "acct-2"
        val written = javaJson.readTree(javaJson.writeValueAsString(account))
        assertEquals("2fa-required", written["status"].asText())
        assertEquals("acct-2", written["account-id"].asText())
    }
}
