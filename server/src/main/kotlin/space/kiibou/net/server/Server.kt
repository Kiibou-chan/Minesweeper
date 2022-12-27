package space.kiibou.net.server

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import space.kiibou.annotations.Inject
import space.kiibou.annotations.meta.ServiceLoadInfo
import space.kiibou.net.common.Callbacks
import space.kiibou.net.common.Serial
import space.kiibou.net.common.SocketConnection
import space.kiibou.net.reflect.ReflectUtils.createInstance
import space.kiibou.net.reflect.ReflectUtils.getAnnotatedFields
import space.kiibou.net.server.service.MessageService
import space.kiibou.net.server.service.RoutingService
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.*

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalSerializationApi::class)
class Server internal constructor(vararg serviceNames: String) {
    private val connections: MutableMap<Long, SocketConnection> =
        Collections.synchronizedMap(HashMap())
    private val services: MutableList<Service> = Collections.synchronizedList(ArrayList())
    private val connectCallbacks: Callbacks<Long, Unit> = Callbacks()
    private val disconnectCallbacks: Callbacks<Long, Unit> = Callbacks()
    private val messageCallbacks: Callbacks<Pair<Long, String>, Unit> = Callbacks()
    private lateinit var serverSocket: ServerSocket
    private val connectionListenerThread: Thread
    private val servicesMap: MutableMap<String, Service> = Collections.synchronizedMap(HashMap())

    private fun injectServices(service: Service) {
        val fields = getAnnotatedFields(service, Inject::class.java)
        for (field in fields) {
            val name = field.type.canonicalName
            val toInject = servicesMap[name]

            logger.info { "Injecting $name into ${service::class.java.canonicalName}" }

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

        logger.info { "Loaded Service $name" }

        return this
    }

    private fun getAutoLoadedServices(): Set<ServiceLoadInfo> {
        val stream = this::class.java.classLoader.getResourceAsStream("META-INF/server/services/Services.json")
            ?: return emptySet()

        return Json.decodeFromStream(stream)
    }

    private fun register(socket: Socket) {
        SocketConnection.create(socket).ifPresent { conn: SocketConnection ->
            conn.registerMessageCallback(::messageReceived)
            conn.registerDisconnectCallback(::connectionClosed)
            connections[conn.handle] = conn
            connectCallbacks.callAll(conn.handle)

            logger.info { "Registered connection with handle ${conn.handle}" }
        }
    }

    fun registerMessageReceivedCallback(callback: (Long, String) -> Unit): Long {
        return messageCallbacks.addCallback { (handle, message) -> callback(handle, message) }
    }

    fun onConnect(callback: (Long) -> Unit): Long = connectCallbacks.addCallback(callback)

    fun onDisconnect(callback: (Long) -> Unit): Long = disconnectCallbacks.addCallback(callback)

    private fun messageReceived(handle: Long, message: String) {
        logger.info { "RCV $handle: $message" }

        messageCallbacks.callAll(handle to message)
    }

    fun sendMessage(handle: Long, message: String): Boolean {
        logger.info { "SND $handle: $message" }

        connections[handle]?.sendMessage(message) ?: return false

        return true
    }

    fun broadcastMessage(message: String) {
        logger.info { "BTC: $message" }

        connections.forEach { (_, conn) -> conn.sendMessage(message) }
    }

    private fun connectionClosed(handle: Long) {
        logger.info { "Client $handle disconnected" }

        connections.remove(handle)
        disconnectCallbacks.callAll(handle)
    }

    init {
        connectionListenerThread = Thread({
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

        val autoLoadedServices = getAutoLoadedServices()

        for (serviceInfo in autoLoadedServices) {
            registerService(serviceInfo.serviceName)
        }

        registerService(MessageService::class.java.canonicalName)
        registerService(RoutingService::class.java.canonicalName)

        for (service in services) injectServices(service)
        for (service in services) service.initialize()
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

fun startServer(vararg services: Class<*>): Optional<Process> {
    val javaHome = System.getProperty("java.home")
    val javaBin = "$javaHome${File.separator}bin${File.separator}java"
    val classpath = System.getProperty("java.class.path")
    val className = "${Server::class.qualifiedName}Kt"
    val port = "--port=8454"
    val servicesArg = "--services=" + services.joinToString(";") { it.canonicalName }
    val builder = ProcessBuilder(javaBin, "-cp", classpath, className, port, servicesArg)

    return try {
        Optional.of(builder.start())
    } catch (e: IOException) {
        logger.error(e) { "Failed to start Server" }

        Optional.empty()
    }
}
