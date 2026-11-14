package com.example.references

import com.example.external.client.consumer.MyAccountCreatedConsumer
import com.example.external.client.consumer.MyAccountReviewedConsumer
import com.example.external.client.producer.MyAccountCreatedProducer
import com.example.external.client.producer.MyAccountReviewedProducer
import com.example.external.model.MyAccountCreatedV1Payload
import com.example.external.model.MyAccountDetails
import com.example.external.model.MyAccountReviewMetadata
import com.example.external.model.MyAccountReviewedV1Payload
import com.example.message.client.consumer.AuditEventsConsumer
import com.example.message.client.producer.AuditEventsProducer
import com.example.message.model.MyReferencedSchema
import com.example.references.client.consumer.DirectPayloadEventsConsumer
import com.example.references.client.consumer.ExternalChannelEventsConsumer
import com.example.references.client.consumer.RawMessageEventsConsumer
import com.example.references.client.consumer.RecursiveNodeEventsConsumer
import com.example.references.client.consumer.SharedPayloadEventsConsumer
import com.example.references.client.producer.DirectPayloadEventsProducer
import com.example.references.client.producer.ExternalChannelEventsProducer
import com.example.references.client.producer.RawMessageEventsProducer
import com.example.references.client.producer.RecursiveNodeEventsProducer
import com.example.references.client.producer.SharedPayloadEventsProducer
import com.example.references.model.DirectPayloadV1Payload
import com.example.references.model.ExternalChannelMessageV1Payload
import com.example.references.model.RawMessageV1Payload
import com.example.references.model.RecursiveNodePayload
import com.example.references.model.SharedPayloadCreatedV1Payload
import com.example.references.model.SharedPayloadUpdatedV1Payload
import systemtest.support.Classpath
import systemtest.support.Documents
import java.lang.reflect.ParameterizedType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.example.recursive.java.TreeNode as JavaTreeNode
import com.example.recursive.kotlin.TreeNode as KotlinTreeNode

/**
 * Contracts split across files resolve to typed models and client contracts.
 * The calls below only compile when every reference was followed to the right schema.
 */
class ExternalReferencesTest {

    @Test
    fun `every external reference form produces a typed producer and consumer`() {
        val producers = listOf(
            object : RawMessageEventsProducer {}.sendRawMessageV1(RawMessageV1Payload(rawValue = "raw")),
            object : DirectPayloadEventsProducer {}.sendDirectPayloadV1(DirectPayloadV1Payload(directValue = 1)),
            object : ExternalChannelEventsProducer {}
                .sendExternalChannelMessageV1(ExternalChannelMessageV1Payload(externalValue = true)),
            object : SharedPayloadEventsProducer {}
                .sendSharedPayloadCreatedV1(SharedPayloadCreatedV1Payload(sharedValue = "created")),
            object : SharedPayloadEventsProducer {}
                .sendSharedPayloadUpdatedV1(SharedPayloadUpdatedV1Payload(sharedValue = "updated")),
            object : RecursiveNodeEventsProducer {}
                .sendRecursiveNodeV1(RecursiveNodePayload(name = "root")),
        )
        producers.forEach { assertTrue(it.isCompletedExceptionally, "default producer methods fail until implemented") }

        object : RawMessageEventsConsumer {}.listenRawMessageV1(RawMessageV1Payload("raw"), "topic")
        object : DirectPayloadEventsConsumer {}.listenDirectPayloadV1(DirectPayloadV1Payload(1), "topic")
        object : ExternalChannelEventsConsumer {}
            .listenExternalChannelMessageV1(ExternalChannelMessageV1Payload(true), "topic")
        object : SharedPayloadEventsConsumer {}.listenSharedPayloadCreatedV1(SharedPayloadCreatedV1Payload("a"), "topic")
        object : SharedPayloadEventsConsumer {}.listenSharedPayloadUpdatedV1(SharedPayloadUpdatedV1Payload("b"), "topic")
        object : RecursiveNodeEventsConsumer {}.listenRecursiveNodeV1(RecursiveNodePayload("root"), "topic")
    }

    @Test
    fun `raw external schema fragments become message-owned payloads, not a shared model`() {
        assertFalse(Classpath.hasClass("com.example.references.model.SharedPayload"))
    }

    @Test
    fun `recursive external payloads keep their self-reference`() {
        val tree = RecursiveNodePayload(name = "root", children = listOf(RecursiveNodePayload(name = "leaf")))
        assertEquals("leaf", tree.children?.single()?.name)
    }

    @Test
    fun `a message object in another file resolves its payload, key, and headers`() {
        val payload = MyReferencedSchema(firstField = 1)
        assertEquals(null, payload.secondField, "optional field defaults to null")
        object : AuditEventsProducer {}.sendAuditRecorded(payload, messageKey = "key", myFirstHeader = "required")
        assertEquals("audit.\${kafka.environment}.events.v1", AuditEventsConsumer.AUDIT_EVENTS_TOPIC_ADDRESS)
    }

    @Test
    fun `external messages and foreign component containers produce their supporting models`() {
        val created = MyAccountCreatedV1Payload(accountId = "acct-1", details = MyAccountDetails(status = "open"))
        val reviewed = MyAccountReviewedV1Payload(reviewId = "r-1", metadata = MyAccountReviewMetadata(reviewer = "me"))
        object : MyAccountCreatedProducer {}.sendMyAccountCreatedV1(created)
        object : MyAccountReviewedProducer {}.sendMyAccountReviewedV1(reviewed)
        object : MyAccountCreatedConsumer {}.listenMyAccountCreatedV1(created, "topic")
        object : MyAccountReviewedConsumer {}.listenMyAccountReviewedV1(reviewed, "topic")
    }

    @Test
    fun `recursive external schemas generate self-referencing Kotlin and Java models`() {
        val kotlinTree = KotlinTreeNode(name = "root", children = listOf(KotlinTreeNode(name = "leaf")))
        assertEquals(1, kotlinTree.children?.size)

        val javaChildren = JavaTreeNode::class.java.getDeclaredField("children").genericType as ParameterizedType
        assertEquals(List::class.java, javaChildren.rawType)
        assertEquals(JavaTreeNode::class.java, javaChildren.actualTypeArguments.single())
    }

    @Test
    fun `bundles keep the recursive schema as an internal component reference`() {
        listOf("bundles/recursive-kotlin.yaml", "bundles/recursive-java.yaml").forEach { path ->
            val document = Documents.read(path)
            assertEquals(emptyList(), Documents.externalReferences(document), "$path has no external references")
            assertFalse(document.at("/components/schemas/TreeNode").isMissingNode, "$path keeps TreeNode as a component")
            assertTrue("#/components/schemas/TreeNode" in Documents.references(document), "$path references TreeNode")
        }
    }

    @Test
    fun `the bundle of split message files has no external references`() {
        assertEquals(emptyList(), Documents.externalReferences(Documents.read("bundles/message-models.yaml")))
    }
}
