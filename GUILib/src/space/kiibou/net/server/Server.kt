package space.kiibou.net.server

import space.kiibou.net.common.Callbacks
import space.kiibou.net.common.SocketConnection
import space.kiibou.net.common.TextMessage
import space.kiibou.net.server.service.ActionService
import space.kiibou.net.server.service.JSONService
import space.kiibou.reflect.Inject
import space.kiibou.reflect.ReflectUtils.createInstance
import space.kiibou.reflect.ReflectUtils.getAnnotatedFields
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Server internal constructor(vararg serviceNames: String) {
    private val connections: MutableMap<Long, SocketConnection> = Collections.synchronizedMap(HashMap<Long, SocketConnection>())
    private val services: MutableList<Service> = Collections.synchronizedList(ArrayList())
    private val messageCallbacks: Callbacks<TextMessage, Unit> = Callbacks()
    private lateinit var serverSocket: ServerSocket
    private val connectionListenerThread: Thread
    private val servicesMap: MutableMap<String, Service> = Collections.synchronizedMap(HashMap())

    private fun injectServices(service: Service) {
        val fields = getAnnotatedFields(service, Inject::class.java)
        for (field in fields) {
            val name = field.type.canonicalName
            val toInject = servicesMap[name]
            println("Injecting $name into ${service.javaClass.canonicalName}")
            field[service] = toInject
        }
    }

    fun connect(port: Int): Server {
        serverSocket = ServerSocket(port)
        connectionListenerThread.start()
        return this
    }

    private fun registerService(name: String): Server {
        val service = createInstance<Service>(name, arrayOf(Server::class.java), this)
        services.add(service)
        servicesMap[name] = service
        println("Successfully loaded Service $name")
        return this
    }

    private fun register(socket: Socket) {
        SocketConnection.create(socket).ifPresent { conn: SocketConnection ->
            conn.registerMessageCallback(::messageReceived)
            conn.registerDisconnectCallback(::connectionClosed)
            connections[conn.handle] = conn
            System.out.printf("Registered connection with handle %d%n", conn.handle)
        }
    }

    fun registerMessageReceivedCallback(callback: (Long, String) -> Unit): Long {
        return messageCallbacks.addCallback { message ->
            callback(message.connectionHandle, message.message)
        }
    }

    private fun messageReceived(handle: Long, message: String) {
        val msg = TextMessage(handle, message)
        messageCallbacks.callAll(msg)
    }

    fun sendMessage(handle: Long, message: String): Boolean {
        connections[handle]?.sendMessage(message) ?: return false
        return true
    }

    fun broadcastMessage(message: String) {
        connections.forEach { (_, conn) -> conn.sendMessage(message) }
    }

    private fun connectionClosed(handle: Long) {
        System.out.printf("Client %d disconnected%n", handle)
        connections.remove(handle)
    }

    init {
        connectionListenerThread = Thread(Runnable {
            while (!Thread.interrupted()) {
                try {
                    val conn = serverSocket.accept()
                    register(conn)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }, "Server-Connection-Listener-Thread")

        for (serviceName in serviceNames) {
            registerService(serviceName)
        }

        registerService(JSONService::class.java.canonicalName)
        registerService(ActionService::class.java.canonicalName)

        for (service in services) injectServices(service)
        for (service in services) service.initialize()
    }
}

fun main(args: Array<String>) {
    var port = -1
    lateinit var services: Array<String>

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

fun startServer(vararg services: Class<*>): Optional<Process> {
    val javaHome = System.getProperty("java.home")
    val javaBin = javaHome + File.separator + "bin" + File.separator + "java"
    val classpath = System.getProperty("java.class.path")
    val className = "space.kiibou.net.server.ServerKt"
    val port = "--port=8454"
    val servicesArg = "--services=" + Arrays.stream(services)
            .map { it.canonicalName }
            .collect(Collectors.joining(";"))
    val builder = ProcessBuilder(javaBin, "-cp", classpath, className, port, servicesArg)

    return try {
        Optional.of(builder.start())
    } catch (e: IOException) {
        System.err.println("Failed to start Server!")
        Optional.empty()
    }
}
