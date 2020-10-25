package space.kiibou.net.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import space.kiibou.net.common.SocketConnection
import java.io.IOException
import java.net.Socket

private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

class Client(private val onConnect: () -> Unit, private val onMessageReceived: (JsonNode) -> Unit, private val onDisconnect: () -> Unit) {
    private var socket: Socket? = null
    private var connection: SocketConnection? = null

    fun connect(address: String, port: Int): Client {
        try {
            socket = Socket(address, port)
            SocketConnection.create(socket!!).ifPresent { conn: SocketConnection ->
                onConnect()
                connection = conn
                conn.registerMessageCallback { _, msg -> onMessageReceived(mapper.readTree(msg)) }
                conn.registerDisconnectCallback { onDisconnect() }
            }
        } catch (ex: IOException) {
            println("Failed to connect to $address:$port")
        }

        return this
    }

    fun disconnect() {
        socket?.close()
    }

    fun sendJson(obj: JsonNode) {
        sendMessage(mapper.writeValueAsString(obj))
    }

    private fun sendMessage(message: String) {
        connection?.sendMessage(message)
    }
}