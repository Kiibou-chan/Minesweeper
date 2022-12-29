package space.kiibou.net.common

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.reflect.KClass

interface Message<T : Any> {
    val messageType: MessageType<T>
    val payload: T

    companion object {
        operator fun <T : Any> invoke(messageType: MessageType<T>, payload: T) = MessageImpl(messageType, payload)
    }
}

data class MessageImpl<T : Any>(override val messageType: MessageType<T>, override val payload: T) : Message<T>

@Serializable
data class ConnectionHandle(val handle: Long)

data class ServerMessage<T : Any>(
    val connectionHandle: ConnectionHandle,
    override val messageType: MessageType<T>,
    override val payload: T
) : Message<T>

fun <T : Any> Message<T>.toServerMessage(handle: ConnectionHandle) = ServerMessage(
    handle,
    messageType,
    payload
)

@Serializable
abstract class MessageType<T : Any>(val clazz: KClass<T>)
