package space.kiibou.net.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object ClientMessageType {
    @Serializable
    object RequestHandle : MessageType<Unit>(Unit::class)

    val serializersModule = SerializersModule {
        polymorphic(MessageType::class) {
            subclass(RequestHandle::class)
        }
    }
}

object ServerMessageType {
    @Serializable
    object SetHandle : MessageType<SetHandleData>(SetHandleData::class)

    @Serializable
    object WrongHandle : MessageType<WrongHandleData>(WrongHandleData::class)

    val serializersModule = SerializersModule {
        polymorphic(MessageType::class) {
            subclass(SetHandle::class)
            subclass(WrongHandle::class)
        }
    }
}

@Serializable
data class SetHandleData(val handle: Long)

@Serializable
data class WrongHandleData(val expectedHandle: Long, val actualHandle: Long)
