package space.kiibou.net.common

data class Message<T>(
    val connectionHandle: Long,
    val content: T,
)
