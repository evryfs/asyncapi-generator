def output = new File(basedir, "target/generated-sources/asyncapi")
def modelPackage = new File(output, "com/example/external/model")
def clientPackage = new File(output, "com/example/external/client")
def payload = new File(modelPackage, "MyAccountCreatedV1Payload.kt")
def details = new File(modelPackage, "MyAccountDetails.kt")
def producer = new File(clientPackage, "producer/MyAccountCreatedProducer.kt")
def consumer = new File(clientPackage, "consumer/MyAccountCreatedConsumer.kt")

assert payload.isFile() : "Expected the referenced payload model to be generated"
assert details.isFile() : "Expected the referenced supporting model to be generated"
assert payload.text.contains("val accountId: String")
assert payload.text.contains("val details: MyAccountDetails")
assert producer.isFile() : "Expected the producer contract to be generated"
assert consumer.isFile() : "Expected the consumer contract to be generated"
assert producer.text.contains("payload: MyAccountCreatedV1Payload")
assert consumer.text.contains("payload: MyAccountCreatedV1Payload")

def classes = new File(basedir, "target/classes")
assert new File(classes, "com/example/external/model/MyAccountCreatedV1Payload.class").isFile() :
    "Expected the referenced payload model to compile"
assert new File(classes, "com/example/external/model/MyAccountDetails.class").isFile() :
    "Expected the referenced supporting model to compile"
assert new File(classes, "com/example/external/client/producer/MyAccountCreatedProducer.class").isFile() :
    "Expected the generated producer contract to compile"
assert new File(classes, "com/example/external/client/consumer/MyAccountCreatedConsumer.class").isFile() :
    "Expected the generated consumer contract to compile"

def bundled = new File(basedir, "target/bundled.yaml")
assert bundled.isFile() : "Expected the bundled AsyncAPI document"
assert !bundled.text.contains("messages.yaml#") : "Expected the external message reference to be bundled"
assert !bundled.text.contains("properties.yaml#") : "Expected the external property reference to be bundled"
