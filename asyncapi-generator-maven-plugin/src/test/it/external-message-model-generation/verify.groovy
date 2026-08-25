def output = new File(basedir, "target/generated-sources/asyncapi")
def modelPackage = new File(output, "com/example/external/model")
def clientPackage = new File(output, "com/example/external/client")
def payload = new File(modelPackage, "MyAccountCreatedV1Payload.kt")
def details = new File(modelPackage, "MyAccountDetails.kt")
def foreignPayload = new File(modelPackage, "MyAccountReviewedV1Payload.kt")
def foreignMetadata = new File(modelPackage, "MyAccountReviewMetadata.kt")
def producer = new File(clientPackage, "producer/MyAccountCreatedProducer.kt")
def consumer = new File(clientPackage, "consumer/MyAccountCreatedConsumer.kt")
def foreignProducer = new File(clientPackage, "producer/MyAccountReviewedProducer.kt")
def foreignConsumer = new File(clientPackage, "consumer/MyAccountReviewedConsumer.kt")

assert payload.isFile() : "Expected the referenced payload model to be generated"
assert details.isFile() : "Expected the referenced supporting model to be generated"
assert foreignPayload.isFile() : "Expected the foreign-container payload model to be generated"
assert foreignMetadata.isFile() : "Expected the foreign-container supporting model to be generated"
assert payload.text.contains("val accountId: String")
assert payload.text.contains("val details: MyAccountDetails")
assert foreignPayload.text.contains("val reviewId: String")
assert foreignPayload.text.contains("val metadata: MyAccountReviewMetadata")
assert producer.isFile() : "Expected the producer contract to be generated"
assert consumer.isFile() : "Expected the consumer contract to be generated"
assert foreignProducer.isFile() : "Expected the foreign-container producer contract to be generated"
assert foreignConsumer.isFile() : "Expected the foreign-container consumer contract to be generated"
assert producer.text.contains("payload: MyAccountCreatedV1Payload")
assert consumer.text.contains("payload: MyAccountCreatedV1Payload")
assert foreignProducer.text.contains("payload: MyAccountReviewedV1Payload")
assert foreignConsumer.text.contains("payload: MyAccountReviewedV1Payload")

def classes = new File(basedir, "target/classes")
assert new File(classes, "com/example/external/model/MyAccountCreatedV1Payload.class").isFile() :
    "Expected the referenced payload model to compile"
assert new File(classes, "com/example/external/model/MyAccountDetails.class").isFile() :
    "Expected the referenced supporting model to compile"
assert new File(classes, "com/example/external/model/MyAccountReviewedV1Payload.class").isFile() :
    "Expected the foreign-container payload model to compile"
assert new File(classes, "com/example/external/model/MyAccountReviewMetadata.class").isFile() :
    "Expected the foreign-container supporting model to compile"
assert new File(classes, "com/example/external/client/producer/MyAccountCreatedProducer.class").isFile() :
    "Expected the generated producer contract to compile"
assert new File(classes, "com/example/external/client/consumer/MyAccountCreatedConsumer.class").isFile() :
    "Expected the generated consumer contract to compile"
assert new File(classes, "com/example/external/client/producer/MyAccountReviewedProducer.class").isFile() :
    "Expected the foreign-container producer contract to compile"
assert new File(classes, "com/example/external/client/consumer/MyAccountReviewedConsumer.class").isFile() :
    "Expected the foreign-container consumer contract to compile"

def bundled = new File(basedir, "target/bundled.yaml")
assert bundled.isFile() : "Expected the bundled AsyncAPI document"
assert !bundled.text.contains("messages.yaml#") : "Expected the external message reference to be bundled"
assert !bundled.text.contains("properties.yaml#") : "Expected the external property reference to be bundled"
assert !bundled.text.contains("foreign-models.yaml#") : "Expected the foreign schema reference to be bundled"

true
