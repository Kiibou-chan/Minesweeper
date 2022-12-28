package space.kiibou.net.server.service

import space.kiibou.annotations.AutoLoad
import space.kiibou.net.common.*
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service

@AutoLoad
class MessageService(server: Server) : Service(server) {
    private val messageCallbacks: Callbacks<Message<*>, Unit> = Callbacks()

    private val messageSerializer = InternalMessageSerializer(Serial.json)

    override fun initialize() {
        server.registerMessageReceivedCallback { handle, message ->
            val parsedMessage = Serial.json.decodeFromString(messageSerializer, message)

            if (parsedMessage.messageType == ClientMessageType.RequestHandle) {
                send(
                    handle,
                    Message(
                        handle,
                        ServerMessageType.SetHandle,
                        SetHandleData(handle)
                    )
                )
            } else if (parsedMessage.connectionHandle != handle) {
                send(
                    handle,
                    Message(
                        handle,
                        ServerMessageType.WrongHandle,
                        WrongHandleData(handle, parsedMessage.connectionHandle)
                    )
                )

                @Suppress("UNCHECKED_CAST")
                messageCallbacks.callAll(
                    Message(
                        handle,
                        parsedMessage.messageType as MessageType<Any>,
                        parsedMessage.payload
                    )
                )
            } else {
                messageCallbacks.callAll(parsedMessage)
            }
        }
    }

    fun registerCallback(callback: (Message<*>) -> Unit): Long {
        return messageCallbacks.addCallback(callback)
    }

    fun removeCallback(callbackHandle: Long) {
        messageCallbacks.removeCallback(callbackHandle)
    }

    fun <T : Any> send(handle: Long, message: Message<T>) {
        server.sendMessage(handle, Serial.json.encodeToString(messageSerializer, message))
    }

    fun <T : Any> send(handle: Long, type: MessageType<T>, payload: T) {
        send(handle, Message(handle, type, payload))
    }

    fun send(handle: Long, type: MessageType<Unit>) {
        send(handle, Message(handle, type, Unit))
    }

    fun <T : Any> respond(original: Message<*>, type: MessageType<T>, payload: T) {
        send(
            original.connectionHandle,
            Message(
                original.connectionHandle,
                type,
                payload
            )
        )
    }

    fun respond(original: Message<*>, type: MessageType<Unit>) {
        respond(original, type, Unit)
    }

    fun <T : Any> broadcast(message: Message<T>) {
        server.broadcastMessage(Serial.json.encodeToString(messageSerializer, message))
    }
}
