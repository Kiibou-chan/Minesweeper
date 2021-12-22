package space.kiibou.net.server

abstract class Service(protected val server: Server) {
    abstract fun initialize()
}
