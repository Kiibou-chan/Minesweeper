package space.kiibou.net.common

import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.util.*

class SocketConnection private constructor(socket: Socket) {
    private val out: PrintWriter
    private val listener: InputStreamListener
    val handle: Long

    fun registerMessageCallback(callback: (Long, String) -> Unit) {
        listener.registerMessageCallback { callback(handle, it) }
    }

    fun registerDisconnectCallback(callback: (Long) -> Unit) {
        listener.registerDisconnectCallback { callback(handle) }
    }

    fun sendMessage(message: String) {
        out.println(message)
        out.flush()
    }

    companion object {
        private var cHandle: Long = 0

        private fun nextHandle(): Long {
            return cHandle++
        }

        fun create(socket: Socket): Optional<SocketConnection> {
            return try {
                Optional.of(SocketConnection(socket))
            } catch (e: IOException) {
                System.err.println("[Client] Failed to create Connection!")
                Optional.empty()
            }
        }
    }

    init {
        handle = nextHandle()
        listener = InputStreamListener(socket.getInputStream())
        out = PrintWriter(OutputStreamWriter(socket.getOutputStream()))
        val listenerThread = Thread(listener, "Listener-Thread $handle")
        listenerThread.start()
    }
}
