package space.kiibou.net.server.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import space.kiibou.net.common.ActionDispatcher
import space.kiibou.net.common.Message
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import space.kiibou.reflect.Inject

class ActionService(server: Server) : Service(server) {
    @Inject
    lateinit var json: JsonService

    private val dispatcher: ActionDispatcher<Message<JsonNode>> = ActionDispatcher {
        val message = it.content
        if (message.has("action")) {
            val action = message.at("/action").textValue()
            val jsonMessage = Message(it.connectionHandle, message)
            dispatchAction(action, jsonMessage)
        }
    }

    override fun initialize() {
        json.registerCallback(dispatcher::messageReceived)
    }

    fun send(handle: Long, action: String, message: ObjectNode = json.mapper.createObjectNode()) {
        message.put("action", action)
        json.send(handle, message)
    }

    fun registerCallback(action: String, callback: (Message<JsonNode>) -> Unit): Long {
        return dispatcher.addCallback(action, callback)
    }

    fun removeCallback(action: String, callbackHandle: Int) {
        dispatcher.removeCallback(action, callbackHandle)
    }
}