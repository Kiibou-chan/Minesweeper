package space.kiibou.net.client

import mu.KotlinLogging
import space.kiibou.net.common.*
import java.net.Socket

private val logger = KotlinLogging.logger { }

class Client(
    val onConnect: () -> Unit,
    val onMessageReceived: (Message<*>) -> Unit,
    val onDisconnect: () -> Unit,
) {
    var socket: Socket? = null
    var connection: SocketConnection? = null

    private val messageSerializer = InternalMessageSerializer(Serial.json)

    fun connect(address: String, port: Int): Client {
        try {
            socket = Socket(address, port)
            SocketConnection.create(socket!!).ifPresent { connection ->
                // The client must be fully usable (connection assigned, callbacks wired)
                // before onConnect fires — callers send messages from that callback.
                this.connection = connection

                connection.registerMessageCallback { _, message ->
                    logger.info { "RCV: $message" }

                    onMessageReceived(Serial.json.decodeFromString(messageSerializer, message))
                }

                connection.registerDisconnectCallback {
                    logger.info { "Disconnected from [$address:$port]" }

                    onDisconnect()
                }

                logger.info { "Connected to [$address:$port]" }

                onConnect()
            }
        } catch (ex: Exception) {
            logger.error(ex) {
                "Failed to connect to [$address:$port]"
            }
        }

        return this
    }

    fun disconnect() {
        if (socket?.isClosed == false)
            socket?.close()
    }

    fun <T : Any> send(messageType: MessageType<T>, payload: T) {
        val message: Message<*> = Message(messageType, payload)

        send(Serial.json.encodeToString(messageSerializer, message))
    }

    fun send(messageType: MessageType<Unit>) = send(messageType, Unit)

    private fun send(message: String) {
        logger.info { "SND: $message" }

        connection?.sendMessage(message)
    }

}