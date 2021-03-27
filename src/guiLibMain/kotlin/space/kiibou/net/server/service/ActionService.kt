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
        if (it.has("handle") && it.has("message")) {
            val handle = it.at("/handle").intValue()
            val message = it.at("/message")
            if (message.has("action")) {
                val action = message.at("/action").textValue()
                val jsonMessage = Message(handle.toLong(), message)
                dispatch(action, jsonMessage)
            }
        }
    }

    override fun initialize() {
        json.registerCallback(dispatcher.messageReceived)
    }

    fun send(handle: Long, action: String, message: ObjectNode = json.mapper.createObjectNode()) {
        message.put("action", action)
        json.send(handle, message)
    }

    fun registerCallback(action: String, callback: (Message<JsonNode>) -> Unit): Long {
        return dispatcher.addActionCallback(action, callback)
    }

    fun removeCallback(action: String, callbackHandle: Int) {
        dispatcher.removeActionCallback(action, callbackHandle)
    }

    private fun dispatch(action: String, jsonMessage: Message<JsonNode>) {
        dispatcher.dispatchAction(action, jsonMessage)
    }
}