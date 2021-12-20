package space.kiibou.net.common

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.SocketException

class InputStreamListener(private val input: InputStream) : Runnable {
    private val messageCallbacks: Callbacks<String, Unit> = Callbacks()
    private val disconnectCallbacks: Callbacks<Unit, Unit> = Callbacks()

    override fun run() {
        try {
            BufferedReader(InputStreamReader(input)).use { reader ->
                while (!Thread.interrupted()) {
                    val message = reader.readLine()
                    messageCallbacks.callAll(message)
                }
            }
        } catch (e: SocketException) {
            disconnectCallbacks.callAll(Unit)
        } catch (e: NullPointerException) {
            disconnectCallbacks.callAll(Unit)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun registerMessageCallback(callback: (String) -> Unit): Long {
        return messageCallbacks.addCallback(callback)
    }

    fun registerDisconnectCallback(callback: (Unit) -> Unit): Long {
        return disconnectCallbacks.addCallback(callback)
    }

    fun stop() {
        Thread.currentThread().interrupt()
        input.close()
    }
}
