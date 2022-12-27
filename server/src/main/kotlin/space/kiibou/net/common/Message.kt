package space.kiibou.net.common

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.reflect.KClass

@SerialName("Message")
data class Message<T : Any>(
    val connectionHandle: Long,
    val messageType: MessageType<T>,
    val payload: T,
)

@Serializable
abstract class MessageType<T : Any>(val clazz: KClass<T>)
