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
        element<Long>("connectionHandle")
        element("messageType", messageTypeSerializer.descriptor)
        element<String>("payload")
    }

    override fun deserialize(decoder: Decoder): Message<*> =
        decoder.decodeStructure(descriptor) {
            var connectionHandle by Delegates.notNull<Long>()
            lateinit var messageType: MessageType<Any>
            lateinit var payloadString: String

            if (decodeSequentially()) {
                connectionHandle = decodeLongElement(descriptor, 0)
                messageType = decodeSerializableElement(descriptor, 1, messageTypeSerializer) as MessageType<Any>
                payloadString = decodeStringElement(descriptor, 2)
            } else {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> connectionHandle = decodeLongElement(descriptor, 0)
                        1 -> messageType = decodeSerializableElement(descriptor, 1, messageTypeSerializer) as MessageType<Any>
                        2 -> payloadString = decodeStringElement(descriptor, 2)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
            }

            val payload = json.decodeFromString(messageType.clazz.serializer(), payloadString)

            Message(connectionHandle, messageType, payload)
        }

    override fun serialize(encoder: Encoder, value: Message<*>) {
        val serializedPayload = json.encodeToString(value.messageType.clazz.serializer() as KSerializer<Any>, value.payload)

        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.connectionHandle)
            encodeSerializableElement(descriptor, 1, messageTypeSerializer, value.messageType)
            encodeStringElement(descriptor, 2, serializedPayload)
        }
    }

}