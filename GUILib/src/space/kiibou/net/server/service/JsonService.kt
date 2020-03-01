package space.kiibou.net.server.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import space.kiibou.net.common.Callbacks
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service

class JsonService(server: Server) : Service(server) {
    private val jsonMessageCallbacks: Callbacks<JsonNode, Unit> = Callbacks()

    val mapper = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun initialize() {}

    private fun onMessageReceived(handle: Long, message: String) {
        val parsedMessage = mapper.readTree(message)
        val result = mapper.createObjectNode().put("handle", handle).set("message", parsedMessage)
        jsonMessageCallbacks.callAll(result)
    }

    fun addJSONMessageReceivedCallback(callback: (JsonNode) -> Unit): Long {
        return jsonMessageCallbacks.addCallback { message ->
            callback(message)
        }
    }

    fun removeJSONMessageReceivedCallback(callbackHandle: Long) {
        jsonMessageCallbacks.removeCallback(callbackHandle)
    }

    fun sendJsonMessage(handle: Long, obj: JsonNode) {
        server.sendMessage(handle, mapper.writeValueAsString(obj))
    }

    fun broadcastJsonMessage(obj: JsonNode) {
        server.broadcastMessage(mapper.writeValueAsString(obj))
    }

    init {
        server.registerMessageReceivedCallback(::onMessageReceived)
    }
}