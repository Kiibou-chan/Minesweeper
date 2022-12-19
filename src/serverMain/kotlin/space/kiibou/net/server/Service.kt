package space.kiibou.net.server

abstract class Service(@PublishedApi internal val server: Server) {
    abstract fun initialize()
}
