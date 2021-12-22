package space.kiibou.net.server.service

import kotlinx.serialization.PolymorphicSerializer
import space.kiibou.net.common.Action
import space.kiibou.net.common.ActionDispatcher
import space.kiibou.net.common.Message
import space.kiibou.net.common.Serial
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import kotlin.reflect.jvm.jvmName

class ActionService(server: Server) : Service(server) {
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal val dispatcher: ActionDispatcher<Message<Action<*>>> = ActionDispatcher {
        dispatchAction(it.content::class.jvmName, it)
    }

    override fun initialize() {
        server.registerMessageReceivedCallback { handle, message ->
            val action = Serial.json.decodeFromString(PolymorphicSerializer(Action::class), message)

            dispatcher.messageReceived(Message(handle, action))
        }
    }

    fun send(handle: Long, action: Action<*>) {
        server.sendMessage(handle, Serial.json.encodeToString(PolymorphicSerializer(Action::class), action))
    }

    inline fun <S, reified T : Action<S>> registerCallback(noinline callback: (Message<T>) -> Unit): Long {
        return dispatcher.addCallback(T::class.jvmName) {
            @Suppress("UNCHECKED_CAST")
            callback(it as Message<T>)
        }
    }

    inline fun <reified T : Action<*>> removeCallback(callbackHandle: Int) {
        dispatcher.removeCallback(T::class.jvmName, callbackHandle)
    }
}
