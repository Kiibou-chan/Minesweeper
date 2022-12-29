package space.kiibou.net.common

import mu.KotlinLogging
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.util.*

private val logger = KotlinLogging.logger {  }

class SocketConnection private constructor(socket: Socket) {
    private val out: PrintWriter
    private val listener: InputStreamListener
    val handle: ConnectionHandle = nextHandle()

    fun registerMessageCallback(callback: (ConnectionHandle, String) -> Unit) {
        listener.registerMessageCallback { callback(handle, it) }
    }

    fun registerDisconnectCallback(callback: (ConnectionHandle) -> Unit) {
        listener.registerDisconnectCallback { callback(handle) }
    }

    fun sendMessage(message: String) {
        out.println(message)
        out.flush()
    }

    companion object {
        private var cHandle: Long = 0

        private fun nextHandle(): ConnectionHandle {
            return ConnectionHandle(cHandle++)
        }

        fun create(socket: Socket): Optional<SocketConnection> {
            return try {
                Optional.of(SocketConnection(socket))
            } catch (e: IOException) {
                logger.warn { "Failed to create socket connection to [${socket.inetAddress.hostAddress}:${socket.port}" }

                Optional.empty()
            }
        }
    }

    init {
        listener = InputStreamListener(socket.getInputStream())
        out = PrintWriter(OutputStreamWriter(socket.getOutputStream()))
        val listenerThread = Thread(listener, "Listener-Thread $handle")
        listenerThread.start()
    }
}
