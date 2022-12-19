package space.kiibou.net.client

import space.kiibou.net.common.*
import java.net.Socket

class Client(
    val onConnect: () -> Unit,
    val onMessageReceived: (Message<*>) -> Unit,
    val onDisconnect: () -> Unit,
) {
    var handle: Long = -1

    var socket: Socket? = null
    var connection: SocketConnection? = null

    private val messageSerializer = InternalMessageSerializer(Serial.json)

    fun connect(address: String, port: Int): Client {
        try {
            socket = Socket(address, port)
            SocketConnection.create(socket!!).ifPresent { connection ->
                onConnect()
                this.connection = connection
                connection.registerMessageCallback { _, message ->
                    println("[Client] RCV: $message")

                    onMessageReceived(Serial.json.decodeFromString(messageSerializer, message))
                }
                connection.registerDisconnectCallback { onDisconnect() }
            }

            println("[Client] Successfully connected to Server at $address:$port")
        } catch (ex: Exception) {
            println("[Client] Failed to connect to Server at $address:$port")
            ex.printStackTrace()
        }

        return this
    }

    fun disconnect() {
        if (socket?.isClosed == false)
            socket?.close()
    }

    fun <T : Any> send(messageType: MessageType<T>, payload: T) {
        val message: Message<*> = Message(handle, messageType, payload)

        send(Serial.json.encodeToString(messageSerializer, message))
    }

    fun send(messageType: MessageType<Unit>) = send(messageType, Unit)

    private fun send(message: String) {
        println("[Client] SND: $message")

        connection?.sendMessage(message)
    }

}