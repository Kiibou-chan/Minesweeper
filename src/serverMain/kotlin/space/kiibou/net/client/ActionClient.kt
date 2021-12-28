package space.kiibou.net.client

import kotlinx.serialization.PolymorphicSerializer
import space.kiibou.net.common.Action
import space.kiibou.net.common.Serial
import space.kiibou.net.common.SocketConnection
import java.net.Socket

class ActionClient(
    override val onConnect: () -> Unit,
    override val onMessageReceived: (Action<*>) -> Unit,
    override val onDisconnect: () -> Unit
) : Client<Action<*>> {
    override var socket: Socket? = null
    override var connection: SocketConnection? = null

    override fun deserialize(string: String): Action<*> {
        return Serial.json.decodeFromString(PolymorphicSerializer(Action::class), string)
    }

    override fun serialize(obj: Action<*>): String {
        return Serial.json.encodeToString(PolymorphicSerializer(Action::class), obj)
    }
}
