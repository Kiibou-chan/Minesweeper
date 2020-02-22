package space.kiibou.net.server.service

import processing.data.JSONObject
import space.kiibou.net.common.Callbacks
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import java.util.*

class JSONService(server: Server) : Service(server) {
    private val jsonMessageCallbacks: Callbacks<JSONObject, Unit> = Callbacks()
    override fun initialize() {}

    private fun onMessageReceived(handle: Long, message: String) {
        val parsedMessage = JSONObject.parse(message)
        val result = JSONObject()
        result.setLong("handle", handle)
        result.setJSONObject("message", parsedMessage)
        jsonMessageCallbacks.callAll(result)
    }

    fun addJSONMessageReceivedCallback(callback: (JSONObject) -> Unit): Long {
        Objects.requireNonNull(callback)
        return jsonMessageCallbacks.addCallback { message ->
            callback(message)
        }
    }

    fun removeJSONMessageReceivedCallback(callbackHandle: Long) {
        jsonMessageCallbacks.removeCallback(callbackHandle)
    }

    fun sendJSONObject(handle: Long, obj: JSONObject) {
        Objects.requireNonNull(obj)
        server.sendMessage(handle, obj.format(-1))
    }

    fun broadcastJSONObject(obj: JSONObject) {
        Objects.requireNonNull(obj)
        server.broadcastMessage(obj.format(-1))
    }

    init {
        server.registerMessageReceivedCallback(::onMessageReceived)
    }
}