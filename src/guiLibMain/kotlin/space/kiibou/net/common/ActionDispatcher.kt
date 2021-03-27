package space.kiibou.net.common

import java.util.*

class ActionDispatcher<T>(private val messageReceivedCallback: ActionDispatcher<T>.(T) -> Unit) {

    private val actionCallbackMap = Collections.synchronizedMap(HashMap<String, Callbacks<T, Unit>>())

    fun dispatchAction(action: String, jsonMessage: T) {
        if (actionCallbackMap.containsKey(action)) {
            actionCallbackMap[action]!!.callAll(jsonMessage)
        }
    }

    fun addCallback(action: String, callback: (T) -> Unit): Long {
        if (!actionCallbackMap.containsKey(action)) {
            actionCallbackMap[action] = Callbacks()
        }

        return actionCallbackMap[action]!!.addCallback(callback)
    }

    fun removeCallback(action: String, callbackHandle: Int) {
        if (actionCallbackMap.containsKey(action)) {
            actionCallbackMap[action]!!.removeCallback(callbackHandle.toLong())
        }
    }

    fun messageReceived(node: T) {
        messageReceivedCallback(node)
    }

}