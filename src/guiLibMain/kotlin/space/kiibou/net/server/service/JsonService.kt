package space.kiibou.net.server.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import space.kiibou.net.common.Callbacks
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service

class JsonService(server: Server) : Service(server) {
    private val jsonMessageCallbacks: Callbacks<JsonNode, Unit> = Callbacks()

    val mapper: ObjectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun initialize() {}

    fun registerCallback(callback: (JsonNode) -> Unit): Long {
        return jsonMessageCallbacks.addCallback { message ->
            callback(message)
        }
    }

    fun removeCallback(callbackHandle: Long) {
        jsonMessageCallbacks.removeCallback(callbackHandle)
    }

    fun send(handle: Long, obj: JsonNode) {
        server.sendMessage(handle, mapper.writeValueAsString(obj))
    }

    fun broadcast(obj: JsonNode) {
        server.broadcastMessage(mapper.writeValueAsString(obj))
    }

    init {
        server.registerMessageReceivedCallback { handle, message ->
            val parsedMessage = mapper.readTree(message)
            val result = mapper.createObjectNode()
                .put("handle", handle)
                .set<JsonNode>("message", parsedMessage)
            jsonMessageCallbacks.callAll(result)
        }
    }
}