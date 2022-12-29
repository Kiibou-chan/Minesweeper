package space.kiibou.net.common

import java.util.*

class Router<T : Message<*>> {

    private val messageCallbackMap: MutableMap<MessageType<*>, Callbacks<T, Unit>> = Collections.synchronizedMap(HashMap())

    fun <T : Any> addCallback(type: MessageType<T>, callback: (Message<T>) -> Unit): Long {
        if (!messageCallbackMap.containsKey(type)) {
            messageCallbackMap[type] = Callbacks()
        }

        @Suppress("UNCHECKED_CAST")
        return messageCallbackMap[type]!!.addCallback(callback as (Any) -> Unit)
    }

    fun removeCallback(type: MessageType<*>, callbackHandle: Int) {
        if (messageCallbackMap.containsKey(type)) {
            messageCallbackMap[type]!!.removeCallback(callbackHandle.toLong())
        }
    }

    fun messageReceived(message: T) {
        if (messageCallbackMap.containsKey(message.messageType)) {
            messageCallbackMap[message.messageType]!!.callAll(message)
        }
    }

}