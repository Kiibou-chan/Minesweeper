package space.kiibou.lobby

import space.kiibou.net.GameConnection
import space.kiibou.net.common.Message
import space.kiibou.net.common.MessageType

/** Records everything the UI sends; incoming messages are injected via the dispatcher. */
class FakeGameConnection : GameConnection {
    val sent = mutableListOf<Message<*>>()

    val sentTypes: List<MessageType<*>> get() = sent.map { it.messageType }

    override fun <T : Any> send(messageType: MessageType<T>, payload: T) {
        sent += Message(messageType, payload)
    }

    override fun send(messageType: MessageType<Unit>) {
        sent += Message(messageType, Unit)
    }
}
