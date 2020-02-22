package space.kiibou.net.server.service

import processing.data.JSONObject
import space.kiibou.net.common.ActionDispatcher
import space.kiibou.net.server.JSONMessage
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import space.kiibou.reflect.Inject

class ActionService(server: Server) : Service(server) {
    @Inject
    lateinit var json: JSONService
    private val dispatcher: ActionDispatcher<JSONMessage>

    override fun initialize() {
        json.addJSONMessageReceivedCallback(dispatcher.messageReceived)
    }

    fun sendActionToClient(handle: Long, action: String, message: JSONObject = JSONObject()) {
        message.setString("action", action)
        json.sendJSONObject(handle, message)
    }

    fun addActionCallback(action: String, callback: (JSONMessage) -> Unit): Long {
        return dispatcher.addActionCallback(action, callback)
    }

    fun removeActionCallback(action: String, callbackHandle: Int) {
        dispatcher.removeActionCallback(action, callbackHandle)
    }

    private fun dispatch(action: String, jsonMessage: JSONMessage) {
        dispatcher.dispatchAction(action, jsonMessage)
    }

    init {
        dispatcher = ActionDispatcher {
            if (it.hasKey("handle") && it.hasKey("message")) {
                val handle = it.getInt("handle")
                val message = it.getJSONObject("message")
                if (message.hasKey("action")) {
                    val action = message.getString("action")
                    val jsonMessage = JSONMessage(handle.toLong(), message)
                    dispatch(action, jsonMessage)
                }
            }
        }
    }
}