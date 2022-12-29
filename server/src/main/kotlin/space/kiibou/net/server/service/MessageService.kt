package space.kiibou.net.server.service

import space.kiibou.annotations.AutoLoad
import space.kiibou.net.common.*
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service

@AutoLoad
class MessageService(server: Server) : Service(server) {
    private val messageCallbacks: Callbacks<ServerMessage<*>, Unit> = Callbacks()

    private val messageSerializer = InternalMessageSerializer(Serial.json)

    override fun initialize() {
        server.registerMessageReceivedCallback { handle, message ->
            val parsedMessage = Serial.json.decodeFromString(messageSerializer, message)

            messageCallbacks.callAll(parsedMessage.toServerMessage(handle))
        }
    }

    fun registerCallback(callback: (ServerMessage<*>) -> Unit): Long {
        return messageCallbacks.addCallback(callback)
    }

    fun removeCallback(callbackHandle: Long) {
        messageCallbacks.removeCallback(callbackHandle)
    }

    fun <T : Any> send(handle: ConnectionHandle, message: Message<T>) {
        server.sendMessage(handle, Serial.json.encodeToString(messageSerializer, message))
    }

    fun <T : Any> send(handle: ConnectionHandle, type: MessageType<T>, payload: T) {
        send(handle, Message(type, payload))
    }

    fun send(handle: ConnectionHandle, type: MessageType<Unit>) {
        send(handle, Message(type, Unit))
    }

    fun <T : Any> respond(original: ServerMessage<*>, type: MessageType<T>, payload: T) {
        send(
            original.connectionHandle,
            Message(
                type,
                payload
            )
        )
    }

    fun respond(original: ServerMessage<*>, type: MessageType<Unit>) {
        respond(original, type, Unit)
    }

    fun <T : Any> broadcast(message: Message<T>) {
        server.broadcastMessage(Serial.json.encodeToString(messageSerializer, message))
    }
}
