package space.kiibou.net.common

import com.fasterxml.jackson.databind.JsonNode
import java.util.*

class ActionDispatcher<T>(private val messageReceivedCallback: ActionDispatcher<T>.(JsonNode) -> Unit) {

    private val actionCallbackMap = Collections.synchronizedMap(HashMap<String, Callbacks<T, Unit>>())

    fun dispatchAction(action: String, jsonMessage: T) {
        if (actionCallbackMap.containsKey(action)) {
            actionCallbackMap[action]!!.callAll(jsonMessage)
        }
    }

    fun addActionCallback(action: String, callback: (T) -> Unit): Long {
        if (!actionCallbackMap.containsKey(action)) {
            actionCallbackMap[action] = Callbacks()
        }

        return actionCallbackMap[action]!!.addCallback(callback)
    }

    fun removeActionCallback(action: String, callbackHandle: Int) {
        if (actionCallbackMap.containsKey(action)) {
            actionCallbackMap[action]!!.removeCallback(callbackHandle.toLong())
        }
    }

    fun messageReceived(node: JsonNode) {
        messageReceivedCallback(node)
    }

}