package space.kiibou.net.server.service

import space.kiibou.annotations.AutoLoad
import space.kiibou.annotations.Inject
import space.kiibou.net.common.*
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service

@AutoLoad
class RoutingService(server: Server) : Service(server) {
    @Inject
    lateinit var messageService: MessageService

    private val router: Router = Router()

    override fun initialize() {
        messageService.registerCallback(router::messageReceived)

    }

    fun <T : Any> registerCallback(type: MessageType<T>, callback: (Message<T>) -> Unit): Long {
        @Suppress("UNCHECKED_CAST")
        return router.addCallback(type, callback as (Any) -> Unit)
    }

    fun removeCallback(type: MessageType<*>, callbackHandle: Int) {
        router.removeCallback(type, callbackHandle)
    }
}
