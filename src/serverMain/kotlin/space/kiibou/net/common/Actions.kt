package space.kiibou.net.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
abstract class Action<T> {
    abstract val data: T

    @Serializable
    class RequestHandle : Action<Unit>() {
        override val data: Unit = Unit
    }

    @Serializable
    class SetHandle(override val data: SetHandleData) : Action<SetHandleData>()

    @Serializable
    class WrongHandle(override val data: WrongHandleData) : Action<WrongHandleData>()

    companion object {
        private val serializationModule = SerializersModule {
            polymorphic(Action::class) {
                subclass(RequestHandle::class)
                subclass(SetHandle::class)
                subclass(WrongHandle::class)
            }
        }

        init {
            Serial.addModule(serializationModule)
        }
    }
}

@Serializable
data class SetHandleData(val handle: Long)

@Serializable
data class WrongHandleData(val expectedHandle: Long, val actualHandle: Long)
