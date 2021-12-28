package space.kiibou.net.client

import space.kiibou.net.common.SocketConnection
import java.net.Socket

interface Client<T> {
    val onConnect: () -> Unit
    val onMessageReceived: (T) -> Unit
    val onDisconnect: () -> Unit

    var socket: Socket?
    var connection: SocketConnection?

    fun deserialize(string: String): T

    fun serialize(obj: T): String

    fun connect(address: String, port: Int): Client<T> {
        try {
            socket = Socket(address, port)
            SocketConnection.create(socket!!).ifPresent { connection ->
                onConnect()
                this.connection = connection
                connection.registerMessageCallback { _, message ->
                    println("[Client] RCV: $message")

                    onMessageReceived(deserialize(message))
                }
                connection.registerDisconnectCallback { onDisconnect() }
            }
        } catch (ex: Exception) {
            println("[Client] Failed to connect to $address:$port")
            ex.printStackTrace()
        }

        return this
    }

    fun disconnect() {
        if (socket?.isClosed == false)
            socket?.close()
    }

    fun send(obj: T) {
        val message = serialize(obj)

        println("[Client] SND: $message")

        connection?.sendMessage(message)
    }

}