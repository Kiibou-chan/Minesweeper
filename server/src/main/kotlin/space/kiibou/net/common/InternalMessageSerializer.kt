package space.kiibou.net.common

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlin.properties.Delegates

@Suppress("UNCHECKED_CAST")
@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class InternalMessageSerializer(private val json: Json) : KSerializer<Message<*>> {
    private val messageTypeSerializer = PolymorphicSerializer(MessageType::class)

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Message") {
        element("messageType", messageTypeSerializer.descriptor)
        element<String>("payload")
    }

    override fun deserialize(decoder: Decoder): Message<*> =
        decoder.decodeStructure(descriptor) {
            lateinit var messageType: MessageType<Any>
            lateinit var payloadString: String

            if (decodeSequentially()) {
                messageType = decodeSerializableElement(descriptor, 0, messageTypeSerializer) as MessageType<Any>
                payloadString = decodeStringElement(descriptor, 1)
            } else {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> messageType = decodeSerializableElement(descriptor, 0, messageTypeSerializer) as MessageType<Any>
                        1 -> payloadString = decodeStringElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
            }

            val payload = json.decodeFromString(messageType.clazz.serializer(), payloadString)

            Message(messageType, payload)
        }

    override fun serialize(encoder: Encoder, value: Message<*>) {
        val serializedPayload = json.encodeToString(value.messageType.clazz.serializer() as KSerializer<Any>, value.payload)

        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, messageTypeSerializer, value.messageType)
            encodeStringElement(descriptor, 1, serializedPayload)
        }
    }

}
