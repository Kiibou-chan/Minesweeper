package space.kiibou.net.common

import processing.data.JSONObject
import java.util.*

class ActionDispatcher<T>(val messageReceived: (JSONObject) -> Unit) {
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
        return actionCallbackMap[action]!!.addCallback { message: T ->
            callback(message)
        }
    }

    fun removeActionCallback(action: String, callbackHandle: Int) {
        if (actionCallbackMap.containsKey(action)) {
            actionCallbackMap[action]!!.removeCallback(callbackHandle.toLong())
        }
    }

}