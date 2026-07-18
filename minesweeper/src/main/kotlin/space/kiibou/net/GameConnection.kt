package space.kiibou.net

import space.kiibou.net.client.Client
import space.kiibou.net.common.MessageType

/**
 * What the UI needs from the network: the two send shapes. Production wraps the socket
 * [Client]; GUI tests substitute a recording fake.
 */
interface GameConnection {
    fun <T : Any> send(messageType: MessageType<T>, payload: T)
    fun send(messageType: MessageType<Unit>)
}

class ClientGameConnection(private val client: Client) : GameConnection {
    override fun <T : Any> send(messageType: MessageType<T>, payload: T) =
        client.send(messageType, payload)

    override fun send(messageType: MessageType<Unit>) = client.send(messageType)
}
