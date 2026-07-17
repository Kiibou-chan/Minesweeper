package space.kiibou.net.server

import mu.KotlinLogging
import space.kiibou.net.common.Callbacks
import space.kiibou.net.common.ConnectionHandle
import space.kiibou.net.common.SocketConnection
import space.kiibou.net.server.service.ServiceLoader
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.*

private val logger = KotlinLogging.logger { }

class Server internal constructor(vararg serviceNames: String) {
    private val connections: MutableMap<ConnectionHandle, SocketConnection> =
        Collections.synchronizedMap(HashMap())
    private val connectCallbacks: Callbacks<ConnectionHandle, Unit> = Callbacks()
    private val disconnectCallbacks: Callbacks<ConnectionHandle, Unit> = Callbacks()
    private val messageCallbacks: Callbacks<Pair<ConnectionHandle, String>, Unit> = Callbacks()
    private lateinit var serverSocket: ServerSocket
    private val connectionListenerThread: Thread

    private val serviceLoader = ServiceLoader(this, serviceNames)

    fun connect(port: Int): Server {
        serverSocket = ServerSocket(port)
        connectionListenerThread.start()
        return this
    }

    private fun registerSocket(socket: Socket) {
        SocketConnection.create(socket).ifPresent { conn: SocketConnection ->
            conn.registerMessageCallback(::messageReceived)
            conn.registerDisconnectCallback(::connectionClosed)
            connections[conn.handle] = conn
            connectCallbacks.callAll(conn.handle)

            logger.info { "Registered connection with handle ${conn.handle}" }
        }
    }

    fun registerMessageReceivedCallback(callback: (ConnectionHandle, String) -> Unit): Long {
        return messageCallbacks.addCallback { (handle, message) -> callback(handle, message) }
    }

    fun onConnect(callback: (ConnectionHandle) -> Unit): Long = connectCallbacks.addCallback(callback)

    fun onDisconnect(callback: (ConnectionHandle) -> Unit): Long = disconnectCallbacks.addCallback(callback)

    private fun messageReceived(handle: ConnectionHandle, message: String) {
        logger.info { "RCV ${handle.handle}: $message" }

        messageCallbacks.callAll(handle to message)
    }

    fun sendMessage(handle: ConnectionHandle, message: String): Boolean {
        logger.info { "SND ${handle.handle}: $message" }

        connections[handle]?.sendMessage(message) ?: return false

        return true
    }

    fun broadcastMessage(message: String) {
        logger.info { "BTC: $message" }

        connections.forEach { (_, conn) -> conn.sendMessage(message) }
    }

    private fun connectionClosed(handle: ConnectionHandle) {
        logger.info { "Client ${handle.handle} disconnected" }

        connections.remove(handle)
        disconnectCallbacks.callAll(handle)
    }

    init {
        connectionListenerThread = Thread({
            while (!Thread.interrupted()) {
                try {
                    val conn = serverSocket.accept()
                    registerSocket(conn)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }, "Server-Connection-Listener-Thread")
    }
}

fun main(args: Array<String>) {
    var port = -1
    var services: Array<String> = emptyArray()

    for (arg in args) {
        val index = arg.indexOf('=')
        if (index != -1) {
            val key = arg.substring(0, index)
            val value = arg.substring(index + 1)
            when (key) {
                "--port" -> port = value.toInt()
                "--services" -> services = value.split(";").toTypedArray()
            }
        }
    }

    Server(*services).connect(port)
}

fun startServer(): Optional<Process> {
    val javaHome = System.getProperty("java.home")
    val javaBin = "$javaHome${File.separator}bin${File.separator}java"
    val classpath = System.getProperty("java.class.path")
    val className = "${Server::class.qualifiedName}Kt"
    val port = "--port=8454"
    val builder = ProcessBuilder(javaBin, "-cp", classpath, className, port)

    builder.redirectInput(ProcessBuilder.Redirect.INHERIT)
    builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)

    return try {
        Optional.of(builder.start())
    } catch (e: IOException) {
        logger.error(e) { "Failed to start Server" }

        Optional.empty()
    }
}
