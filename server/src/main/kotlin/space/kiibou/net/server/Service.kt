package space.kiibou.net.server

abstract class Service(val server: Server) {
    abstract fun initialize()
}
