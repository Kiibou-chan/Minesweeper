package space.kiibou.net.client

import processing.data.JSONObject
import space.kiibou.net.common.SocketConnection
import java.io.IOException
import java.net.Socket

class Client(private val onConnect: () -> Unit, private val onMessageReceived: (JSONObject) -> Unit, private val onDisconnect: () -> Unit) {
    private var socket: Socket? = null
    private var connection: SocketConnection? = null

    fun connect(address: String, port: Int): Client {
        try {
            socket = Socket(address, port)
            SocketConnection.create(socket!!).ifPresent { conn: SocketConnection ->
                onConnect()
                connection = conn
                conn.registerMessageCallback { _, msg -> onMessageReceived(JSONObject.parse(msg)) }
                conn.registerDisconnectCallback { onDisconnect() }
            }
        } catch (ex: IOException) {
            println("Failed to connect to $address:$port")
        }

        return this
    }

    fun stop() {
        socket?.close()
    }

    fun sendMessage(message: String) {
        connection?.sendMessage(message)
    }

    fun sendJSON(obj: JSONObject) {
        val message = obj.format(-1)
        sendMessage(message)
    }
}